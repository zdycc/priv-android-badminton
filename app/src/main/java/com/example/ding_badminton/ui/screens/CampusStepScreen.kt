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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ding_badminton.domain.policy.CampusPolicy

@Composable
fun CampusStepScreen(
    policies: Map<String, CampusPolicy>,
    selectedCampus: String,
    onSelectCampus: (String) -> Unit,
    onNext: () -> Unit,
    onViewMyBooking: () -> Unit,
    hasPendingTask: Boolean,
) {
    val campuses = policies.keys.toList()
    var showNoTaskDialog by remember { mutableStateOf(false) }
    StepScreenEntrance {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Column(
                modifier =
                    Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .widthIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StepHeader(step = 1, total = 3, title = "选择校区")
                SectionCard(title = "校区") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        campuses.forEach { campus ->
                            FilterChip(
                                selected = selectedCampus == campus,
                                onClick = { onSelectCampus(campus) },
                                label = { Text(campus) },
                            )
                        }
                    }
                }
                PrimaryWideButton(
                    text = "下一步",
                    enabled = selectedCampus.isNotBlank(),
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryWideButton(
                    text = "查看我的预约",
                    onClick = {
                        if (hasPendingTask) {
                            onViewMyBooking()
                        } else {
                            showNoTaskDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (showNoTaskDialog) {
        AlertDialog(
            onDismissRequest = { showNoTaskDialog = false },
            title = { Text("提示") },
            text = { Text("查询不到未预约文件，请重新预约") },
            confirmButton = { Button(onClick = { showNoTaskDialog = false }) { Text("确认") } },
        )
    }
}
