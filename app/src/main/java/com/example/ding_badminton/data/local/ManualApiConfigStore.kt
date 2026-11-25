package com.example.ding_badminton.data.local

import android.content.Context
import org.json.JSONObject
import java.io.File

data class ManualApiConfig(
    val userToken: String,
    val venueParamsRaw: String,
    val submitParamsRaw: String,
)

class ManualApiConfigStore(
    context: Context,
) {
    private val file = File(context.filesDir, "manual_api_config.json")

    fun save(config: ManualApiConfig) {
        val json =
            JSONObject()
                .put("userToken", config.userToken)
                .put("venueParamsRaw", config.venueParamsRaw)
                .put("submitParamsRaw", config.submitParamsRaw)
                .put("updatedAtMs", System.currentTimeMillis())
        file.writeText(json.toString())
    }

    fun load(): ManualApiConfig? {
        if (!file.exists()) return null
        val text = file.readText().trim()
        if (text.isBlank()) return null
        return runCatching {
            val json = JSONObject(text)
            ManualApiConfig(
                userToken = json.optString("userToken").trim(),
                venueParamsRaw = json.optString("venueParamsRaw"),
                submitParamsRaw = json.optString("submitParamsRaw"),
            )
        }.getOrNull()
    }
}
