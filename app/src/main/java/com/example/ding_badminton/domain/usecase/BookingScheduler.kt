package com.example.ding_badminton.domain.usecase

import com.example.ding_badminton.data.remote.BookingRemoteDataSource
import com.example.ding_badminton.domain.model.BookingConfig
import com.example.ding_badminton.domain.model.BookingPhase
import com.example.ding_badminton.domain.model.BookingUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BookingScheduler(
    private val remoteDataSource: BookingRemoteDataSource,
    private val validator: BookingValidator,
    private val timeCalculator: BookingTimeCalculator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(BookingUiState())
    val state: StateFlow<BookingUiState> = _state.asStateFlow()

    private var loopJob: Job? = null

    fun start(config: BookingConfig) {
        val check = validator.validate(config)
        if (check.isFailure) {
            _state.value =
                _state.value.copy(
                    running = false,
                    phase = BookingPhase.BLOCKED,
                    lastReason = check.exceptionOrNull()?.message ?: "参数校验失败",
                    config = config,
                )
            return
        }

        loopJob?.cancel()
        loopJob =
            scope.launch {
                val timePlan = timeCalculator.buildPlan(config)
                _state.value =
                    BookingUiState(
                        running = true,
                        phase = BookingPhase.WAITING_OPEN,
                        attempts = 0,
                        openTimeStr = timePlan.openDateTimeStr,
                        targetDateStr = timePlan.targetDateStr,
                        config = config,
                    )

                while (isActive) {
                    val openIn = timeCalculator.secondsUntilOpen(timePlan.openDateTime)
                    _state.value = _state.value.copy(openInSec = openIn)
                    if (openIn <= 0) {
                        break
                    }
                    delay(1000)
                }

                while (isActive) {
                    val attempt = _state.value.attempts + 1
                    if (attempt > 10) {
                        _state.value =
                            _state.value.copy(
                                running = false,
                                phase = BookingPhase.STOPPED,
                                lastReason = "重试10次仍失败，任务已自动取消",
                                nextAttemptInSec = null,
                            )
                        break
                    }
                    _state.value =
                        _state.value.copy(
                            phase = BookingPhase.RETRYING,
                            attempts = attempt,
                            nextAttemptInSec = null,
                            lastReason = "",
                        )
                    val outcome = remoteDataSource.submitOrder(config)
                    if (outcome.success) {
                        _state.value =
                            _state.value.copy(
                                running = false,
                                phase = BookingPhase.SUCCESS,
                                lastReason = outcome.reason,
                                nextAttemptInSec = null,
                            )
                        break
                    }

                    if (!outcome.retryable) {
                        _state.value =
                            _state.value.copy(
                                running = false,
                                phase = BookingPhase.BLOCKED,
                                lastReason = outcome.reason,
                                nextAttemptInSec = null,
                            )
                        break
                    }

                    _state.value = _state.value.copy(lastReason = outcome.reason)
                    for (remain in 60 downTo 1) {
                        if (!isActive) return@launch
                        _state.value = _state.value.copy(nextAttemptInSec = remain)
                        delay(1000)
                    }
                }
            }
    }

    fun stop() {
        stopWithReason("任务已手动停止")
    }

    fun stopWithReason(reason: String) {
        loopJob?.cancel()
        _state.value =
            _state.value.copy(
                running = false,
                phase = BookingPhase.STOPPED,
                lastReason = reason,
                nextAttemptInSec = null,
            )
    }

    fun setMappingSource(source: String) {
        _state.value = _state.value.copy(mappingSource = source)
    }

    fun setDebugLogs(logs: List<String>) {
        _state.value = _state.value.copy(debugLogs = logs)
    }
}
