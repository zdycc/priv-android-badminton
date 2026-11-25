package com.example.ding_badminton.data.submit

import com.example.ding_badminton.data.debug.RequestLogStore
import com.example.ding_badminton.data.remote.RemoteConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AuthCheckApi(
    private val baseUrl: String = RemoteConfig.BASE_URL,
) {
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()

    suspend fun checkToken(token: String): Result<String> {
        return runCatching {
            val baseCandidates =
                listOf(
                    baseUrl.replaceFirst("https://", "http://"),
                ).distinct()
            val endpoints =
                listOf(
                    RemoteConfig.LOGIN_CHECK_ENDPOINT,
                    "/staff.php/getInfoByStaff",
                    "/staff.php/getinfoByStaff",
                    "/staff.php/getIndexByStaff",
                    "/staff.php/getindexByStaff",
                ).distinct()
            var lastError = "登录态校验失败"
            for (base in baseCandidates) {
                for (endpoint in endpoints) {
                    val url = "${base.trimEnd('/')}/${endpoint.trimStart('/')}"
                    RequestLogStore.append("auth-check", "GET $url")
                    val req =
                        Request.Builder()
                            .url(url)
                            .addHeader("Accept", "application/json, text/plain, */*")
                            .addHeader("User-Token", token)
                            .get()
                            .build()
                    client.newCall(req).execute().use { resp ->
                        val encryptedBody = resp.body?.string().orEmpty()
                        val decodedBody = runCatching { CryptoCodec.decryptBase64AesCbcZeroPadded(encryptedBody) }.getOrNull()
                        val body = decodedBody ?: encryptedBody
                        RequestLogStore.append("auth-check", "HTTP ${resp.code} endpoint=$endpoint base=${base.take(5)} decrypt=${decodedBody != null} ${body.take(120)}")
                        if (resp.code == 404) {
                            lastError = "接口不存在（$endpoint）"
                            return@use
                        }
                        if (!resp.isSuccessful) {
                            lastError = "登录态校验失败 HTTP ${resp.code}"
                            return@use
                        }
                        val invalidKeywords = listOf("请登录", "未登录", "登录失效", "登录过期", "token失效", "token无效")
                        if (invalidKeywords.any { body.contains(it, ignoreCase = true) }) {
                            lastError = "登录态无效或已过期"
                            return@use
                        }
                        return@runCatching "登录态有效"
                    }
                }
            }
            error(lastError)
        }
    }
}
