package com.example.ding_badminton.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ding_badminton.R
import com.example.ding_badminton.data.debug.LoginDebugFileLogger
import com.example.ding_badminton.data.local.ManualApiConfigStore
import com.example.ding_badminton.data.local.ScheduledTaskStore
import com.example.ding_badminton.data.local.UserPrefsRepository
import com.example.ding_badminton.domain.model.BookingConfig
import com.example.ding_badminton.domain.model.BookingPhase
import com.example.ding_badminton.domain.usecase.BookingAlarmScheduler
import com.example.ding_badminton.domain.usecase.BookingTimeCalculator
import com.example.ding_badminton.ui.model.BookingDraft
import com.example.ding_badminton.ui.screens.CampusStepScreen
import com.example.ding_badminton.ui.screens.CountdownScreen
import com.example.ding_badminton.ui.screens.DateStepScreen
import com.example.ding_badminton.ui.screens.LoginScreen
import com.example.ding_badminton.ui.screens.ResultScreen
import com.example.ding_badminton.ui.screens.SnowfallOverlay
import com.example.ding_badminton.ui.screens.SlotStepScreen

@Composable
fun AppRoot(autoStartBooking: Boolean = false) {
    val appContext = LocalContext.current.applicationContext
    val vm: BookingViewModel =
        viewModel(
            factory =
                BookingViewModel.factory(
                    UserPrefsRepository(appContext),
                    ScheduledTaskStore(appContext),
                    BookingAlarmScheduler(appContext),
                    ManualApiConfigStore(appContext),
                ),
        )
    val navController = rememberNavController()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val hasLoginState by vm.hasLoginState.collectAsStateWithLifecycle()
    val latestConfig by vm.latestConfig.collectAsStateWithLifecycle()
    val userToken by vm.userToken.collectAsStateWithLifecycle()
    val authCheckMessage by vm.authCheckMessage.collectAsStateWithLifecycle()
    val manualApiConfig by vm.manualApiConfig.collectAsStateWithLifecycle()
    val scheduledTask by vm.scheduledTask.collectAsStateWithLifecycle()
    val alarmScheduled by vm.alarmScheduled.collectAsStateWithLifecycle()
    val timeCalculator = remember { BookingTimeCalculator() }
    var draft by remember {
        mutableStateOf(
            BookingDraft(
                campusName = latestConfig?.campusName.orEmpty(),
                targetDayOffset = latestConfig?.targetDayOffset ?: 2,
                courtNumberText = (latestConfig?.courtNumber ?: 1).toString(),
                selectedSlots = latestConfig?.timeSlots ?: emptyList(),
            ),
        )
    }
    var autoStartConsumed by remember { mutableStateOf(false) }
    var addBookingMode by remember { mutableStateOf(false) }
    var immediateBookingMessage by remember { mutableStateOf("") }
    val isImmediateByOffset: (Int) -> Boolean = { dayOffset ->
        val probeConfig =
            BookingConfig(
                campusName = draft.campusName.ifBlank { "屏峰校区" },
                venueName = "probe",
                courtNumber = 1,
                timeSlots = listOf("08:00-09:00"),
                targetDayOffset = dayOffset,
            )
        val plan = timeCalculator.buildPlan(probeConfig)
        timeCalculator.secondsUntilOpen(plan.openDateTime) <= 0
    }
    val isImmediateConfig: (BookingConfig) -> Boolean = { cfg ->
        val plan = timeCalculator.buildPlan(cfg)
        timeCalculator.secondsUntilOpen(plan.openDateTime) <= 0
    }

    LaunchedEffect(latestConfig) {
        val cfg = latestConfig ?: return@LaunchedEffect
        if (draft.campusName.isBlank()) {
            draft =
                draft.copy(
                    campusName = cfg.campusName,
                    targetDayOffset = cfg.targetDayOffset,
                    courtNumberText = cfg.courtNumber.toString(),
                    selectedSlots = cfg.timeSlots,
                )
        }
    }
    LaunchedEffect(scheduledTask, hasLoginState) {
        val route = navController.currentDestination?.route
        val shouldJumpToCountdown =
            route == AppRoutes.LOGIN ||
                route == AppRoutes.CONFIG_CAMPUS ||
                route == AppRoutes.CONFIG_DATE ||
                route == AppRoutes.CONFIG_SLOT
        if (scheduledTask != null && shouldJumpToCountdown) {
            navController.navigate(AppRoutes.COUNTDOWN)
        }
    }
    LaunchedEffect(autoStartBooking, autoStartConsumed, hasLoginState, latestConfig) {
        if (!autoStartBooking || autoStartConsumed) return@LaunchedEffect
        val cfg = latestConfig ?: return@LaunchedEffect
        if (!hasLoginState) return@LaunchedEffect
        autoStartConsumed = true
        vm.start(cfg)
        navController.navigate(AppRoutes.COUNTDOWN)
    }
    LaunchedEffect(scheduledTask, hasLoginState, latestConfig, state.running, state.phase) {
        if (scheduledTask != null && hasLoginState && !state.running && state.phase == BookingPhase.IDLE) {
            val cfg = latestConfig ?: return@LaunchedEffect
            vm.start(cfg)
        }
    }
    LaunchedEffect(state.phase) {
        if (state.phase == BookingPhase.SUCCESS || state.phase == BookingPhase.BLOCKED || state.phase == BookingPhase.STOPPED) {
            navController.navigate(AppRoutes.RESULT)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.phone),
            contentDescription = "wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.86f,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.16f)),
        )
        SnowfallOverlay()
        NavHost(navController = navController, startDestination = AppRoutes.LOGIN, modifier = Modifier.fillMaxSize()) {
            composable(AppRoutes.LOGIN) {
                LoginScreen(
                    hasLoginState = hasLoginState,
                    authCheckMessage = authCheckMessage,
                    initialUserToken = manualApiConfig?.userToken.orEmpty(),
                    initialVenueParamsRaw = manualApiConfig?.venueParamsRaw.orEmpty(),
                    initialSubmitParamsRaw = manualApiConfig?.submitParamsRaw.orEmpty(),
                    onVerifyAndSave = { token, venueRaw, submitRaw ->
                        LoginDebugFileLogger.clear(appContext)
                        LoginDebugFileLogger.append(
                            appContext,
                            "开始HTTP校验: token_length=${token.length}",
                        )
                        vm.saveManualApiConfig(token, venueRaw, submitRaw) { success ->
                            if (success) {
                                navController.navigate(AppRoutes.CONFIG_CAMPUS) {
                                    popUpTo(AppRoutes.LOGIN) { inclusive = false }
                                }
                            }
                        }
                    },
                )
            }
            composable(AppRoutes.CONFIG_CAMPUS) {
                CampusStepScreen(
                    policies = vm.policies,
                    selectedCampus = draft.campusName,
                    onSelectCampus = { campus ->
                        draft = draft.copy(campusName = campus, selectedSlots = emptyList())
                    },
                    onNext = { navController.navigate(AppRoutes.CONFIG_DATE) },
                    onViewMyBooking = { navController.navigate(AppRoutes.COUNTDOWN) },
                    hasPendingTask = scheduledTask != null,
                )
            }
            composable(AppRoutes.CONFIG_DATE) {
                DateStepScreen(
                    selectedDayOffset = draft.targetDayOffset,
                    onSelectDayOffset = { draft = draft.copy(targetDayOffset = it) },
                    onBack = { navController.popBackStack() },
                    onNext = { navController.navigate(AppRoutes.CONFIG_SLOT) },
                    immediateOnly = addBookingMode || scheduledTask != null,
                    isImmediateDate = isImmediateByOffset,
                )
            }
            composable(AppRoutes.CONFIG_SLOT) {
                val policy = vm.policies[draft.campusName]
                SlotStepScreen(
                    selectedCampus = draft.campusName,
                    policy = policy,
                    selectedCourtNumber = draft.courtNumberText.toIntOrNull(),
                    selectedSlots = draft.selectedSlots,
                    targetDayOffset = draft.targetDayOffset,
                    onSelectCell = { court, slot ->
                        val currentCourt = draft.courtNumberText.toIntOrNull()
                        val currentSlots = draft.selectedSlots.toMutableList()
                        val nextSlots =
                            if (currentCourt != court) {
                                mutableListOf(slot)
                            } else {
                                if (currentSlots.contains(slot)) {
                                    currentSlots.remove(slot)
                                } else if (currentSlots.size < 2) {
                                    currentSlots.add(slot)
                                }
                                currentSlots
                            }
                        draft = draft.copy(courtNumberText = court.toString(), selectedSlots = nextSlots)
                    },
                    onBack = { navController.popBackStack() },
                    onSubmit = { config: BookingConfig ->
                        val immediate = isImmediateConfig(config)
                        if (scheduledTask != null) {
                            if (!immediate) {
                                immediateBookingMessage = "您已经有一个计时预约了，不可再次选中"
                                navController.navigate(AppRoutes.CONFIG_DATE)
                            } else {
                                vm.submitImmediateBooking(config) { _, msg ->
                                    immediateBookingMessage = msg
                                }
                                addBookingMode = false
                                navController.navigate(AppRoutes.COUNTDOWN)
                            }
                        } else if (addBookingMode) {
                            if (!immediate) {
                                immediateBookingMessage = "您已经有一个计时预约了，不可再次选中"
                                navController.navigate(AppRoutes.CONFIG_DATE)
                            } else {
                                vm.submitImmediateBooking(config) { _, msg ->
                                    immediateBookingMessage = msg
                                }
                                addBookingMode = false
                                navController.navigate(AppRoutes.COUNTDOWN)
                            }
                        } else {
                            vm.submitTask(config)
                            navController.navigate(AppRoutes.COUNTDOWN)
                        }
                    },
                )
            }
            composable(AppRoutes.COUNTDOWN) {
                CountdownScreen(
                    config = state.config ?: latestConfig,
                    openTimeText = state.openTimeStr,
                    targetDateText = state.targetDateStr,
                    retryInSec = state.nextAttemptInSec,
                    openInSec = state.openInSec,
                    attempts = state.attempts,
                    maxAttempts = 10,
                    lastReason = state.lastReason,
                    hasLoginState = hasLoginState,
                    hasTaskFile = scheduledTask != null,
                    alarmScheduled = alarmScheduled,
                    onStopBooking = {
                        vm.stopBookingManually()
                        navController.navigate(AppRoutes.RESULT)
                    },
                    onAddBooking = {
                        addBookingMode = true
                        navController.navigate(AppRoutes.CONFIG_CAMPUS)
                    },
                )
            }
            composable(AppRoutes.RESULT) {
                ResultScreen(
                    state = state,
                    onConfirmAndBackLogin = {
                        vm.finishResultAndBackLogin()
                        navController.navigate(AppRoutes.LOGIN) {
                            popUpTo(AppRoutes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
        }
        if (immediateBookingMessage.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { immediateBookingMessage = "" },
                title = { Text("提示") },
                text = { Text(immediateBookingMessage) },
                confirmButton = {
                    Button(onClick = { immediateBookingMessage = "" }) { Text("确认") }
                },
            )
        }
    }
}
