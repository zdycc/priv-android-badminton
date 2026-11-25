package com.example.ding_badminton.data.submit

import com.example.ding_badminton.data.debug.RequestLogStore
import com.example.ding_badminton.data.remote.RemoteConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class DirectPasswordLoginApi(
    private val entryUrl: String = RemoteConfig.LOGIN_ENTRY_URL,
) {
    private val cookieJar = InMemoryCookieJar()
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

    suspend fun loginAndGetToken(
        account: String,
        password: String,
    ): Result<String> {
        return runCatching {
            val accountTrimmed = account.trim()
            require(accountTrimmed.isNotBlank()) { "账号不能为空" }
            require(password.isNotBlank()) { "密码不能为空" }

            val entryResp =
                client.newCall(
                    Request.Builder().url(entryUrl).get().build(),
                ).execute()
            val entryBody = entryResp.body?.string().orEmpty()
            val finalUrl = entryResp.request.url.toString()
            val entryHeaderToken = readTokenFromSetCookie(entryResp.headers)
            RequestLogStore.append("login-http", "entry_final=$finalUrl token_header=${entryHeaderToken.isNotBlank()}")
            val loginPageUrl =
                when {
                    finalUrl.contains("/cas/login", ignoreCase = true) -> finalUrl
                    else -> extractCasLoginUrl(entryBody).ifBlank { finalUrl }
                }
            entryResp.close()
            val tokenAfterEntry =
                cookieJar.findCookieValue("Auth-Token").ifBlank { entryHeaderToken }
            if (tokenAfterEntry.isNotBlank()) return@runCatching tokenAfterEntry

            val loginResp =
                client.newCall(
                    Request.Builder().url(loginPageUrl).get().build(),
                ).execute()
            val loginHtml = loginResp.body?.string().orEmpty()
            val loginUrl = loginResp.request.url
            val loginHeaderToken = readTokenFromSetCookie(loginResp.headers)
            RequestLogStore.append("login-http", "login_page=${loginUrl} token_header=${loginHeaderToken.isNotBlank()}")
            loginResp.close()
            val tokenAfterLoginPage =
                cookieJar.findCookieValue("Auth-Token").ifBlank { loginHeaderToken }
            if (tokenAfterLoginPage.isNotBlank()) return@runCatching tokenAfterLoginPage
            require(loginUrl.toString().contains("/cas/login", ignoreCase = true)) {
                "未进入CAS登录页：$loginUrl"
            }

            val formAction = extractFormAction(loginHtml).ifBlank { loginUrl.toString() }
            val postUrl = resolveUrl(loginUrl.toString(), formAction)
            val lt = extractInputValue(loginHtml, "lt")
            val execution = extractInputValue(loginHtml, "execution")
            val eventId = extractInputValue(loginHtml, "_eventId").ifBlank { "submit" }
            val dllt = extractInputValue(loginHtml, "dllt").ifBlank { "userNamePasswordLogin" }
            val rmShown = extractInputValue(loginHtml, "rmShown").ifBlank { "1" }
            require(lt.isNotBlank() && execution.isNotBlank()) { "登录页参数缺失：lt/execution" }

            val form =
                FormBody.Builder()
                    .add("username", accountTrimmed)
                    .add("password", password)
                    .add("lt", lt)
                    .add("execution", execution)
                    .add("_eventId", eventId)
                    .add("dllt", dllt)
                    .add("rmShown", rmShown)
                    .build()

            val submitResp =
                client.newCall(
                    Request.Builder()
                        .url(postUrl)
                        .post(form)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build(),
                ).execute()
            val afterLoginUrl = submitResp.request.url.toString()
            val submitBody = submitResp.body?.string().orEmpty()
            val submitHeaderToken = readTokenFromSetCookie(submitResp.headers)
            RequestLogStore.append(
                "login-http",
                "submit_final=$afterLoginUrl token_header=${submitHeaderToken.isNotBlank()} body=${submitBody.take(100)}",
            )
            submitResp.close()

            val token =
                cookieJar.findCookieValue("Auth-Token").ifBlank {
                    submitHeaderToken.ifBlank {
                        findTokenFromUrls(listOf(afterLoginUrl, entryUrl, RemoteConfig.BASE_URL))
                    }
                }
            require(token.isNotBlank()) {
                val isStillCas = afterLoginUrl.contains("/cas/login", ignoreCase = true)
                val hasErrorHint = submitBody.contains("error", ignoreCase = true) || submitBody.contains("失败")
                "未获取到Auth-Token（final=$afterLoginUrl, stillCas=$isStillCas, hasErrorHint=$hasErrorHint）"
            }
            token
        }
    }

    private fun findTokenFromUrls(urls: List<String>): String {
        urls.forEach { raw ->
            val parsed = raw.toHttpUrlOrNull() ?: return@forEach
            val token = cookieJar.loadForRequest(parsed).firstOrNull { it.name == "Auth-Token" }?.value.orEmpty()
            if (token.isNotBlank()) return token
        }
        return ""
    }

    private fun extractCasLoginUrl(html: String): String {
        val regex = Regex("""https://oauth\.zjut\.edu\.cn/cas/login[^\s"'<>]*""")
        return regex.find(html)?.value.orEmpty()
    }

    private fun extractFormAction(html: String): String {
        val regex = Regex("""<form[^>]*action\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun extractInputValue(
        html: String,
        name: String,
    ): String {
        val regex =
            Regex(
                """<input[^>]*name\s*=\s*["']$name["'][^>]*value\s*=\s*["']([^"']*)["']""",
                RegexOption.IGNORE_CASE,
            )
        return regex.find(html)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun resolveUrl(
        baseUrl: String,
        action: String,
    ): String {
        if (action.startsWith("http://") || action.startsWith("https://")) return action
        val base = baseUrl.toHttpUrlOrNull() ?: return baseUrl
        return base.resolve(action)?.toString() ?: baseUrl
    }

    private fun readTokenFromSetCookie(headers: Headers): String {
        val setCookies = headers.values("Set-Cookie")
        for (line in setCookies) {
            val m = Regex("""(?:^|;\s*)Auth-Token=([^;]+)""").find(line)
            if (m != null) return m.groupValues[1]
        }
        return ""
    }

    private class InMemoryCookieJar : CookieJar {
        private val store = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(
            url: HttpUrl,
            cookies: List<Cookie>,
        ) {
            val host = url.host
            val current = store.getOrPut(host) { mutableListOf() }
            cookies.forEach { newCookie ->
                current.removeAll { it.name == newCookie.name && it.path == newCookie.path }
                current.add(newCookie)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            return store.flatMap { (host, cookies) ->
                if (url.host == host || url.host.endsWith(".$host")) {
                    cookies.filter { !it.expiresAt.let { exp -> exp != Long.MAX_VALUE && exp < now } }
                } else {
                    emptyList()
                }
            }
        }

        fun findCookieValue(name: String): String {
            return store.values.flatten().firstOrNull { it.name == name }?.value.orEmpty()
        }
    }
}
