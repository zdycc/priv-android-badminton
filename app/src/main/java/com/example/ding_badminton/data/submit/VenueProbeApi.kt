package com.example.ding_badminton.data.submit

import com.example.ding_badminton.data.debug.RequestLogStore
import com.example.ding_badminton.data.remote.RemoteConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class VenueProbeApi(
    private val baseUrl: String = RemoteConfig.BASE_URL,
    private val endpoint: String = RemoteConfig.VENUE_LIST_ENDPOINT,
) {
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()

    suspend fun probe(
        token: String,
        venueParamsRaw: String,
    ): Result<String> {
        return runCatching {
            val url = "${baseUrl.trimEnd('/')}/${endpoint.trimStart('/')}"
            RequestLogStore.append("venue-probe", "POST $url")
            val bodyText = venueParamsRaw.trim()
            val req =
                Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Content-Type", "application/json;charset=UTF-8")
                    .addHeader("User-Token", token)
                    .post(bodyText.toRequestBody("application/json;charset=UTF-8".toMediaType()))
                    .build()
            client.newCall(req).execute().use { resp ->
                val encryptedBody = resp.body?.string().orEmpty()
                val decodedBody = runCatching { CryptoCodec.decryptBase64AesCbcZeroPadded(encryptedBody) }.getOrNull()
                val body = (decodedBody ?: encryptedBody).trim()
                RequestLogStore.append("venue-probe", "HTTP ${resp.code} decrypt=${decodedBody != null} ${body.take(120)}")
                if (resp.code == 404) error("接口不存在（${RemoteConfig.VENUE_LIST_ENDPOINT}）")
                if (!resp.isSuccessful) error("请求失败 HTTP ${resp.code}")
                val invalidKeywords = listOf("请登录", "未登录", "登录失效", "登录过期", "token失效", "token无效")
                if (invalidKeywords.any { body.contains(it, ignoreCase = true) }) {
                    error("user-token无效或已过期")
                }
                "校验通过"
            }
        }
    }
}
