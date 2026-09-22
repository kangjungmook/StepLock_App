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

/** 기기에 보관하는 일별 기록의 길이. 통계 화면이 고를 수 있는 최대 기간이기도 합니다. */
const val HISTORY_DAYS = 30

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

/**
 * 잠금 조건 한 벌의 키 묶음.
 *
 * 같은 구조를 두 벌 보관합니다 — 접두사 없는 쪽이 **적용 중**, `desired_` 쪽이
 * **사용자가 정해 둔 값**입니다. 완화를 기다리는 동안 둘이 달라집니다.
 * 접두사 없는 이름을 그대로 둔 덕에 기존 설치의 값이 그대로 읽힙니다.
 */
private class ConditionKeys(prefix: String) {
    val stepGoal = intPreferencesKey("${prefix}step_goal")
    val sleepGoalHours = floatPreferencesKey("${prefix}sleep_goal_hours")
    val pomodoroGoal = intPreferencesKey("${prefix}pomodoro_goal")
    val stepsEnabled = booleanPreferencesKey("${prefix}steps_enabled")
    val sleepEnabled = booleanPreferencesKey("${prefix}sleep_enabled")
    val pomodoroEnabled = booleanPreferencesKey("${prefix}pomodoro_enabled")
    val requireAll = booleanPreferencesKey("${prefix}require_all_conditions")
    val blockedAppIds = stringSetPreferencesKey("${prefix}blocked_app_ids")
    val relaxDelayDays = intPreferencesKey("${prefix}relax_delay_days")
}

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.stepLockStore

    /** 실제로 적용 중인 조건. 잠금 판정이 쓰는 값입니다. */
    private val effective = ConditionKeys("")

    /** 사용자가 정해 둔 조건. 완화 대기 중이면 [effective] 보다 느슨합니다. */
    private val desired = ConditionKeys("desired_")

    private object Keys {
        val deviceUuid = stringPreferencesKey("device_uuid")
        val accountId = stringPreferencesKey("account_id")
        val displayName = stringPreferencesKey("display_name")
        val accountEmail = stringPreferencesKey("account_email")
        val guest = booleanPreferencesKey("guest")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        /** 예약된 완화가 적용되는 날(ISO). 없으면 예약이 없습니다. */
        val settingsApplyOn = stringPreferencesKey("settings_apply_on")
        val stepBaselineDate = stringPreferencesKey("step_baseline_date")
        val stepBaselineCounter = longPreferencesKey("step_baseline_counter")
        val pomodoroEndsAt = longPreferencesKey("pomodoro_ends_at")
        val pomodoroPausedRemaining = longPreferencesKey("pomodoro_paused_remaining")
        val pomodoroSessionsDate = stringPreferencesKey("pomodoro_sessions_date")
        val pomodoroSessionsCount = intPreferencesKey("pomodoro_sessions_count")
        val sleepMinutesDate = stringPreferencesKey("sleep_minutes_date")
        val sleepMinutes = intPreferencesKey("sleep_minutes")
        val dailyHistory = stringSetPreferencesKey("daily_history")
        val tempAllowUntil = longPreferencesKey("temp_allow_until")
        val tempAllowDate = stringPreferencesKey("temp_allow_date")
        val tempAllowCount = intPreferencesKey("temp_allow_count")
        val tempAllowBonus = intPreferencesKey("temp_allow_bonus")
        val settingsUpdatedAt = longPreferencesKey("settings_updated_at")
    }

    val preferences: Flow<AppPreferences> = store.data.map { it.toAppPreferences() }

    /** 첫 실행 시 기기 UUID를 만들어 모든 로컬 레코드에 부여합니다. */
    suspend fun ensureDeviceUuid(): String = store.edit { prefs ->
        if (prefs[Keys.deviceUuid].isNullOrBlank()) {
            prefs[Keys.deviceUuid] = UUID.randomUUID().toString()
        }
    }[Keys.deviceUuid].orEmpty()

    /**
     * 설정을 바꿉니다. **엄해지는 변경은 즉시, 느슨해지는 변경은 대기 기간 뒤에**
     * 적용됩니다.
     *
     * [transform] 은 사용자가 화면에서 보고 있는 값(= 예약 포함)을 받습니다.
     * 결과가 적용 중인 값보다 느슨하면 예약만 걸고 적용 중인 값은 건드리지 않습니다.
     *
     * 대기 기간은 **적용 중인 설정**의 것을 씁니다. 방금 줄인 기간을 쓰면
     * "7일 → 0일"로 바꾸는 변경이 스스로 0일 뒤에 적용되어 장치가 뚫립니다.
     */
    suspend fun updateSettings(transform: (LockSettings) -> LockSettings) {
        store.edit { prefs ->
            prefs.materializeDueRelaxation()

            val current = prefs.readConditions(effective)
            val next = transform(prefs.readConditions(desired))
            val delayDays = current.relaxDelay.days

            if (delayDays == 0 || !next.isLooserThan(current)) {
                prefs.writeConditions(effective, next)
                prefs.writeConditions(desired, next)
                prefs.remove(Keys.settingsApplyOn)
            } else {
                // 느슨해질 때마다 대기가 처음부터 다시 시작됩니다. 예약 중에 조건을
                // 더 풀어 두고 원래 날짜에 한꺼번에 받는 걸 막습니다.
                prefs.writeConditions(desired, next)
                prefs[Keys.settingsApplyOn] = LocalDate.now().plusDays(delayDays.toLong()).toString()
            }

            prefs.writeAccountFields(next)
            prefs[Keys.settingsUpdatedAt] = System.currentTimeMillis()
        }
    }

    /**
     * 서버 값이 더 최신일 때 통째로 덮어씁니다. 서버의 시각을 그대로 보관합니다.
     *
     * 서버 스냅샷은 그 자체가 결론이라 예약을 남겨 두지 않습니다 — 적용 중인 값과
     * 정해 둔 값을 모두 서버 값으로 맞추고 예약을 지웁니다.
     */
    suspend fun applyRemoteSettings(settings: LockSettings, updatedAt: Long) {
        store.edit { prefs ->
            prefs.writeConditions(effective, settings)
            prefs.writeConditions(desired, settings)
            prefs.remove(Keys.settingsApplyOn)
            prefs.writeAccountFields(settings)
            prefs[Keys.settingsUpdatedAt] = updatedAt
        }
    }

    /** 대기 기간만 바꿉니다 — 늘리면 즉시, 줄이면 현재 기간을 기다립니다. */
    suspend fun setRelaxDelay(delay: RelaxDelay) {
        updateSettings { it.copy(relaxDelay = delay) }
    }

    /**
     * 예약 날짜가 지났으면 적용 중인 값을 정해 둔 값으로 맞춥니다.
     *
     * 읽기(Flow)는 저장소를 고칠 수 없으므로 [toAppPreferences] 는 같은 판단을
     * 계산으로만 합니다. 여기서는 다음 쓰기 때 실제로 정리해 둡니다 — 그래야
     * 다음 비교가 낡은 값을 기준으로 이뤄지지 않습니다.
     */
    private fun MutablePreferences.materializeDueRelaxation() {
        val applyOn = this[Keys.settingsApplyOn]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return
        if (LocalDate.now() < applyOn) return
        writeConditions(effective, readConditions(desired))
        remove(Keys.settingsApplyOn)
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

    private fun MutablePreferences.writeConditions(keys: ConditionKeys, next: LockSettings) {
        this[keys.stepGoal] = next.stepGoal
        this[keys.sleepGoalHours] = next.sleepGoalHours
        this[keys.pomodoroGoal] = next.pomodoroGoal
        this[keys.stepsEnabled] = next.stepsEnabled
        this[keys.sleepEnabled] = next.sleepEnabled
        this[keys.pomodoroEnabled] = next.pomodoroEnabled
        this[keys.requireAll] = next.requireAllConditions
        this[keys.blockedAppIds] = next.blockedAppIds
        this[keys.relaxDelayDays] = next.relaxDelay.days
    }

    /** 계정 정보는 잠금 조건이 아니라 예약 대상이 아닙니다 — 언제나 바로 씁니다. */
    private fun MutablePreferences.writeAccountFields(next: LockSettings) {
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
    /**
     * 임시 허용을 한 번 씁니다. 하루 한도를 넘으면 아무것도 바꾸지 않고 false 를 돌려줍니다.
     * 호출자가 화면을 닫기 전에 결과를 확인해야, 한도를 넘긴 상태로 잠금이 풀리지 않습니다.
     */
    suspend fun useTemporaryAllow(): Boolean {
        var granted = false
        store.edit { prefs ->
            val today = LocalDate.now().toString()
            val sameDay = prefs[Keys.tempAllowDate] == today
            val usedToday = if (sameDay) prefs[Keys.tempAllowCount] ?: 0 else 0
            val bonusToday = if (sameDay) prefs[Keys.tempAllowBonus] ?: 0 else 0
            if (usedToday >= TemporaryAllow.DAILY_LIMIT + bonusToday) return@edit
            prefs[Keys.tempAllowDate] = today
            prefs[Keys.tempAllowCount] = usedToday + 1
            // 날짜 키 하나가 사용 횟수와 보너스의 유효 기간을 함께 쥐고 있어서,
            // 날짜를 새로 쓸 때 보너스도 같은 기준으로 다시 적어 둡니다.
            prefs[Keys.tempAllowBonus] = bonusToday
            prefs[Keys.tempAllowUntil] =
                System.currentTimeMillis() + TemporaryAllow.MINUTES * 60_000L
            granted = true
        }
        return granted
    }

    /**
     * 리워드 광고를 **끝까지 본 뒤에** 호출합니다. 임시 허용 횟수를 한 번 늘려 주고,
     * 하루 보너스 한도를 넘으면 아무것도 바꾸지 않고 false 를 돌려줍니다.
     *
     * 허용을 바로 쓰지는 않습니다 — 횟수만 늘려 두고, 실제로 쓸지는 사용자가
     * [useTemporaryAllow] 로 한 번 더 결정합니다.
     */
    suspend fun grantTemporaryAllowBonus(): Boolean {
        var granted = false
        store.edit { prefs ->
            val today = LocalDate.now().toString()
            val sameDay = prefs[Keys.tempAllowDate] == today
            val bonusToday = if (sameDay) prefs[Keys.tempAllowBonus] ?: 0 else 0
            if (bonusToday >= TemporaryAllow.AD_BONUS_LIMIT) return@edit
            val usedToday = if (sameDay) prefs[Keys.tempAllowCount] ?: 0 else 0
            prefs[Keys.tempAllowDate] = today
            prefs[Keys.tempAllowCount] = usedToday
            prefs[Keys.tempAllowBonus] = bonusToday + 1
            granted = true
        }
        return granted
    }

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

    /**
     * 한 벌의 조건을 읽습니다.
     *
     * `desired_` 쪽은 처음에는 비어 있습니다. 그때는 적용 중인 값으로 떨어져야
     * 설정 화면이 빈 기본값을 보여 주지 않습니다.
     */
    private fun Preferences.readConditions(keys: ConditionKeys): LockSettings {
        val defaults = LockSettings(deviceUuid = this[Keys.deviceUuid].orEmpty())

        /**
         * 이 벌에 값이 없으면 적용 중인 벌 → 기본값 순으로 떨어집니다.
         *
         * `desired_` 는 처음 한 번도 설정을 바꾸지 않은 기기에서는 비어 있습니다.
         * 그때 기본값으로 떨어지면, 이미 목표를 바꿔 둔 사람의 설정 화면이
         * 갑자기 8,000보로 보입니다.
         */
        fun <T> read(own: (ConditionKeys) -> Preferences.Key<T>, fallback: T): T =
            this[own(keys)] ?: this[own(effective)] ?: fallback

        return defaults.copy(
            accountId = this[Keys.accountId],
            displayName = this[Keys.displayName],
            accountEmail = this[Keys.accountEmail],
            stepGoal = read({ it.stepGoal }, defaults.stepGoal),
            sleepGoalHours = read({ it.sleepGoalHours }, defaults.sleepGoalHours),
            pomodoroGoal = read({ it.pomodoroGoal }, defaults.pomodoroGoal),
            stepsEnabled = read({ it.stepsEnabled }, defaults.stepsEnabled),
            sleepEnabled = read({ it.sleepEnabled }, defaults.sleepEnabled),
            pomodoroEnabled = read({ it.pomodoroEnabled }, defaults.pomodoroEnabled),
            requireAllConditions = read({ it.requireAll }, defaults.requireAllConditions),
            blockedAppIds = read({ it.blockedAppIds }, defaults.blockedAppIds),
            relaxDelay = RelaxDelay.fromDays(
                read({ it.relaxDelayDays }, defaults.relaxDelay.days),
            ),
        )
    }

    private fun Preferences.toAppPreferences(): AppPreferences {
        val desiredSettings = readConditions(desired)
        val applyOn = this[Keys.settingsApplyOn]
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        // 날짜가 지났으면 읽는 시점에 이미 적용된 것으로 봅니다. 읽기(Flow)는
        // 저장소를 고칠 수 없어서 계산으로 맞추고, 실제 정리는 다음 쓰기 때
        // materializeDueRelaxation() 이 합니다. 앱을 안 열어도 날이 지나면 풀립니다.
        val due = applyOn != null && !LocalDate.now().isBefore(applyOn)
        val settings = if (due) desiredSettings else readConditions(effective)
        val accountId = this[Keys.accountId]
        return AppPreferences(
            settings = settings,
            desiredSettings = desiredSettings,
            settingsApplyOn = applyOn?.takeIf { !due },
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
            temporaryAllow = run {
                val sameDay = this[Keys.tempAllowDate] == LocalDate.now().toString()
                TemporaryAllowState(
                    allowedUntil = this[Keys.tempAllowUntil],
                    usedToday = if (sameDay) this[Keys.tempAllowCount] ?: 0 else 0,
                    bonusEarnedToday = if (sameDay) this[Keys.tempAllowBonus] ?: 0 else 0,
                )
            },
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
