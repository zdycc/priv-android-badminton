package com.example.ding_badminton.data.submit

import com.example.ding_badminton.data.debug.RequestLogStore
import com.example.ding_badminton.data.submit.model.SubmitRequest
import com.example.ding_badminton.data.submit.model.SubmitResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface SubmitApi {
    suspend fun submitVenueOrder(request: SubmitRequest): SubmitResult
}

class PlaceholderSubmitApi : SubmitApi {
    override suspend fun submitVenueOrder(request: SubmitRequest): SubmitResult {
        // M3 占位：后续替换为真实 HTTP 请求。
        return SubmitResult(
            ok = false,
            message = "真实提交接口尚未接入（M3 占位）",
        )
    }
}

class OkHttpSubmitApi(
    private val baseUrl: String,
    private val endpoint: String,
) : SubmitApi {
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

    override suspend fun submitVenueOrder(request: SubmitRequest): SubmitResult {
        return try {
            val url = "${baseUrl.trimEnd('/')}/${endpoint.trimStart('/')}"
            val bodyJson = """{"params":"${request.encryptedParams.replace("\"", "\\\"")}"}"""
            RequestLogStore.append("submit", "POST $url")
            val req =
                Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json;charset=UTF-8")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("User-Token", request.userToken)
                    .post(bodyJson.toRequestBody("application/json;charset=UTF-8".toMediaType()))
                    .build()

            client.newCall(req).execute().use { resp ->
                val encryptedBody = resp.body?.string().orEmpty()
                val decodedBody = runCatching { CryptoCodec.decryptBase64AesCbcZeroPadded(encryptedBody) }.getOrNull()
                val bodyForParse = decodedBody ?: encryptedBody
                val bodyPreview = bodyForParse.toReadablePreview(160)
                RequestLogStore.append(
                    "submit",
                    "HTTP ${resp.code} decrypt=${decodedBody != null} body=$bodyPreview",
                )
                if (!resp.isSuccessful) {
                    val msg =
                        if (decodedBody != null) {
                            "提交失败 HTTP ${resp.code}: ${bodyForParse.toReadablePreview(180)}"
                        } else {
                            "提交失败 HTTP ${resp.code}（响应解密失败，可能是登录态或加解密不匹配）"
                        }
                    return@use SubmitResult(ok = false, message = msg)
                }
                val isSuccess =
                    bodyForParse.contains("\"code\":200") ||
                        bodyForParse.contains("\"code\":6000") ||
                        bodyForParse.contains("\"success\":true") ||
                        bodyForParse.contains("\"msg\":\"success\"") ||
                        bodyForParse.contains("预约成功") ||
                        bodyForParse.contains("已预约")
                val message =
                    when {
                        isSuccess -> "预约提交成功"
                        bodyForParse.contains("已被占用") -> "该时间段已被占用"
                        bodyForParse.contains("请登录") || bodyForParse.contains("未登录") -> "登录态失效，请重新登录"
                        decodedBody == null -> "响应解密失败（可能是登录态过期、密钥不一致或接口变更）"
                        else -> parseServerErrorMessage(bodyForParse)
                    }
                SubmitResult(ok = isSuccess, message = message)
            }
        } catch (e: Exception) {
            RequestLogStore.append("submit", "EX ${e.message ?: e::class.java.simpleName}")
            SubmitResult(ok = false, message = "网络异常：${e.message ?: e::class.java.simpleName}")
        }
    }

    private fun String.toReadablePreview(limit: Int): String {
        val normalized = replace(Regex("[\\p{Cntrl}&&[^\n\t\r]]"), "�").replace("\n", " ").trim()
        return normalized.take(limit)
    }

    private fun parseServerErrorMessage(raw: String): String {
        val text = decodeUnicodeEscapes(raw)
        runCatching {
            val obj = JSONObject(text)
            val code = obj.optString("code")
            val msg = obj.optString("msg")
            if (code == "6000" || msg.equals("success", ignoreCase = true)) {
                return "预约提交成功"
            }
            if (code == "6010") {
                return "提交参数无效（code=6010）：$msg。当前多半是场地映射错误，请先修正 siteId/timeId 映射。"
            }
            if (msg.isNotBlank()) {
                return "提交失败（code=${code.ifBlank { "-" }}）：$msg"
            }
        }
        return text.ifBlank { "提交失败（响应为空）" }.toReadablePreview(220)
    }

    private fun decodeUnicodeEscapes(input: String): String {
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            if (i + 5 < input.length && input[i] == '\\' && input[i + 1] == 'u') {
                val hex = input.substring(i + 2, i + 6)
                val code = hex.toIntOrNull(16)
                if (code != null) {
                    sb.append(code.toChar())
                    i += 6
                    continue
                }
            }
            sb.append(input[i])
            i++
        }
        return sb.toString()
    }
}
