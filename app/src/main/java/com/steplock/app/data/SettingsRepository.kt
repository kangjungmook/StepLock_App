package com.steplock.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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

private const val HISTORY_DAYS = 30

private fun encodeDay(stat: DailyStat): String =
    "${stat.date}|${stat.steps}|${stat.sleepMinutes}|${stat.pomodoroSessions}"

private fun decodeDay(raw: String, deviceUuid: String, accountId: String?): DailyStat? {
    val parts = raw.split('|')
    if (parts.size != 4) return null
    return runCatching {
        DailyStat(
            deviceUuid = deviceUuid,
            accountId = accountId,
            date = LocalDate.parse(parts[0]),
            steps = parts[1].toInt(),
            sleepMinutes = parts[2].toInt(),
            pomodoroSessions = parts[3].toInt(),
        )
    }.getOrNull()
}

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.stepLockStore

    private object Keys {
        val deviceUuid = stringPreferencesKey("device_uuid")
        val accountId = stringPreferencesKey("account_id")
        val displayName = stringPreferencesKey("display_name")
        val accountEmail = stringPreferencesKey("account_email")
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
        val pomodoroEndsAt = longPreferencesKey("pomodoro_ends_at")
        val pomodoroPausedRemaining = longPreferencesKey("pomodoro_paused_remaining")
        val pomodoroSessionsDate = stringPreferencesKey("pomodoro_sessions_date")
        val pomodoroSessionsCount = intPreferencesKey("pomodoro_sessions_count")
        val sleepMinutesDate = stringPreferencesKey("sleep_minutes_date")
        val sleepMinutes = intPreferencesKey("sleep_minutes")
        val dailyHistory = stringSetPreferencesKey("daily_history")
        val settingsUpdatedAt = longPreferencesKey("settings_updated_at")
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
            prefs.writeSettings(transform(prefs.toSettings()))
            prefs[Keys.settingsUpdatedAt] = System.currentTimeMillis()
        }
    }

    /** 서버 값이 더 최신일 때 통째로 덮어씁니다. 서버의 시각을 그대로 보관합니다. */
    suspend fun applyRemoteSettings(settings: LockSettings, updatedAt: Long) {
        store.edit { prefs ->
            prefs.writeSettings(settings)
            prefs[Keys.settingsUpdatedAt] = updatedAt
        }
    }

    /** 원격에만 있던 날짜를 채웁니다. 로컬에 있는 날짜는 건드리지 않습니다. */
    suspend fun mergeHistory(rows: List<DailyStat>) {
        store.edit { prefs ->
            val existing = prefs[Keys.dailyHistory].orEmpty()
            val existingDates = existing.map { it.substringBefore('|') }.toSet()
            val added = rows
                .filter { it.date.toString() !in existingDates }
                .map { encodeDay(it) }
            if (added.isEmpty()) return@edit
            prefs[Keys.dailyHistory] = (existing + added)
                .sortedByDescending { it.substringBefore('|') }
                .take(HISTORY_DAYS)
                .toSet()
        }
    }

    private fun MutablePreferences.writeSettings(next: LockSettings) {
        this[Keys.stepGoal] = next.stepGoal
        this[Keys.sleepGoalHours] = next.sleepGoalHours
        this[Keys.pomodoroGoal] = next.pomodoroGoal
        this[Keys.stepsEnabled] = next.stepsEnabled
        this[Keys.sleepEnabled] = next.sleepEnabled
        this[Keys.pomodoroEnabled] = next.pomodoroEnabled
        this[Keys.requireAll] = next.requireAllConditions
        this[Keys.blockedAppIds] = next.blockedAppIds
        next.accountId?.let { this[Keys.accountId] = it }
        next.displayName?.let { this[Keys.displayName] = it }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        store.edit { it[Keys.onboardingCompleted] = completed }
    }

    suspend fun setGuest() {
        store.edit { it[Keys.guest] = true }
    }

    /** 로그인 성공 — 이 기기의 로컬 레코드를 계정에 귀속시킵니다. */
    suspend fun setAccount(accountId: String, email: String?) {
        store.edit { prefs ->
            prefs[Keys.accountId] = accountId
            if (email != null) {
                prefs[Keys.accountEmail] = email
                prefs[Keys.displayName] = email.substringBefore('@')
            }
            prefs.remove(Keys.guest)
        }
    }

    suspend fun clearAccount() {
        store.edit { prefs ->
            prefs.remove(Keys.accountId)
            prefs.remove(Keys.accountEmail)
            prefs.remove(Keys.displayName)
        }
    }

    /**
     * 계정 삭제 뒤 기기에 남은 것까지 지웁니다. 설정·기록·걸음 기준점·집중 세션·
     * 수면 캐시가 전부 사라지고 기기 UUID도 새로 발급됩니다.
     * 온보딩 완료 표시만 남겨 권한 안내를 처음부터 다시 받지 않게 합니다.
     */
    suspend fun clearAllLocalData() {
        store.edit { prefs ->
            val onboarded = prefs[Keys.onboardingCompleted] ?: false
            prefs.clear()
            prefs[Keys.onboardingCompleted] = onboarded
            prefs[Keys.deviceUuid] = UUID.randomUUID().toString()
            prefs[Keys.guest] = true
        }
    }

    suspend fun startPomodoro(durationMs: Long = Pomodoro.SESSION_MS) {
        store.edit { prefs ->
            prefs[Keys.pomodoroEndsAt] = System.currentTimeMillis() + durationMs
            prefs.remove(Keys.pomodoroPausedRemaining)
        }
    }

    suspend fun pausePomodoro() {
        store.edit { prefs ->
            val endsAt = prefs[Keys.pomodoroEndsAt] ?: return@edit
            val remaining = (endsAt - System.currentTimeMillis()).coerceAtLeast(0L)
            prefs[Keys.pomodoroPausedRemaining] = remaining
            prefs.remove(Keys.pomodoroEndsAt)
        }
    }

    suspend fun resumePomodoro() {
        store.edit { prefs ->
            val remaining = prefs[Keys.pomodoroPausedRemaining] ?: return@edit
            prefs[Keys.pomodoroEndsAt] = System.currentTimeMillis() + remaining
            prefs.remove(Keys.pomodoroPausedRemaining)
        }
    }

    suspend fun resetPomodoro() {
        store.edit { prefs ->
            prefs.remove(Keys.pomodoroEndsAt)
            prefs.remove(Keys.pomodoroPausedRemaining)
        }
    }

    /** 진행 중이던 세션만 한 번 집계합니다 — 화면과 서비스가 동시에 불러도 중복되지 않게. */
    suspend fun completePomodoroSession() {
        store.edit { prefs ->
            if (prefs[Keys.pomodoroEndsAt] == null) return@edit
            val today = LocalDate.now().toString()
            val sameDay = prefs[Keys.pomodoroSessionsDate] == today
            prefs[Keys.pomodoroSessionsDate] = today
            prefs[Keys.pomodoroSessionsCount] = if (sameDay) {
                (prefs[Keys.pomodoroSessionsCount] ?: 0) + 1
            } else {
                1
            }
            prefs.remove(Keys.pomodoroEndsAt)
            prefs.remove(Keys.pomodoroPausedRemaining)
        }
    }

    /**
     * 하루 한 줄씩 `날짜|걸음|수면분|세션` 형식으로 보관합니다.
     * 통계가 주·월 단위를 넘어가면 Room으로 옮기는 게 맞습니다.
     */
    suspend fun recordDay(stat: DailyStat) {
        store.edit { prefs ->
            val encoded = encodeDay(stat)
            val existing = prefs[Keys.dailyHistory].orEmpty()
            val sameDay = existing.firstOrNull { it.startsWith("${stat.date}|") }
            if (sameDay == encoded) return@edit
            prefs[Keys.dailyHistory] = (existing - setOfNotNull(sameDay) + encoded)
                .sortedByDescending { it.substringBefore('|') }
                .take(HISTORY_DAYS)
                .toSet()
        }
    }

    suspend fun writeSleepMinutes(date: LocalDate, minutes: Int) {
        store.edit { prefs ->
            prefs[Keys.sleepMinutesDate] = date.toString()
            prefs[Keys.sleepMinutes] = minutes
        }
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
            accountEmail = this[Keys.accountEmail],
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
            sleepMinutesToday = if (this[Keys.sleepMinutesDate] == LocalDate.now().toString()) {
                this[Keys.sleepMinutes] ?: 0
            } else {
                0
            },
            settingsUpdatedAt = this[Keys.settingsUpdatedAt] ?: 0L,
            history = this[Keys.dailyHistory].orEmpty()
                .mapNotNull { decodeDay(it, settings.deviceUuid, accountId) }
                .sortedBy { it.date },
            pomodoro = PomodoroState(
                endsAt = this[Keys.pomodoroEndsAt],
                pausedRemainingMs = this[Keys.pomodoroPausedRemaining],
                sessionsToday = if (this[Keys.pomodoroSessionsDate] == LocalDate.now().toString()) {
                    this[Keys.pomodoroSessionsCount] ?: 0
                } else {
                    0
                },
            ),
        )
    }
}
