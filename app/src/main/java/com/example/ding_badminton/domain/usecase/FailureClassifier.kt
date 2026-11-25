package com.example.ding_badminton.domain.usecase

import com.example.ding_badminton.domain.model.BookingOutcome

class FailureClassifier {
    fun classify(rawMessage: String): BookingOutcome {
        val msg = rawMessage.trim()
        if (msg.contains("已被占用")) {
            return BookingOutcome(success = false, reason = "该时间段已被占用", retryable = false)
        }
        if (
            msg.contains("已满") ||
            msg.contains("名额已满") ||
            msg.contains("预约已满") ||
            msg.contains("余量不足") ||
            msg.contains("库存不足") ||
            msg.contains("不可预约") ||
            msg.contains("暂无名额")
        ) {
            return BookingOutcome(success = false, reason = msg.ifBlank { "该时段或场地暂不可预约" }, retryable = false)
        }
        if (
            msg.contains("6010") ||
            msg.contains("参数无效") ||
            msg.contains("场地映射")
        ) {
            return BookingOutcome(success = false, reason = msg, retryable = false)
        }
        if (
            msg.contains("请登录") ||
            msg.contains("登录失效") ||
            msg.contains("未登录") ||
            msg.contains("登录失败") ||
            msg.contains("登录态")
        ) {
            return BookingOutcome(success = false, reason = "用户登录失败或登录态失效", retryable = false)
        }
        return BookingOutcome(success = false, reason = if (msg.isEmpty()) "预约失败（未知原因）" else msg, retryable = true)
    }
}
