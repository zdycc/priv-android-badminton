package com.example.ding_badminton.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ding_badminton.domain.model.BookingConfig
import com.example.ding_badminton.domain.policy.CampusPolicy

@Composable
fun SlotStepScreen(
    selectedCampus: String,
    policy: CampusPolicy?,
    selectedCourtNumber: Int?,
    selectedSlots: List<String>,
    targetDayOffset: Int,
    onSelectCell: (courtNumber: Int, slot: String) -> Unit,
    onBack: () -> Unit,
    onSubmit: (BookingConfig) -> Unit,
) {
    var showSlotValidationDialog by remember { mutableStateOf(false) }
    StepScreenEntrance {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier =
                    Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .widthIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StepHeader(step = 3, total = 3, title = "场地与时段")
                SectionCard(title = "场地号与时间段") {
                    val p = policy
                    if (p == null) {
                        Text("请先返回上一步选择校区", color = Color.White.copy(alpha = 0.92f))
                    } else {
                        Text(
                            "校区：$selectedCampus",
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            LegendDot(color = Color(0x665CC8FF))
                            Text("已选择", color = Color.White.copy(alpha = 0.92f))
                            LegendDot(color = Color.White.copy(alpha = 0.08f))
                            Text("可选", color = Color.White.copy(alpha = 0.9f))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val courts = (1..p.maxCourt).toList()
                        val hScroll = rememberScrollState()
                        val firstColW = 112
                        val courtColW = 88
                        val rowH = 48
                        Row(modifier = Modifier.horizontalScroll(hScroll)) {
                            Column {
                                Row {
                                    TableHeaderCell(
                                        text = "时间\\场地",
                                        widthDp = firstColW,
                                        heightDp = rowH,
                                    )
                                    courts.forEach { court ->
                                        TableHeaderCell(
                                            text = "${court}号",
                                            widthDp = courtColW,
                                            heightDp = rowH,
                                        )
                                    }
                                }
                                p.timeSlots.forEach { slot ->
                                    Row {
                                        TableHeaderCell(
                                            text = slot,
                                            widthDp = firstColW,
                                            heightDp = rowH,
                                        )
                                        courts.forEach { court ->
                                            val selected =
                                                selectedCourtNumber == court && selectedSlots.contains(slot)
                                            TableCell(
                                                text = if (selected) "已选" else "",
                                                widthDp = courtColW,
                                                heightDp = rowH,
                                                selected = selected,
                                                onClick = { onSelectCell(court, slot) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            "横向滑动可查看更多场地；当前可选 1~2 个时间段（2个必须连续）",
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        Text(
                            "当前选择：${selectedCourtNumber?.let { "${it}号场" } ?: "-"} / ${
                                if (selectedSlots.isEmpty()) "-" else selectedSlots.joinToString("、")
                            }",
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                val canSubmit = policy != null && selectedSlots.isNotEmpty() && selectedCourtNumber != null
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PrimaryWideButton(text = "上一步", onClick = onBack, modifier = Modifier.weight(1f))
                    PrimaryWideButton(
                        text = "提交",
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit,
                        onClick = {
                            val p = policy ?: return@PrimaryWideButton
                            val court = selectedCourtNumber ?: return@PrimaryWideButton
                            if (!isSlotSelectionValid(p.timeSlots, selectedSlots)) {
                                showSlotValidationDialog = true
                                return@PrimaryWideButton
                            }
                            onSubmit(
                                BookingConfig(
                                    campusName = selectedCampus,
                                    venueName = p.venueName,
                                    courtNumber = court,
                                    timeSlots = selectedSlots,
                                    targetDayOffset = targetDayOffset,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
    if (showSlotValidationDialog) {
        AlertDialog(
            onDismissRequest = { showSlotValidationDialog = false },
            title = { Text("时间段选择有误") },
            text = { Text("选择两个时间段时，必须是相邻时间段，请重新选择。") },
            confirmButton = {
                Button(onClick = { showSlotValidationDialog = false }) { Text("我知道了") }
            },
        )
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier =
            Modifier
                .width(14.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp)),
    )
}

@Composable
private fun TableHeaderCell(
    text: String,
    widthDp: Int,
    heightDp: Int,
) {
    Box(
        modifier =
            Modifier
                .width(widthDp.dp)
                .height(heightDp.dp)
                .border(1.dp, Color.White.copy(alpha = 0.42f))
                .background(Color(0x55B9D5FF))
                .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
        )
    }
}

@Composable
private fun TableCell(
    text: String,
    widthDp: Int,
    heightDp: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(widthDp.dp)
                .height(heightDp.dp)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Color(0xCCBFE8FF) else Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(4.dp),
                )
                .clip(RoundedCornerShape(4.dp))
                .background(if (selected) Color(0x665CC8FF) else Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.95f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun isSlotSelectionValid(
    orderedSlots: List<String>,
    selectedSlots: List<String>,
): Boolean {
    if (selectedSlots.size <= 1) return true
    if (selectedSlots.size != 2) return false
    val first = orderedSlots.indexOf(selectedSlots[0])
    val second = orderedSlots.indexOf(selectedSlots[1])
    if (first < 0 || second < 0) return false
    return kotlin.math.abs(first - second) == 1
}
