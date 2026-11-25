package com.example.ding_badminton.data.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RequestLogStore {
    private val fmt = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun append(tag: String, message: String) {
        val normalized = message.replace(Regex("[\\p{Cntrl}&&[^\n\t\r]]"), "�")
        val line = "${LocalDateTime.now().format(fmt)} [$tag] $normalized"
        val next = (_logs.value + line).takeLast(200)
        _logs.value = next
    }
}
