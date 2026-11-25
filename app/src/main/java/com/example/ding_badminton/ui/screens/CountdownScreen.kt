package com.example.ding_badminton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.ding_badminton.domain.model.BookingConfig

@Composable
fun CountdownScreen(
    config: BookingConfig?,
    openTimeText: String,
    targetDateText: String,
    retryInSec: Int?,
    openInSec: Int?,
    attempts: Int,
    maxAttempts: Int,
    lastReason: String,
    hasLoginState: Boolean,
    hasTaskFile: Boolean,
    alarmScheduled: Boolean,
    onStopBooking: () -> Unit,
    onAddBooking: () -> Unit,
) {
    var showStopConfirm by remember { mutableStateOf(false) }
    val countingRetry = retryInSec != null && retryInSec > 0
    val countdownLabel = if (countingRetry) "距重试" else "距开放"
    val countdownSec = if (countingRetry) retryInSec else openInSec
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(20.dp)
                    .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("正在预约", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            SectionCard(title = "任务信息") {
                Text("目标日期：${targetDateText.ifBlank { "-" }}", color = Color.White)
                Text("开放时间：${openTimeText.ifBlank { "-" }}", color = Color.White)
                Text("$countdownLabel：${countdownSec?.let(::toHhMmSs) ?: "--:--:--"}", color = Color.White)
                Text("重试次数：${attempts}/$maxAttempts（每次间隔60s）", color = Color.White)
                Text(
                    "预约信息：${
                        config?.let {
                            "${it.campusName} / ${it.courtNumber}号 / ${it.timeSlots.joinToString()}"
                        } ?: "-"
                    }",
                    color = Color.White.copy(alpha = 0.92f),
                )
                if (lastReason.isNotBlank()) {
                    Text("失败原因：$lastReason", color = Color.White.copy(alpha = 0.9f))
                }
            }
            SectionCard(title = "状态检查") {
                Text("登录状态：${if (hasLoginState) "已登录" else "未登录"}", color = Color.White)
                Text("任务文件：${if (hasTaskFile) "已生成" else "缺失"}", color = Color.White)
                Text("定时闹钟：${if (alarmScheduled) "已注册" else "未注册"}", color = Color.White)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryWideButton(
                    text = "停止预约",
                    onClick = { showStopConfirm = true },
                    modifier = Modifier.weight(1f),
                )
                PrimaryWideButton(
                    text = "添加预约",
                    onClick = onAddBooking,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text("确认停止预约？") },
            text = { Text("取消预约后将永久删除任务") },
            confirmButton = {
                Button(onClick = {
                    showStopConfirm = false
                    onStopBooking()
                }) { Text("确认") }
            },
            dismissButton = { Button(onClick = { showStopConfirm = false }) { Text("取消") } },
        )
    }
}

private fun toHhMmSs(totalSeconds: Int): String {
    if (totalSeconds <= 0) return "00:00:00"
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
