package com.example.ding_badminton.data.submit.model

data class TimeArrItem(
    val siteId: String,
    val timeId: String,
)

data class SubmitPayload(
    val id: String,
    val day: String,
    val mark: String = "",
    val timeArr: List<TimeArrItem>,
    val people: List<CompanionPerson> = listOf(CompanionPerson()),
)

data class CompanionPerson(
    val name: String = "",
    val phone: String = "",
)

data class SubmitRequest(
    val encryptedParams: String,
    val userToken: String,
)

data class SubmitResult(
    val ok: Boolean,
    val message: String,
)
