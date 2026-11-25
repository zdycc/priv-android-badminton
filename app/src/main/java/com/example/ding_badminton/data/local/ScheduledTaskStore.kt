package com.example.ding_badminton.data.local

import android.content.Context
import com.example.ding_badminton.domain.model.BookingConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ScheduledTaskRecord(
    val loginAccount: String,
    val passwordEncrypted: String,
    val campusName: String,
    val venueName: String,
    val courtNumber: Int,
    val timeSlots: List<String>,
    val targetDayOffset: Int,
    val createdAtMs: Long,
)

class ScheduledTaskStore(
    context: Context,
) {
    private val taskDir: File = File(context.filesDir, "booking_tasks").apply { mkdirs() }
    private val pointerFile: File = File(context.filesDir, "latest_booking_task.pointer")

    fun save(record: ScheduledTaskRecord) {
        val targetFile = File(taskDir, buildFileName(record))
        val json =
            JSONObject()
                .put("loginAccount", record.loginAccount)
                .put("passwordEncrypted", record.passwordEncrypted)
                .put("campusName", record.campusName)
                .put("venueName", record.venueName)
                .put("courtNumber", record.courtNumber)
                .put("timeSlots", JSONArray(record.timeSlots))
                .put("targetDayOffset", record.targetDayOffset)
                .put("createdAtMs", record.createdAtMs)
        targetFile.writeText(json.toString())
        pointerFile.writeText(targetFile.name)
    }

    fun load(): ScheduledTaskRecord? {
        val taskFile = currentTaskFile() ?: return null
        if (!taskFile.exists()) return null
        val text = taskFile.readText().trim()
        if (text.isBlank()) return null
        return runCatching {
            val json = JSONObject(text)
            val slots = mutableListOf<String>()
            val slotsArr = json.optJSONArray("timeSlots") ?: JSONArray()
            for (i in 0 until slotsArr.length()) {
                slots += slotsArr.optString(i)
            }
            ScheduledTaskRecord(
                loginAccount = json.optString("loginAccount"),
                passwordEncrypted = json.optString("passwordEncrypted"),
                campusName = json.optString("campusName"),
                venueName = json.optString("venueName"),
                courtNumber = json.optInt("courtNumber", 1),
                timeSlots = slots.filter { it.isNotBlank() },
                targetDayOffset = json.optInt("targetDayOffset", 2),
                createdAtMs = json.optLong("createdAtMs", 0L),
            )
        }.getOrNull()
    }

    fun delete() {
        currentTaskFile()?.takeIf { it.exists() }?.delete()
        if (pointerFile.exists()) pointerFile.delete()
    }

    private fun currentTaskFile(): File? {
        if (!pointerFile.exists()) return null
        val fileName = pointerFile.readText().trim()
        if (fileName.isBlank()) return null
        return File(taskDir, fileName)
    }

    private fun buildFileName(record: ScheduledTaskRecord): String {
        val accountLast4 = record.loginAccount.takeLast(4).ifBlank { "0000" }
        val campusInitial =
            when {
                record.campusName.contains("莫") -> "M"
                record.campusName.contains("屏") -> "P"
                else -> record.campusName.take(1).uppercase()
            }
        val slotCode =
            record.timeSlots
                .sorted()
                .mapNotNull(::extractHour)
                .joinToString("")
                .ifBlank { "0" }
        return "${accountLast4}-${campusInitial}-${record.courtNumber}-${slotCode}.json"
    }

    private fun extractHour(slot: String): String? {
        val start = slot.substringBefore("-").trim()
        val hour = start.substringBefore(":").toIntOrNull() ?: return null
        return hour.toString()
    }
}
