package com.example.ding_badminton.domain.usecase

import com.example.ding_badminton.domain.model.BookingConfig
import com.example.ding_badminton.domain.policy.BookingPolicies

class BookingValidator {
    fun validate(config: BookingConfig): Result<Unit> {
        val policy = BookingPolicies.campusPolicies[config.campusName]
            ?: return Result.failure(IllegalArgumentException("未知校区：${config.campusName}"))
        if (config.targetDayOffset !in 0..2) {
            return Result.failure(IllegalArgumentException("日期仅支持今天/明天/后天"))
        }
        if (config.courtNumber !in 1..policy.maxCourt) {
            return Result.failure(IllegalArgumentException("场地号范围应为 1~${policy.maxCourt}"))
        }
        if (config.timeSlots.isEmpty() || config.timeSlots.size > 2) {
            return Result.failure(IllegalArgumentException("时间段必须选择 1~2 个"))
        }
        if (!policy.timeSlots.containsAll(config.timeSlots)) {
            return Result.failure(IllegalArgumentException("存在不在当前校区范围内的时间段"))
        }
        if (config.timeSlots.size == 2) {
            val sortedIndex =
                config.timeSlots
                    .map { slot -> policy.timeSlots.indexOf(slot) }
                    .sorted()
            if (sortedIndex.size == 2 && sortedIndex[1] - sortedIndex[0] != 1) {
                return Result.failure(IllegalArgumentException("两个时间段必须连续（例如 14:00-15:00 与 15:00-16:00）"))
            }
        }
        return Result.success(Unit)
    }
}
