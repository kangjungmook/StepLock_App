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
import com.steplock.app.data.SettingsRepository
import com.steplock.app.data.StepTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StepLockUiState(
    val settings: LockSettings,
    val today: DailyStat,
    val onboardingCompleted: Boolean,
    val authState: AuthState,
)

class StepLockViewModel(
    private val repository: SettingsRepository,
    stepTracker: StepTracker,
) : ViewModel() {

    val apps = BlockedAppCatalog.apps

    /** 설정이 DataStore에서 올라오기 전에는 null입니다. */
    val uiState: StateFlow<StepLockUiState?> =
        combine(repository.preferences, stepTracker.todaySteps()) { prefs, steps ->
            StepLockUiState(
                settings = prefs.settings,
                today = DailyStat(
                    deviceUuid = prefs.settings.deviceUuid,
                    accountId = prefs.settings.accountId,
                    date = LocalDate.now(),
                    steps = steps,
                    sleepMinutes = 0,
                    pomodoroSessions = 0,
                ),
                onboardingCompleted = prefs.onboardingCompleted,
                authState = prefs.authState,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { repository.ensureDeviceUuid() }
    }

    fun setStepsEnabled(enabled: Boolean) = edit { it.copy(stepsEnabled = enabled) }

    fun setSleepEnabled(enabled: Boolean) = edit { it.copy(sleepEnabled = enabled) }

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

    private fun edit(transform: (LockSettings) -> LockSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val app = context.applicationContext
                val repository = SettingsRepository(app)
                StepLockViewModel(repository, StepTracker(app, repository))
            }
        }
    }
}
