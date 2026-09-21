package com.steplock.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.steplock.app.data.AuthState
import com.steplock.app.data.BlockedAppCatalog
import com.steplock.app.data.DailyStat
import com.steplock.app.data.LockSettings
import com.steplock.app.data.Pomodoro
import com.steplock.app.data.SettingsRepository
import com.steplock.app.data.SleepRepository
import com.steplock.app.data.StepTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StepLockUiState(
    val settings: LockSettings,
    val today: DailyStat,
    /** 오늘까지 7일, 기록이 없는 날은 0으로 채웁니다. */
    val weekly: List<DailyStat>,
    val onboardingCompleted: Boolean,
    val authState: AuthState,
)

data class PomodoroUiState(
    val remainingMs: Long,
    val progress: Float,
    val running: Boolean,
    val paused: Boolean,
    val sessionsToday: Int,
    val goal: Int,
)

class StepLockViewModel(
    private val repository: SettingsRepository,
    stepTracker: StepTracker,
    private val sleepRepository: SleepRepository,
) : ViewModel() {

    val apps = BlockedAppCatalog.apps

    val sleepReadPermission: String get() = sleepRepository.readPermission

    /** 설정이 DataStore에서 올라오기 전에는 null입니다. */
    val uiState: StateFlow<StepLockUiState?> =
        combine(repository.preferences, stepTracker.todaySteps()) { prefs, steps ->
            val today = DailyStat(
                deviceUuid = prefs.settings.deviceUuid,
                accountId = prefs.settings.accountId,
                date = LocalDate.now(),
                steps = steps,
                sleepMinutes = prefs.sleepMinutesToday,
                pomodoroSessions = prefs.pomodoro.sessionsToday,
            )
            StepLockUiState(
                settings = prefs.settings,
                today = today,
                weekly = lastSevenDays(prefs.history, today),
                onboardingCompleted = prefs.onboardingCompleted,
                authState = prefs.authState,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val secondTicker = flow {
        while (true) {
            emit(Unit)
            delay(1_000)
        }
    }

    val pomodoro: StateFlow<PomodoroUiState?> =
        combine(repository.preferences, secondTicker) { prefs, _ ->
            val state = prefs.pomodoro
            val remaining = when {
                state.endsAt != null -> (state.endsAt - System.currentTimeMillis()).coerceAtLeast(0L)
                state.pausedRemainingMs != null -> state.pausedRemainingMs
                else -> Pomodoro.SESSION_MS
            }
            PomodoroUiState(
                remainingMs = remaining,
                progress = 1f - (remaining.toFloat() / Pomodoro.SESSION_MS).coerceIn(0f, 1f),
                running = state.isRunning,
                paused = state.isPaused,
                sessionsToday = state.sessionsToday,
                goal = prefs.settings.pomodoroGoal,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { repository.ensureDeviceUuid() }

        // 서비스가 죽은 채로 시간이 지난 세션도 앱을 열면 집계됩니다.
        viewModelScope.launch {
            combine(repository.preferences, secondTicker) { prefs, _ -> prefs.pomodoro.endsAt }
                .collect { endsAt ->
                    if (endsAt != null && endsAt <= System.currentTimeMillis()) {
                        repository.completePomodoroSession()
                    }
                }
        }
    }

    fun startPomodoro() {
        viewModelScope.launch { repository.startPomodoro() }
    }

    fun pausePomodoro() {
        viewModelScope.launch { repository.pausePomodoro() }
    }

    fun resumePomodoro() {
        viewModelScope.launch { repository.resumePomodoro() }
    }

    fun resetPomodoro() {
        viewModelScope.launch { repository.resetPomodoro() }
    }

    fun setStepsEnabled(enabled: Boolean) = edit { it.copy(stepsEnabled = enabled) }

    fun setSleepEnabled(enabled: Boolean) = edit { it.copy(sleepEnabled = enabled) }

    fun isHealthConnectAvailable(): Boolean = sleepRepository.isAvailable()

    fun refreshSleep() {
        viewModelScope.launch { sleepRepository.refresh() }
    }

    /** 권한이 이미 있으면 바로 켜고, 없으면 화면이 권한을 요청하도록 알립니다. */
    fun enableSleepIfPermitted(onNeedsPermission: () -> Unit) {
        viewModelScope.launch {
            if (sleepRepository.hasPermission()) {
                enableSleepAndRefresh()
            } else {
                onNeedsPermission()
            }
        }
    }

    fun onSleepPermissionGranted() {
        viewModelScope.launch {
            if (sleepRepository.hasPermission()) enableSleepAndRefresh()
        }
    }

    private suspend fun enableSleepAndRefresh() {
        repository.updateSettings { it.copy(sleepEnabled = true) }
        sleepRepository.refresh()
    }

    fun setPomodoroEnabled(enabled: Boolean) = edit { it.copy(pomodoroEnabled = enabled) }

    fun setRequireAllConditions(enabled: Boolean) = edit { it.copy(requireAllConditions = enabled) }

    fun changeStepGoal(delta: Int) = edit {
        it.copy(stepGoal = (it.stepGoal + delta).coerceIn(1000, 20000))
    }

    fun changeSleepGoal(delta: Float) = edit {
        it.copy(sleepGoalHours = (it.sleepGoalHours + delta).coerceIn(4f, 12f))
    }

    fun changePomodoroGoal(delta: Int) = edit {
        it.copy(pomodoroGoal = (it.pomodoroGoal + delta).coerceIn(1, 8))
    }

    fun toggleBlockedApp(appId: String) = edit {
        val blocked = it.blockedAppIds
        it.copy(blockedAppIds = if (appId in blocked) blocked - appId else blocked + appId)
    }

    fun continueAsGuest() {
        viewModelScope.launch { repository.setGuest() }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.setOnboardingCompleted(true) }
    }

    /** 통계에 쓰이도록 오늘 값을 이력에 적어 둡니다. 값이 같으면 쓰지 않습니다. */
    fun recordToday() {
        viewModelScope.launch {
            uiState.value?.let { repository.recordDay(it.today) }
        }
    }

    private fun edit(transform: (LockSettings) -> LockSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    private fun lastSevenDays(history: List<DailyStat>, today: DailyStat): List<DailyStat> {
        val byDate = history.associateBy { it.date } + (today.date to today)
        return (6L downTo 0L).map { offset ->
            val date = today.date.minusDays(offset)
            byDate[date] ?: DailyStat(
                deviceUuid = today.deviceUuid,
                accountId = today.accountId,
                date = date,
                steps = 0,
                sleepMinutes = 0,
                pomodoroSessions = 0,
            )
        }
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val app = context.applicationContext
                val repository = SettingsRepository(app)
                StepLockViewModel(
                    repository = repository,
                    stepTracker = StepTracker(app, repository),
                    sleepRepository = SleepRepository(app, repository),
                )
            }
        }
    }
}
