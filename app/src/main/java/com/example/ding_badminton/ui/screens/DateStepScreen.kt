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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DateStepScreen(
    selectedDayOffset: Int,
    onSelectDayOffset: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    immediateOnly: Boolean,
    isImmediateDate: (Int) -> Boolean,
) {
    var showInvalidDialog by remember { mutableStateOf(false) }
    StepScreenEntrance {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Column(
                modifier =
                    Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StepHeader(step = 2, total = 3, title = "选择日期")
                SectionCard(title = "预约日期") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        DayChip("今天", 0, selectedDayOffset, onSelectDayOffset)
                        DayChip("明天", 1, selectedDayOffset, onSelectDayOffset)
                        DayChip("后天", 2, selectedDayOffset, onSelectDayOffset)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PrimaryWideButton(text = "上一步", onClick = onBack, modifier = Modifier.weight(1f))
                    PrimaryWideButton(
                        text = "下一步",
                        onClick = {
                            if (immediateOnly && !isImmediateDate(selectedDayOffset)) {
                                showInvalidDialog = true
                            } else {
                                onNext()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    if (showInvalidDialog) {
        AlertDialog(
            onDismissRequest = { showInvalidDialog = false },
            title = { Text("提示") },
            text = { Text("您已经有一个计时预约了，不可再次选中") },
            confirmButton = { Button(onClick = { showInvalidDialog = false }) { Text("确认") } },
        )
    }
}

@Composable
private fun DayChip(
    label: String,
    offset: Int,
    selectedOffset: Int,
    onPick: (Int) -> Unit,
) {
    FilterChip(
        selected = selectedOffset == offset,
        onClick = { onPick(offset) },
        label = { Text(label, color = Color.White) },
    )
}
