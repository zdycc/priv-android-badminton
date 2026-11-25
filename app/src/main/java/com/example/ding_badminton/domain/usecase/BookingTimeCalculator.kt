package com.example.ding_badminton.domain.usecase

import com.example.ding_badminton.domain.model.BookingConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BookingTimeCalculator {
    private val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    data class TimePlan(
        val targetDate: LocalDate,
        val openDateTime: LocalDateTime,
        val targetDateStr: String,
        val openDateTimeStr: String,
    )

    fun buildPlan(config: BookingConfig): TimePlan {
        require(config.targetDayOffset in 0..2) { "targetDayOffset 必须为 0/1/2" }
        val now = LocalDateTime.now(zoneId)
        val today = now.toLocalDate()
        val targetDate = today.plusDays(config.targetDayOffset.toLong())
        val openDay = if (config.targetDayOffset == 2) today.plusDays(1) else today
        val openDateTime = LocalDateTime.of(openDay, LocalTime.of(7, 0, 5))
        return TimePlan(
            targetDate = targetDate,
            openDateTime = openDateTime,
            targetDateStr = targetDate.format(dateFormatter),
            openDateTimeStr = openDateTime.format(dateTimeFormatter),
        )
    }

    fun secondsUntilOpen(openDateTime: LocalDateTime): Int {
        val now = LocalDateTime.now(zoneId)
        val seconds = java.time.Duration.between(now, openDateTime).seconds
        return if (seconds <= 0L) 0 else seconds.toInt()
    }
}
