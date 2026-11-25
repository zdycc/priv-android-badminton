package com.example.ding_badminton.ui.model

data class BookingDraft(
    val campusName: String = "",
    val targetDayOffset: Int = 2,
    val courtNumberText: String = "1",
    val selectedSlots: List<String> = emptyList(),
)
