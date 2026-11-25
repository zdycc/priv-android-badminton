package com.example.ding_badminton.data.submit

import com.example.ding_badminton.data.remote.meta.VenueMetadataRepository
import com.example.ding_badminton.data.submit.model.SubmitPayload
import com.example.ding_badminton.data.submit.model.TimeArrItem
import com.example.ding_badminton.domain.model.BookingConfig
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PayloadBuilder {
    suspend fun build(config: BookingConfig): SubmitPayload
}

class DefaultPayloadBuilder(
    private val venueMetadataRepository: VenueMetadataRepository,
) : PayloadBuilder {
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val dayFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")
    private val _mappingSource = MutableStateFlow("unknown")
    val mappingSource: StateFlow<String> = _mappingSource.asStateFlow()

    override suspend fun build(config: BookingConfig): SubmitPayload {
        val mapping = venueMetadataRepository.resolveMapping(config)
        _mappingSource.value = mapping.source
        val timeArr =
            config.timeSlots.map { slot ->
                val timeId = mapping.slotToTimeId[slot] ?: "1"
                val resolvedSiteId = mapping.courtToSiteId[config.courtNumber] ?: config.courtNumber.toString()
                TimeArrItem(
                    siteId = resolvedSiteId,
                    timeId = timeId,
                )
            }
        val targetDay =
            LocalDate.now(zoneId)
                .plusDays(config.targetDayOffset.toLong())
                .format(dayFormatter)

        return SubmitPayload(
            id = mapping.venueId,
            day = targetDay,
            timeArr = timeArr,
        )
    }
}
