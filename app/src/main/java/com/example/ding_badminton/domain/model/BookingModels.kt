package com.example.ding_badminton.domain.model

data class BookingConfig(
    val campusName: String,
    val venueName: String,
    val courtNumber: Int,
    val timeSlots: List<String>,
    val targetDayOffset: Int,
    val timeoutMs: Int = 8000,
)

data class BookingOutcome(
    val success: Boolean,
    val reason: String,
    val retryable: Boolean,
)

enum class BookingPhase {
    IDLE,
    LOGIN,
    READY,
    WAITING_OPEN,
    RETRYING,
    SUCCESS,
    BLOCKED,
    STOPPED,
}

data class BookingUiState(
    val running: Boolean = false,
    val phase: BookingPhase = BookingPhase.IDLE,
    val attempts: Int = 0,
    val lastReason: String = "",
    val nextAttemptInSec: Int? = null,
    val openInSec: Int? = null,
    val openTimeStr: String = "",
    val targetDateStr: String = "",
    val mappingSource: String = "unknown",
    val debugLogs: List<String> = emptyList(),
    val config: BookingConfig? = null,
)
