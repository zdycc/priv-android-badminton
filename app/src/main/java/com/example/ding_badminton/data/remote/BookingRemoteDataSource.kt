package com.example.ding_badminton.data.remote

import com.example.ding_badminton.domain.model.BookingConfig
import com.example.ding_badminton.domain.model.BookingOutcome

interface BookingRemoteDataSource {
    suspend fun submitOrder(config: BookingConfig): BookingOutcome
}

class SwitchableBookingRemoteDataSource(
    private val mock: BookingRemoteDataSource,
    private val real: BookingRemoteDataSource,
) : BookingRemoteDataSource {
    @Volatile
    private var useMock: Boolean = true

    fun setUseMock(useMock: Boolean) {
        this.useMock = useMock
    }

    override suspend fun submitOrder(config: BookingConfig): BookingOutcome {
        return if (useMock) mock.submitOrder(config) else real.submitOrder(config)
    }
}

class MockBookingRemoteDataSource : BookingRemoteDataSource {
    private var counter: Int = 0

    override suspend fun submitOrder(config: BookingConfig): BookingOutcome {
        counter += 1
        return when {
            counter % 5 == 0 -> BookingOutcome(success = true, reason = "预约成功（M1 模拟）", retryable = false)
            counter % 2 == 0 -> BookingOutcome(success = false, reason = "系统繁忙，请稍后再试", retryable = true)
            else -> BookingOutcome(success = false, reason = "当前请求拥堵，稍后重试", retryable = true)
        }
    }
}
