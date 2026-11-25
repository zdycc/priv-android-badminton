package com.example.ding_badminton.data.remote.meta

import com.example.ding_badminton.data.debug.RequestLogStore
import com.example.ding_badminton.data.remote.RemoteConfig
import com.example.ding_badminton.data.submit.CryptoCodec
import com.example.ding_badminton.domain.model.BookingConfig
import com.example.ding_badminton.domain.policy.BookingPolicies
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class VenueMapping(
    val venueId: String,
    val slotToTimeId: Map<String, String>,
    val courtToSiteId: Map<Int, String> = emptyMap(),
    val source: String,
)

interface VenueMetadataRepository {
    suspend fun resolveMapping(config: BookingConfig): VenueMapping
}

class FallbackVenueMetadataRepository : VenueMetadataRepository {
    override suspend fun resolveMapping(config: BookingConfig): VenueMapping {
        val policy = BookingPolicies.campusPolicies[config.campusName]
        val slotMap =
            (policy?.timeSlots ?: emptyList())
                .mapIndexed { idx, slot -> slot to (idx + 1).toString() }
                .toMap()
        val courtToSiteId =
            when (config.campusName) {
                // 来自真实页面 timeArr 样本：莫干山 1号场=14，2号场=15，3号场=16...
                "莫干山校区" -> (1..9).associateWith { court -> (court + 13).toString() }
                // 屏峰先按直连兜底；若后续抓包有差异再改成真实映射。
                "屏峰校区" -> (1..6).associateWith { court -> court.toString() }
                else -> emptyMap()
            }
        return VenueMapping(
            venueId =
                when (config.campusName) {
                    "莫干山校区" -> "6"
                    "屏峰校区" -> "1"
                    else -> "1"
                },
            slotToTimeId = slotMap,
            courtToSiteId = courtToSiteId,
            source = "fallback",
        )
    }
}

class ApiVenueMetadataRepository(
    private val baseUrl: String = RemoteConfig.BASE_URL,
    private val endpoint: String = RemoteConfig.VENUE_LIST_ENDPOINT,
) : VenueMetadataRepository {
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

    override suspend fun resolveMapping(config: BookingConfig): VenueMapping {
        val url = "${baseUrl.trimEnd('/')}/${endpoint.trimStart('/')}"
        RequestLogStore.append("meta", "GET $url")
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                error("metadata api failed: HTTP ${resp.code}")
            }
            val encryptedBody = resp.body?.string().orEmpty()
            val text =
                runCatching { CryptoCodec.decryptBase64AesCbcZeroPadded(encryptedBody) }
                    .getOrElse { encryptedBody }
            RequestLogStore.append("meta", "decoded ${text.take(120)}")
            val venueId = parseVenueId(text, config)
            if (venueId.isNullOrBlank()) {
                error("metadata api parse failed: venue id not found")
            }
            val policy = BookingPolicies.campusPolicies[config.campusName]
            val slotMap =
                (policy?.timeSlots ?: emptyList())
                    .mapIndexed { idx, slot -> slot to (idx + 1).toString() }
                    .toMap()
            val courtToSiteId =
                when (config.campusName) {
                    // 莫干山必须走真实映射：1号场=14，2号场=15...
                    "莫干山校区" -> (1..9).associateWith { court -> (court + 13).toString() }
                    "屏峰校区" -> (1..6).associateWith { court -> court.toString() }
                    else -> emptyMap()
                }
            return VenueMapping(
                venueId = venueId,
                slotToTimeId = slotMap,
                courtToSiteId = courtToSiteId,
                source = "api",
            )
        }
    }

    private fun parseVenueId(
        text: String,
        config: BookingConfig,
    ): String? {
        val venueNameKeywords = listOf(config.venueName, "羽毛球馆", "羽毛球场")
        runCatching {
            val root = JSONObject(text)
            val arrays = mutableListOf<JSONArray>()
            listOf("data", "list", "rows", "result").forEach { key ->
                val node = root.opt(key)
                when (node) {
                    is JSONArray -> arrays.add(node)
                    is JSONObject -> {
                        listOf("list", "rows", "items").forEach { sub ->
                            val arr = node.optJSONArray(sub)
                            if (arr != null) arrays.add(arr)
                        }
                    }
                }
            }
            arrays.forEach { arr ->
                val hit = findIdInArray(arr, venueNameKeywords)
                if (!hit.isNullOrBlank()) return hit
            }
        }
        runCatching {
            val arr = JSONArray(text)
            val hit = findIdInArray(arr, venueNameKeywords)
            if (!hit.isNullOrBlank()) return hit
        }
        return Regex("""["']id["']\s*:\s*["']?(\d+)["']?""").find(text)?.groupValues?.get(1)
    }

    private fun findIdInArray(
        arr: JSONArray,
        venueNameKeywords: List<String>,
    ): String? {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val name = obj.optString("name").ifBlank { obj.optString("venueName") }
            if (venueNameKeywords.any { k -> k.isNotBlank() && name.contains(k) }) {
                val id = obj.optString("id")
                if (id.isNotBlank()) return id
            }
        }
        return null
    }
}

class CompositeVenueMetadataRepository(
    private val primary: VenueMetadataRepository,
    private val fallback: VenueMetadataRepository,
) : VenueMetadataRepository {
    override suspend fun resolveMapping(config: BookingConfig): VenueMapping {
        return runCatching { primary.resolveMapping(config) }.getOrElse { fallback.resolveMapping(config) }
    }
}
