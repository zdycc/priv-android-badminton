package com.example.ding_badminton.data.local

import android.content.Context
import org.json.JSONObject
import java.io.File

class LoginValidationTempStore(
    context: Context,
) {
    private val file = File(context.filesDir, "login_validation_temp.sec")

    fun save(
        account: String,
        passwordEncrypted: String,
    ) {
        val json =
            JSONObject()
                .put("account", account.trim())
                .put("passwordEncrypted", passwordEncrypted)
                .put("createdAtMs", System.currentTimeMillis())
        file.writeText(json.toString())
    }

    fun delete() {
        if (file.exists()) file.delete()
    }
}
