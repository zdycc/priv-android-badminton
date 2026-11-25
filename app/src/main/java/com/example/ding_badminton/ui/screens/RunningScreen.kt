package com.example.ding_badminton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import com.example.ding_badminton.domain.model.BookingUiState

@Composable
fun RunningScreen(
    state: BookingUiState,
    onStop: () -> Unit,
    onViewResult: () -> Unit,
) {
    val topCountdown = (state.openInSec ?: state.nextAttemptInSec)?.toString() ?: "--"
    val topCountdownLabel = if (state.openInSec != null) "距开放（秒）" else "距重试（秒）"
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = statusTop + 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = topCountdown,
                style =
                    MaterialTheme.typography.displayLarge.merge(
                        TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.45f),
                                offset = Offset(0f, 3f),
                                blurRadius = 9f,
                            ),
                        ),
                    ),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = topCountdownLabel,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.24f)),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("运行中", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text("当前状态：${readablePhase(state.phase.name)}", color = Color.White)
                    Text("重试上限：10 次（每次间隔 60 秒）", color = Color.White)
                    Text("目标日期：${state.targetDateStr.ifBlank { "-" }}", color = Color.White)
                    Text("开放时间：${state.openTimeStr.ifBlank { "-" }}", color = Color.White)
                    Text("尝试次数：${state.attempts}", color = Color.White)
                    if (state.lastReason.isNotBlank()) {
                        Text("说明：${state.lastReason}", color = Color.White)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
                PrimaryWideButton(text = "停止任务", onClick = onStop, modifier = Modifier.weight(1f))
                PrimaryWideButton(text = "查看结果", onClick = onViewResult, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun readablePhase(phase: String): String {
    return when (phase) {
        "WAITING_OPEN" -> "等待开放"
        "RETRYING" -> "正在重试"
        "SUCCESS" -> "预约成功"
        "BLOCKED" -> "预约失败"
        "STOPPED" -> "任务停止"
        else -> "运行中"
    }
}
