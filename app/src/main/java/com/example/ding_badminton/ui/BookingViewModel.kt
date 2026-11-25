package com.example.ding_badminton.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ding_badminton.data.local.ScheduledTaskRecord
import com.example.ding_badminton.data.local.ScheduledTaskStore
import com.example.ding_badminton.data.local.UserPrefsRepository
import com.example.ding_badminton.data.local.ManualApiConfig
import com.example.ding_badminton.data.local.ManualApiConfigStore
import com.example.ding_badminton.data.debug.RequestLogStore
import com.example.ding_badminton.data.remote.BookingRemoteDataSource
import com.example.ding_badminton.data.remote.RealBookingRemoteDataSource
import com.example.ding_badminton.data.remote.meta.ApiVenueMetadataRepository
import com.example.ding_badminton.data.remote.meta.CompositeVenueMetadataRepository
import com.example.ding_badminton.data.remote.meta.FallbackVenueMetadataRepository
import com.example.ding_badminton.data.submit.DefaultPayloadBuilder
import com.example.ding_badminton.data.submit.LocalTokenProvider
import com.example.ding_badminton.data.submit.OkHttpSubmitApi
import com.example.ding_badminton.data.submit.AesCbcZeroPaddingSigner
import com.example.ding_badminton.data.submit.AuthCheckApi
import com.example.ding_badminton.data.submit.DirectPasswordLoginApi
import com.example.ding_badminton.data.submit.VenueProbeApi
import com.example.ding_badminton.data.remote.RemoteConfig
import com.example.ding_badminton.domain.model.BookingConfig
import com.example.ding_badminton.domain.model.BookingPhase
import com.example.ding_badminton.domain.model.BookingUiState
import com.example.ding_badminton.domain.policy.BookingPolicies
import com.example.ding_badminton.domain.usecase.BookingScheduler
import com.example.ding_badminton.domain.usecase.BookingAlarmScheduler
import com.example.ding_badminton.domain.usecase.BookingTimeCalculator
import com.example.ding_badminton.domain.usecase.FailureClassifier
import com.example.ding_badminton.domain.usecase.BookingValidator
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookingViewModel(
    private val userPrefsRepository: UserPrefsRepository,
    private val scheduledTaskStore: ScheduledTaskStore,
    private val bookingAlarmScheduler: BookingAlarmScheduler,
    private val manualApiConfigStore: ManualApiConfigStore,
) : ViewModel() {
    private val authCheckApi = AuthCheckApi()
    private val directPasswordLoginApi = DirectPasswordLoginApi()
    private val venueProbeApi = VenueProbeApi()
    val policies = BookingPolicies.campusPolicies
    private val _hasLoginState = MutableStateFlow(false)
    val hasLoginState: StateFlow<Boolean> = _hasLoginState.asStateFlow()
    private val _latestConfig = MutableStateFlow<BookingConfig?>(null)
    val latestConfig: StateFlow<BookingConfig?> = _latestConfig.asStateFlow()
    private val _userToken = MutableStateFlow("")
    val userToken: StateFlow<String> = _userToken.asStateFlow()
    private val _loginAccount = MutableStateFlow("")
    val loginAccount: StateFlow<String> = _loginAccount.asStateFlow()
    private val _loginPassword = MutableStateFlow("")
    val loginPassword: StateFlow<String> = _loginPassword.asStateFlow()
    private val _autoResumeEnabled = MutableStateFlow(true)
    val autoResumeEnabled: StateFlow<Boolean> = _autoResumeEnabled.asStateFlow()
    private val _authCheckMessage = MutableStateFlow("")
    val authCheckMessage: StateFlow<String> = _authCheckMessage.asStateFlow()
    private val _shouldResumeTask = MutableStateFlow(false)
    private val _scheduledTask = MutableStateFlow<ScheduledTaskRecord?>(null)
    val scheduledTask: StateFlow<ScheduledTaskRecord?> = _scheduledTask.asStateFlow()
    private val _alarmScheduled = MutableStateFlow(false)
    val alarmScheduled: StateFlow<Boolean> = _alarmScheduled.asStateFlow()
    private val _manualApiConfig = MutableStateFlow<ManualApiConfig?>(null)
    val manualApiConfig: StateFlow<ManualApiConfig?> = _manualApiConfig.asStateFlow()
    private var resumeConsumed = false
    private var hasLoginStateRaw = false
    private var successCleanupDone = false
    private val payloadBuilder =
        DefaultPayloadBuilder(
            CompositeVenueMetadataRepository(
                primary = ApiVenueMetadataRepository(),
                fallback = FallbackVenueMetadataRepository(),
            ),
        )
    private val bookingRemoteDataSource: BookingRemoteDataSource =
        RealBookingRemoteDataSource(
            tokenProvider = LocalTokenProvider(userPrefsRepository),
            payloadBuilder = payloadBuilder,
            cryptoSigner = AesCbcZeroPaddingSigner(),
            submitApi =
                OkHttpSubmitApi(
                    baseUrl = RemoteConfig.BASE_URL,
                    endpoint = RemoteConfig.SUBMIT_ENDPOINT,
                ),
            failureClassifier = FailureClassifier(),
        )

    private val scheduler =
        BookingScheduler(
            remoteDataSource = bookingRemoteDataSource,
            validator = BookingValidator(),
            timeCalculator = BookingTimeCalculator(),
        )

    val uiState: StateFlow<BookingUiState> = scheduler.state

    init {
        _scheduledTask.value = scheduledTaskStore.load()
        _manualApiConfig.value = manualApiConfigStore.load()
        viewModelScope.launch {
            userPrefsRepository.hasLoginStateFlow.collectLatest {
                hasLoginStateRaw = it
                refreshLoginReadyState()
            }
        }
        viewModelScope.launch {
            userPrefsRepository.latestConfigFlow.collectLatest { _latestConfig.value = it }
        }
        viewModelScope.launch {
            userPrefsRepository.userTokenFlow.collectLatest {
                _userToken.value = it.orEmpty()
                refreshLoginReadyState()
            }
        }
        viewModelScope.launch {
            userPrefsRepository.loginAccountFlow.collectLatest { _loginAccount.value = it }
        }
        viewModelScope.launch {
            val savedAccount = userPrefsRepository.getSavedAccount()
            if (savedAccount.isNotBlank()) {
                _loginAccount.value = savedAccount
            }
        }
        viewModelScope.launch {
            _loginPassword.value = userPrefsRepository.getSavedPassword()
        }
        viewModelScope.launch {
            userPrefsRepository.autoResumeEnabledFlow.collectLatest { enabled ->
                _autoResumeEnabled.value = enabled
                if (!enabled) userPrefsRepository.saveShouldResumeTask(false)
            }
        }
        viewModelScope.launch {
            userPrefsRepository.shouldResumeTaskFlow.collectLatest { shouldResume ->
                _shouldResumeTask.value = shouldResume
                tryAutoResume()
            }
        }
        viewModelScope.launch {
            userPrefsRepository.alarmScheduledFlow.collectLatest { _alarmScheduled.value = it }
        }
        viewModelScope.launch {
            payloadBuilder.mappingSource.collectLatest { source ->
                scheduler.setMappingSource(source)
            }
        }
        viewModelScope.launch {
            RequestLogStore.logs.collectLatest { logs ->
                scheduler.setDebugLogs(logs)
            }
        }
        viewModelScope.launch {
            uiState.collectLatest { state ->
                when (state.phase) {
                    BookingPhase.SUCCESS -> {
                        userPrefsRepository.saveShouldResumeTask(false)
                        if (!successCleanupDone) {
                            successCleanupDone = true
                            clearScheduledTaskAfterSuccess()
                        }
                    }
                    BookingPhase.BLOCKED, BookingPhase.STOPPED -> {
                        userPrefsRepository.saveShouldResumeTask(false)
                    }
                    else -> Unit
                }
                tryAutoResume()
            }
        }
    }

    fun saveLoginForm(
        account: String,
        password: String,
    ) {
        viewModelScope.launch {
            userPrefsRepository.saveLoginForm(account = account, password = password)
            _loginAccount.value = account.trim()
            _loginPassword.value = password
        }
    }

    fun setLoginFeedback(message: String) {
        _authCheckMessage.value = message
    }

    fun saveManualApiConfig(
        userToken: String,
        venueParamsRaw: String,
        submitParamsRaw: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val token = userToken.trim()
            if (token.isBlank()) {
                _authCheckMessage.value = "请先输入user-token"
                onResult(false)
                return@launch
            }
            if (venueParamsRaw.isBlank()) {
                _authCheckMessage.value = "请填写 getVenueLstByStaff 参数"
                onResult(false)
                return@launch
            }
            if (submitParamsRaw.isBlank()) {
                _authCheckMessage.value = "请填写 submitVenueOrderByStaff 参数"
                onResult(false)
                return@launch
            }
            val venueJsonCheck = validateJsonText(venueParamsRaw)
            if (venueJsonCheck != null) {
                _authCheckMessage.value = "getVenueLstByStaff 参数格式错误：$venueJsonCheck"
                onResult(false)
                return@launch
            }
            val submitJsonCheck = validateJsonText(submitParamsRaw)
            if (submitJsonCheck != null) {
                _authCheckMessage.value = "submitVenueOrderByStaff 参数格式错误：$submitJsonCheck"
                onResult(false)
                return@launch
            }
            val check = withContext(Dispatchers.IO) { venueProbeApi.probe(token, venueParamsRaw) }
            if (check.isFailure) {
                userPrefsRepository.markLoginState(false)
                _authCheckMessage.value = "校验失败：${check.exceptionOrNull()?.message ?: "user-token校验失败"}"
                onResult(false)
                return@launch
            }
            userPrefsRepository.saveUserToken(token)
            userPrefsRepository.markLoginState(true)
            val config =
                ManualApiConfig(
                    userToken = token,
                    venueParamsRaw = venueParamsRaw,
                    submitParamsRaw = submitParamsRaw,
                )
            manualApiConfigStore.save(config)
            _manualApiConfig.value = config
            _authCheckMessage.value = "校验成功并已保存参数"
            onResult(true)
        }
    }

    fun submitImmediateBooking(
        config: BookingConfig,
        onDone: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { bookingRemoteDataSource.submitOrder(config) }
            val msg =
                if (outcome.success) {
                    "直接预约成功"
                } else {
                    "直接预约失败：${outcome.reason}"
                }
            _authCheckMessage.value = msg
            onDone(outcome.success, msg)
        }
    }

    private fun validateJsonText(raw: String): String? {
        val text = raw.trim()
        return runCatching {
            when {
                text.startsWith("{") -> JSONObject(text)
                text.startsWith("[") -> JSONArray(text)
                else -> error("必须以 { 或 [ 开头")
            }
            null
        }.getOrElse { e -> e.message ?: "不是合法JSON" }
    }

    fun verifyLoginByPassword(
        account: String,
        password: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val accountTrimmed = account.trim()
            if (accountTrimmed.isBlank() || password.isBlank()) {
                _authCheckMessage.value = "请输入账号和密码"
                onResult(false)
                return@launch
            }
            _authCheckMessage.value = "校验中..."
            userPrefsRepository.saveLoginValidationTemp(accountTrimmed, password)

            val tokenResult = withContext(Dispatchers.IO) { directPasswordLoginApi.loginAndGetToken(accountTrimmed, password) }
            if (tokenResult.isFailure) {
                userPrefsRepository.markLoginState(false)
                _authCheckMessage.value = "校验失败：${tokenResult.exceptionOrNull()?.message ?: "登录失败"}"
                onResult(false)
                return@launch
            }

            val token = tokenResult.getOrNull().orEmpty()
            val check = withContext(Dispatchers.IO) { authCheckApi.checkToken(token) }
            if (check.isFailure) {
                userPrefsRepository.markLoginState(false)
                val reason = check.exceptionOrNull()?.message ?: "登录态校验失败"
                _authCheckMessage.value = "校验失败：$reason"
                onResult(false)
                return@launch
            }

            userPrefsRepository.saveUserToken(token)
            userPrefsRepository.markLoginState(true)
            userPrefsRepository.saveLoginForm(accountTrimmed, password)
            userPrefsRepository.clearLoginValidationTemp()
            _loginAccount.value = accountTrimmed
            _loginPassword.value = password
            _authCheckMessage.value = "校验成功"
            onResult(true)
        }
    }

    fun onLoginValidationFailed(reason: String = "校验失败，请重新输入账号密码") {
        viewModelScope.launch {
            userPrefsRepository.clearLoginState()
            _authCheckMessage.value = reason
        }
    }

    fun start(config: BookingConfig) {
        viewModelScope.launch {
            successCleanupDone = false
            userPrefsRepository.saveConfig(config)
            userPrefsRepository.saveShouldResumeTask(_autoResumeEnabled.value)
            scheduler.start(config)
            resumeConsumed = true
        }
    }

    fun submitTask(config: BookingConfig) {
        viewModelScope.launch {
            runCatching {
                successCleanupDone = false
                // 先置位，避免 shouldResumeTask 的流回调抢先触发重复 start。
                resumeConsumed = true
                userPrefsRepository.saveConfig(config)
                userPrefsRepository.saveShouldResumeTask(true)
                val encryptedPassword = userPrefsRepository.getSavedPasswordEncrypted()
                scheduledTaskStore.save(
                    ScheduledTaskRecord(
                        loginAccount = _loginAccount.value.trim(),
                        passwordEncrypted = encryptedPassword,
                        campusName = config.campusName,
                        venueName = config.venueName,
                        courtNumber = config.courtNumber,
                        timeSlots = config.timeSlots,
                        targetDayOffset = config.targetDayOffset,
                        createdAtMs = System.currentTimeMillis(),
                    ),
                )
                _scheduledTask.value = scheduledTaskStore.load()
                val alarmExact = bookingAlarmScheduler.schedule(config)
                userPrefsRepository.saveAlarmScheduled(true)
                scheduler.start(config)
                _authCheckMessage.value =
                    if (alarmExact) {
                        "提交成功，已生成本地预约任务"
                    } else {
                        "提交成功，已生成任务（系统未授予精确定时，可能有秒级偏差）"
                    }
            }.onFailure { e ->
                _authCheckMessage.value = "提交失败：${e.message ?: "未知错误"}"
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            userPrefsRepository.saveShouldResumeTask(false)
            bookingAlarmScheduler.cancel()
            userPrefsRepository.saveAlarmScheduled(false)
            scheduler.stop()
        }
    }

    fun setAutoResumeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.saveAutoResumeEnabled(enabled)
            if (!enabled) {
                userPrefsRepository.saveShouldResumeTask(false)
            }
        }
    }

    fun stopBookingManually() {
        viewModelScope.launch {
            scheduler.stopWithReason("手动取消预约")
            bookingAlarmScheduler.cancel()
            scheduledTaskStore.delete()
            userPrefsRepository.saveAlarmScheduled(false)
            _scheduledTask.value = null
            userPrefsRepository.clearConfig()
            _authCheckMessage.value = "已停止预约"
        }
    }

    fun finishResultAndBackLogin() {
        viewModelScope.launch {
            scheduledTaskStore.delete()
            userPrefsRepository.saveAlarmScheduled(false)
            userPrefsRepository.saveShouldResumeTask(false)
            userPrefsRepository.clearConfig()
            userPrefsRepository.clearLoginState()
            _scheduledTask.value = null
            _authCheckMessage.value = ""
        }
    }

    private fun tryAutoResume() {
        if (resumeConsumed) return
        if (!_hasLoginState.value) return
        if (!_autoResumeEnabled.value) return
        if (!_shouldResumeTask.value) return
        val cfg = _latestConfig.value ?: return
        if (uiState.value.running) return
        scheduler.start(cfg)
        resumeConsumed = true
    }

    private fun refreshLoginReadyState() {
        _hasLoginState.value = hasLoginStateRaw && _userToken.value.trim().isNotEmpty()
    }

    private suspend fun clearScheduledTaskAfterSuccess() {
        bookingAlarmScheduler.cancel()
        scheduledTaskStore.delete()
        userPrefsRepository.saveAlarmScheduled(false)
        userPrefsRepository.clearConfig()
        _scheduledTask.value = null
        _alarmScheduled.value = false
    }

    companion object {
        fun factory(
            userPrefsRepository: UserPrefsRepository,
            scheduledTaskStore: ScheduledTaskStore,
            bookingAlarmScheduler: BookingAlarmScheduler,
            manualApiConfigStore: ManualApiConfigStore,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = BookingViewModel(userPrefsRepository, scheduledTaskStore, bookingAlarmScheduler, manualApiConfigStore) as T
            }
    }
}
