package com.example.ding_badminton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ding_badminton.domain.model.BookingPhase
import com.example.ding_badminton.domain.model.BookingUiState

@Composable
fun ResultScreen(
    state: BookingUiState,
    onConfirmAndBackLogin: () -> Unit,
) {
    val success = state.phase == BookingPhase.SUCCESS
    val title = if (success) "预约成功" else "预约失败"
    val reasonText = if (success) "-" else state.lastReason.ifBlank { "系统未返回具体原因" }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(20.dp)
                    .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            SectionCard(title = "执行结果") {
                Text("状态：${if (success) "successful" else "failed"}", color = Color.White)
                Text("尝试次数：${state.attempts} / 10", color = Color.White.copy(alpha = 0.92f))
                Text("失败原因：$reasonText", color = Color.White)
                Text(
                    "预约信息：${
                        state.config?.let {
                            "${it.campusName} / ${it.courtNumber}号场 / ${it.timeSlots.joinToString()}"
                        } ?: "-"
                    }",
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
            PrimaryWideButton(
                text = "确定并返回登录页面",
                onClick = onConfirmAndBackLogin,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
