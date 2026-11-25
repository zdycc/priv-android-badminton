package com.example.ding_badminton.data.local

import android.content.Context
import org.json.JSONObject
import java.io.File

class LoginCredentialFileStore(
    context: Context,
) {
    private val file = File(context.filesDir, "login_credential.sec")

    fun save(
        account: String,
        passwordEncrypted: String,
    ) {
        val json =
            JSONObject()
                .put("account", account)
                .put("passwordEncrypted", passwordEncrypted)
                .put("updatedAtMs", System.currentTimeMillis())
        file.writeText(json.toString())
    }

    fun loadAccount(): String {
        val json = loadJsonOrNull() ?: return ""
        return json.optString("account").trim()
    }

    fun loadPasswordEncrypted(): String {
        val json = loadJsonOrNull() ?: return ""
        return json.optString("passwordEncrypted")
    }

    private fun loadJsonOrNull(): JSONObject? {
        if (!file.exists()) return null
        val text = file.readText().trim()
        if (text.isBlank()) return null
        return runCatching { JSONObject(text) }.getOrNull()
    }
}
