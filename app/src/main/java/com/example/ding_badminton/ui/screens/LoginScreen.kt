package com.example.ding_badminton.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ding_badminton.data.remote.RemoteConfig

@Composable
fun LoginScreen(
    hasLoginState: Boolean,
    authCheckMessage: String,
    initialUserToken: String,
    initialVenueParamsRaw: String,
    initialSubmitParamsRaw: String,
    onVerifyAndSave: (String, String, String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var step by remember { mutableStateOf(1) }
    var showReqableGuide by remember { mutableStateOf(false) }
    var userToken by remember(initialUserToken) { mutableStateOf(initialUserToken) }
    var venueParamsRaw by remember(initialVenueParamsRaw) { mutableStateOf(initialVenueParamsRaw) }
    var submitParamsRaw by remember(initialSubmitParamsRaw) { mutableStateOf(initialSubmitParamsRaw) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 14.dp),
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    modifier = Modifier.size(34.dp),
                    onClick = { showReqableGuide = true },
                ) {
                    Text(
                        "❔",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) { 
            Text(
                "ding_badminton",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "登录与授权",
                modifier = Modifier.padding(bottom = 2.dp),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium,
            )
            SectionCard(title = "登录状态") {
                Text(
                    if (hasLoginState) "已校验，可重新校验更新登录状态" else "请按步骤输入参数并保存",
                    color = Color.White.copy(alpha = 0.92f),
                )
                if (step == 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("登录凭证", color = Color.White)
                        IconButton(
                            modifier = Modifier.size(30.dp),
                            onClick = { clipboard.setText(AnnotatedString("getIndexByStaff")) },
                        ) {
                            Text("⧉", color = Color.White)
                        }
                    }
                    Text("找到getIndexByStaff文件，点击标头查看user-token", color = Color.White.copy(alpha = 0.86f))
                    OutlinedTextField(
                        value = userToken,
                        onValueChange = { userToken = it },
                        label = { Text("请输入user-token值（不是关键词）") },
                        placeholder = { Text("例如：VEdULTI0Nzc4MC1vV2dUZjJ3Ym1WZlo5...", color = Color.White.copy(alpha = 0.45f)) },
                        trailingIcon = {
                            IconButton(onClick = {
                                val text = clipboard.getText()?.text.orEmpty()
                                if (text.isNotBlank()) userToken = text
                            }) {
                                Text("📋", color = Color.White)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (step == 2) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("场地接口参数", color = Color.White)
                        IconButton(
                            modifier = Modifier.size(30.dp),
                            onClick = { clipboard.setText(AnnotatedString("getVenueLstByStaff")) },
                        ) {
                            Text("⧉", color = Color.White)
                        }
                    }
                    OutlinedTextField(
                        value = venueParamsRaw,
                        onValueChange = { venueParamsRaw = it },
                        label = { Text("请输入 getVenueLstByStaff 的请求载荷") },
                        placeholder = { Text("""例如：{"params":"xxxx"}""", color = Color.White.copy(alpha = 0.45f)) },
                        trailingIcon = {
                            IconButton(onClick = {
                                val text = clipboard.getText()?.text.orEmpty()
                                if (text.isNotBlank()) venueParamsRaw = text
                            }) {
                                Text("📋", color = Color.White)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                    )
                    Text(
                        "保存前会自动检查JSON格式",
                        color = Color.White.copy(alpha = 0.72f),
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("提交接口参数", color = Color.White)
                        IconButton(
                            modifier = Modifier.size(30.dp),
                            onClick = { clipboard.setText(AnnotatedString("submitVenueOrderByStaff")) },
                        ) {
                            Text("⧉", color = Color.White)
                        }
                    }
                    OutlinedTextField(
                        value = submitParamsRaw,
                        onValueChange = { submitParamsRaw = it },
                        label = { Text("请输入 submitVenueOrderByStaff 的请求载荷") },
                        placeholder = { Text("""例如：{"params":"xxxx"}""", color = Color.White.copy(alpha = 0.45f)) },
                        trailingIcon = {
                            IconButton(onClick = {
                                val text = clipboard.getText()?.text.orEmpty()
                                if (text.isNotBlank()) submitParamsRaw = text
                            }) {
                                Text("📋", color = Color.White)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                    )
                    Text(
                        "保存前会自动检查JSON格式",
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }
                if (authCheckMessage.isNotBlank()) {
                    Text(authCheckMessage, color = Color.White.copy(alpha = 0.92f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PrimaryWideButton(
                        text = "上一步",
                        onClick = { if (step > 1) step -= 1 },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryWideButton(
                        text = if (step < 3) "下一步" else "保存并校验",
                        onClick = {
                            if (step < 3) {
                                step += 1
                            } else {
                                onVerifyAndSave(userToken.trim(), venueParamsRaw.trim(), submitParamsRaw.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showReqableGuide,
            enter =
                fadeIn(animationSpec = tween(durationMillis = 260)) +
                    slideInVertically(
                        initialOffsetY = { it / 8 },
                        animationSpec = tween(durationMillis = 320),
                    ) +
                    expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = 320),
                    ),
            exit =
                fadeOut(animationSpec = tween(durationMillis = 220)) +
                    slideOutVertically(
                        targetOffsetY = { it / 10 },
                        animationSpec = tween(durationMillis = 240),
                    ) +
                    shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = 240),
                    ),
        ) {
            ReqableGuideOverlay(onClose = { showReqableGuide = false })
        }
    }
}

@Composable
private fun ReqableGuideOverlay(
    onClose: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val entryUrl = RemoteConfig.LOGIN_ENTRY_URL
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Reqable 抓包教程",
                    color = Color.Black,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(modifier = Modifier.size(28.dp), onClick = onClose) {
                    Text("✕", color = Color.Black)
                }
            }
            Text("1) 开启开发者模式", color = Color.Black)
            Text(
                "设置 > 关于手机 > 连续点击版本号/内部版本号约7次，返回后进入“开发者选项”，开启 USB 调试（部分机型需再开启“允许安装证书/网络调试”）。",
                color = Color.Black.copy(alpha = 0.82f),
            )
            Text("2) 打开预约入口网址并进入登录流程", color = Color.Black)
            Text(
                "请先在系统浏览器打开以下入口网址，再进行账号登录与后续操作：",
                color = Color.Black.copy(alpha = 0.82f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entryUrl,
                    color = Color(0xFF0B57D0),
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable { uriHandler.openUri(entryUrl) },
                )
            }
            Text(
                "点击上方链接可直接跳转浏览器。\n" +
                    "建议：打开后不要频繁刷新，避免 ticket 失效导致会话不一致。",
                color = Color.Black.copy(alpha = 0.82f),
            )
            Text("3) 配置 Reqable 代理与 HTTPS 解密", color = Color.Black)
            Text(
                "手机安装 Reqable 后启动抓包，按 App 内提示安装并信任证书；若需要抓 HTTPS，请在系统安全设置里确认用户证书已启用。抓取本项目主要接口使用 HTTP 域名，也建议保留 HTTPS 解密能力用于排查跳转链路。",
                color = Color.Black.copy(alpha = 0.82f),
            )
            Text("4) 过滤关键接口", color = Color.Black)
            Text(
                "在 Reqable 过滤框输入：getIndexByStaff、getVenueLstByStaff、submitVenueOrderByStaff。先访问登录页并完成操作，再在会话列表里点开对应请求查看 Header/Body。",
                color = Color.Black.copy(alpha = 0.82f),
            )
            Text("5) 三个参数如何取", color = Color.Black)
            Text(
                "- user-token：在 getIndexByStaff 请求头里复制 user-token 值\n" +
                    "- 场地参数：在 getVenueLstByStaff 请求体复制完整 JSON（通常是 {\"params\":\"...\"}）\n" +
                    "- 提交参数：在 submitVenueOrderByStaff 请求体复制完整 JSON（通常是 {\"params\":\"...\"}）",
                color = Color.Black.copy(alpha = 0.82f),
            )
            Text("6) 本页复制/粘贴按钮怎么用", color = Color.Black)
            Text(
                "标题右侧“⧉”会复制接口关键词，便于你在 Reqable 里快速搜索；输入框右侧“📋”会一键粘贴剪贴板内容。建议每步粘贴后先检查是否有多余空格或缺少大括号。",
                color = Color.Black.copy(alpha = 0.82f),
            )
            Text("7) 常见问题", color = Color.Black)
            Text(
                "- 抓不到请求：确认 Reqable 处于运行状态，且网络流量经过它\n" +
                    "- 校验失败 404：检查接口路径是否正确、是否贴错旧会话\n" +
                    "- JSON 无效：必须粘贴完整请求体，不能只粘 params 字符串",
                color = Color.Black.copy(alpha = 0.82f),
            )
        }
    }
}
