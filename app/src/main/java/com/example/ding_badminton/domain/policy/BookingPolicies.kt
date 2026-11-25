package com.example.ding_badminton.domain.policy

data class CampusPolicy(
    val venueName: String,
    val maxCourt: Int,
    val timeSlots: List<String>,
)

object BookingPolicies {
    val campusPolicies: Map<String, CampusPolicy> =
        mapOf(
            "屏峰校区" to
                CampusPolicy(
                    venueName = "小馆羽毛球场",
                    maxCourt = 6,
                    timeSlots = listOf("08:00-09:00", "09:00-10:00", "12:00-13:00", "17:00-18:00"),
                ),
            "莫干山校区" to
                CampusPolicy(
                    venueName = "羽毛球馆",
                    maxCourt = 9,
                    timeSlots =
                        listOf(
                            "08:00-09:00",
                            "09:00-10:00",
                            "10:00-11:00",
                            "11:00-12:00",
                            "12:00-13:00",
                            "13:00-14:00",
                            "14:00-15:00",
                            "15:00-16:00",
                            "16:00-17:00",
                            "17:00-18:00",
                            "18:00-19:00",
                            "19:00-20:00",
                            "20:00-21:00",
                        ),
                ),
        )
}
