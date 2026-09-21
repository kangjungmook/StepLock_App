package com.steplock.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

private val Context.stepLockStore: DataStore<Preferences> by preferencesDataStore(name = "steplock")

/** 걸음 센서는 부팅 이후 누적값을 주므로, 날짜별 기준점을 따로 보관합니다. */
data class StepBaseline(val date: LocalDate, val counter: Long)

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.stepLockStore

    private object Keys {
        val deviceUuid = stringPreferencesKey("device_uuid")
        val accountId = stringPreferencesKey("account_id")
        val displayName = stringPreferencesKey("display_name")
        val guest = booleanPreferencesKey("guest")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val stepGoal = intPreferencesKey("step_goal")
        val sleepGoalHours = floatPreferencesKey("sleep_goal_hours")
        val pomodoroGoal = intPreferencesKey("pomodoro_goal")
        val stepsEnabled = booleanPreferencesKey("steps_enabled")
        val sleepEnabled = booleanPreferencesKey("sleep_enabled")
        val pomodoroEnabled = booleanPreferencesKey("pomodoro_enabled")
        val requireAll = booleanPreferencesKey("require_all_conditions")
        val blockedAppIds = stringSetPreferencesKey("blocked_app_ids")
        val stepBaselineDate = stringPreferencesKey("step_baseline_date")
        val stepBaselineCounter = longPreferencesKey("step_baseline_counter")
    }

    val preferences: Flow<AppPreferences> = store.data.map { it.toAppPreferences() }

    /** 첫 실행 시 기기 UUID를 만들어 모든 로컬 레코드에 부여합니다. */
    suspend fun ensureDeviceUuid(): String = store.edit { prefs ->
        if (prefs[Keys.deviceUuid].isNullOrBlank()) {
            prefs[Keys.deviceUuid] = UUID.randomUUID().toString()
        }
    }[Keys.deviceUuid].orEmpty()

    suspend fun updateSettings(transform: (LockSettings) -> LockSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.stepGoal] = next.stepGoal
            prefs[Keys.sleepGoalHours] = next.sleepGoalHours
            prefs[Keys.pomodoroGoal] = next.pomodoroGoal
            prefs[Keys.stepsEnabled] = next.stepsEnabled
            prefs[Keys.sleepEnabled] = next.sleepEnabled
            prefs[Keys.pomodoroEnabled] = next.pomodoroEnabled
            prefs[Keys.requireAll] = next.requireAllConditions
            prefs[Keys.blockedAppIds] = next.blockedAppIds
            next.accountId?.let { prefs[Keys.accountId] = it }
            next.displayName?.let { prefs[Keys.displayName] = it }
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        store.edit { it[Keys.onboardingCompleted] = completed }
    }

    suspend fun setGuest() {
        store.edit { it[Keys.guest] = true }
    }

    suspend fun readStepBaseline(): StepBaseline? {
        val prefs = store.data.first()
        val date = prefs[Keys.stepBaselineDate] ?: return null
        val counter = prefs[Keys.stepBaselineCounter] ?: return null
        return StepBaseline(LocalDate.parse(date), counter)
    }

    suspend fun writeStepBaseline(baseline: StepBaseline) {
        store.edit { prefs ->
            prefs[Keys.stepBaselineDate] = baseline.date.toString()
            prefs[Keys.stepBaselineCounter] = baseline.counter
        }
    }

    private fun Preferences.toSettings(): LockSettings {
        val defaults = LockSettings(deviceUuid = this[Keys.deviceUuid].orEmpty())
        return defaults.copy(
            accountId = this[Keys.accountId],
            displayName = this[Keys.displayName],
            stepGoal = this[Keys.stepGoal] ?: defaults.stepGoal,
            sleepGoalHours = this[Keys.sleepGoalHours] ?: defaults.sleepGoalHours,
            pomodoroGoal = this[Keys.pomodoroGoal] ?: defaults.pomodoroGoal,
            stepsEnabled = this[Keys.stepsEnabled] ?: defaults.stepsEnabled,
            sleepEnabled = this[Keys.sleepEnabled] ?: defaults.sleepEnabled,
            pomodoroEnabled = this[Keys.pomodoroEnabled] ?: defaults.pomodoroEnabled,
            requireAllConditions = this[Keys.requireAll] ?: defaults.requireAllConditions,
            blockedAppIds = this[Keys.blockedAppIds] ?: defaults.blockedAppIds,
        )
    }

    private fun Preferences.toAppPreferences(): AppPreferences {
        val settings = toSettings()
        val accountId = this[Keys.accountId]
        return AppPreferences(
            settings = settings,
            onboardingCompleted = this[Keys.onboardingCompleted] ?: false,
            authState = when {
                accountId != null -> AuthState.SignedIn(accountId)
                this[Keys.guest] == true -> AuthState.Guest
                else -> AuthState.Unknown
            },
        )
    }
}
