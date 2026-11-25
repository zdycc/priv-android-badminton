package com.example.ding_badminton.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.ding_badminton.domain.model.BookingConfig
import com.example.ding_badminton.domain.policy.CampusPolicy

@Composable
fun ConfigScreen(
    policies: Map<String, CampusPolicy>,
    initialConfig: BookingConfig?,
    useMockRemote: Boolean,
    onToggleUseMockRemote: (Boolean) -> Unit,
    autoResumeEnabled: Boolean,
    onToggleAutoResumeEnabled: (Boolean) -> Unit,
    onStart: (BookingConfig) -> Unit,
) {
    val campuses = policies.keys.toList()
    var selectedCampus by remember { mutableStateOf(initialConfig?.campusName ?: campuses.firstOrNull().orEmpty()) }
    var courtNumberText by remember { mutableStateOf((initialConfig?.courtNumber ?: 1).toString()) }
    var targetDayOffset by remember { mutableIntStateOf(initialConfig?.targetDayOffset ?: 2) }
    val selectedSlots = remember { mutableStateListOf<String>() }
    val policy = policies[selectedCampus]

    if (selectedSlots.isEmpty() && !initialConfig?.timeSlots.isNullOrEmpty()) {
        initialConfig?.timeSlots?.forEach { slot ->
            if (policy?.timeSlots?.contains(slot) == true && selectedSlots.size < 2) {
                selectedSlots.add(slot)
            }
        }
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("参数配置", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Text("使用 Mock 提交（调试推荐）")
            Switch(checked = useMockRemote, onCheckedChange = onToggleUseMockRemote)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Text("自动恢复上次任务")
            Switch(checked = autoResumeEnabled, onCheckedChange = onToggleAutoResumeEnabled)
        }

        Text("1) 校区")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            campuses.forEach { campus ->
                FilterChip(
                    selected = selectedCampus == campus,
                    onClick = {
                        selectedCampus = campus
                        selectedSlots.clear()
                    },
                    label = { Text(campus) },
                )
            }
        }

        Text("2) 日期")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DayChip("今天", 0, targetDayOffset) { targetDayOffset = it }
            DayChip("明天", 1, targetDayOffset) { targetDayOffset = it }
            DayChip("后天", 2, targetDayOffset) { targetDayOffset = it }
        }

        OutlinedTextField(
            value = courtNumberText,
            onValueChange = { courtNumberText = it.filter(Char::isDigit) },
            label = { Text("3) 场地号") },
            supportingText = { Text("当前校区范围：1~${policy?.maxCourt ?: "-"}") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        Text("4) 时间段（最多2个）")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            (policy?.timeSlots ?: emptyList()).forEach { slot ->
                FilterChip(
                    selected = selectedSlots.contains(slot),
                    onClick = {
                        if (selectedSlots.contains(slot)) {
                            selectedSlots.remove(slot)
                        } else if (selectedSlots.size < 2) {
                            selectedSlots.add(slot)
                        }
                    },
                    label = { Text(slot) },
                )
            }
        }

        val canStart = policy != null && selectedSlots.isNotEmpty() && courtNumberText.isNotEmpty()
        Button(
            enabled = canStart,
            onClick = {
                val p = policy ?: return@Button
                val court = courtNumberText.toIntOrNull() ?: return@Button
                onStart(
                    BookingConfig(
                        campusName = selectedCampus,
                        venueName = p.venueName,
                        courtNumber = court,
                        timeSlots = selectedSlots.toList(),
                        targetDayOffset = targetDayOffset,
                    ),
                )
            },
        ) {
            Text("启动预约任务")
        }
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
        label = { Text(label) },
    )
}
