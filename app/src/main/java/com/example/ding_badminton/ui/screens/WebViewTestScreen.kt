package com.example.ding_badminton.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ding_badminton.data.remote.RemoteConfig

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewTestScreen(
    onBack: () -> Unit,
) {
    var currentUrl by remember { mutableStateOf(RemoteConfig.LOGIN_ENTRY_URL) }
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("WebView网址测试", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text("当前地址：$currentUrl", color = Color.White.copy(alpha = 0.9f))
        PrimaryWideButton(text = "返回登录页", onClick = onBack, modifier = Modifier.fillMaxWidth())
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webChromeClient = WebChromeClient()
                    webViewClient =
                        object : WebViewClient() {
                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                super.onPageFinished(view, url)
                                currentUrl = url.orEmpty()
                            }
                        }
                    loadUrl(RemoteConfig.LOGIN_ENTRY_URL)
                }
            },
        )
    }
}
