package com.example.ding_badminton.domain.usecase

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ding_badminton.alarm.BookingAlarmReceiver
import com.example.ding_badminton.domain.model.BookingConfig
import java.time.ZoneId

class BookingAlarmScheduler(
    private val context: Context,
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)
    private val timeCalculator = BookingTimeCalculator()

    fun schedule(config: BookingConfig): Boolean {
        val openDateTime = timeCalculator.buildPlan(config).openDateTime
        val triggerAtMillis = openDateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        val intent = pendingIntent()
        return runCatching {
            val canExact =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    intent,
                )
                true
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    intent,
                )
                false
            }
        }.getOrElse {
            false
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, BookingAlarmReceiver::class.java).apply {
            action = BookingAlarmReceiver.ACTION_AUTO_BOOKING
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val REQUEST_CODE = 60101
    }
}
