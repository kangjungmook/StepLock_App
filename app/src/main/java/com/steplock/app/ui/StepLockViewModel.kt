package com.steplock.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.steplock.app.data.AppUsageReader
import com.steplock.app.data.AuthRepository
import com.steplock.app.data.AuthState
import com.steplock.app.data.DailyStat
import com.steplock.app.data.HISTORY_DAYS
import com.steplock.app.data.InstalledApp
import com.steplock.app.data.InstalledAppsRepository
import com.steplock.app.data.LockSettings
import com.steplock.app.data.Pomodoro
import com.steplock.app.data.RelaxDelay
import com.steplock.app.data.SettingsRepository
import com.steplock.app.data.SleepRepository
import com.steplock.app.data.StreakCalculator
import com.steplock.app.data.StepTracker
import com.steplock.app.data.SyncRepository
import com.steplock.app.data.ThemeMode
import com.steplock.app.service.PomodoroService
import com.steplock.app.ui.components.SocialProvider
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDate

data class StepLockUiState(
    /** 적용 중인 설정 — 잠금 판정과 홈 화면이 씁니다. */
    val settings: LockSettings,
    /** 설정 화면에 보여 줄 값. 예약된 완화가 있으면 [settings] 보다 느슨합니다. */
    val desiredSettings: LockSettings,
    /** 예약된 완화가 적용되는 날. null 이면 예약이 없습니다. */
    val settingsApplyOn: LocalDate?,
    val today: DailyStat,
    /** 오늘까지 7일, 기록이 없는 날은 0으로 채웁니다. */
    val weekly: List<DailyStat>,
    /**
     * 오늘까지 30일. DataStore가 보관하는 기간과 같습니다 — 통계 화면에서
     * 기간을 바꿀 때 다시 읽지 않아도 되게 한 번에 올려 보냅니다.
     */
    val monthly: List<DailyStat>,
    val onboardingCompleted: Boolean,
    val authState: AuthState,
    /** 연속 달성 일수 — 오늘이 아직 미달이면 어제까지로 셉니다. */
    val streak: Int,
    val longestStreak: Int,
    /** 오늘 남은 임시 허용 횟수. 0이면 잠금 화면에서 버튼이 사라집니다. */
    val temporaryAllowRemaining: Int,
    /** 광고를 봐서 더 받을 수 있는 횟수. 0이면 광고 버튼도 사라집니다. */
    val temporaryAllowBonusRemaining: Int,
    /** 지금 잠그고 있는 앱. 이름은 기기에서 읽어 채웁니다. */
    val blockedApps: List<InstalledApp>,
    /** 집중 세션이 도는 중. 이 동안에는 조건을 채웠어도 잠급니다. */
    val focusing: Boolean = false,
    /** 집중 세션이 끝나는 시각(epoch ms). 잠금 화면이 남은 시간을 그립니다. */
    val focusEndsAt: Long? = null,
    /**
     * 임시 허용이 끝나는 시각(epoch ms). 지났을 수도 있어서 화면이 지금 시각과
     * 비교해야 합니다 — 이 값만으로 "허용 중"이라고 판단하면 안 됩니다.
     */
    val temporaryAllowUntil: Long? = null,
    /** 오늘 잠금 화면으로 막은 횟수 — 패키지 이름별. */
    val blockedToday: Map<String, Int> = emptyMap(),
    /** 오늘 잠근 앱별 사용 시간(ms) — 안드로이드 사용 기록 기준. */
    val usageToday: Map<String, Long> = emptyMap(),
)

enum class LoginError {
    InvalidEmail,
    ShortPassword,
    SignInFailed,
    SignUpFailed,
    SocialFailed,
    ResetFailed,
}

enum class LoginNotice { PasswordResetSent }

data class LoginUiState(
    val submitting: Boolean = false,
    val error: LoginError? = null,
    val notice: LoginNotice? = null,
    /** 회원가입 인증 메일을 보낸 주소. 가입 화면이 "메일함을 확인해 주세요"로 바뀝니다. */
    val confirmSentTo: String? = null,
)

/** 계정 삭제는 되돌릴 수 없어서 확인 → 진행 → 실패 단계를 화면이 구분해야 합니다. */
data class DeleteAccountUiState(
    val confirming: Boolean = false,
    val deleting: Boolean = false,
    val failed: Boolean = false,
)

data class PomodoroUiState(
    val remainingMs: Long,
    val progress: Float,
    val running: Boolean,
    val sessionsToday: Int,
    val goal: Int,
    /** 지금 할 수 있는 것 — 화면이 버튼과 스텝이의 말을 고릅니다. */
    val phase: FocusPhase,
)

/** 집중 타이머 화면의 상태. 세션은 시작하면 멈출 수 없어서 "멈춤"이 없습니다. */
enum class FocusPhase {
    /** 세션이 도는 중 — 끝날 때까지 기다리는 것 말고는 할 게 없습니다. */
    Running,

    /** 잠금이 걸려 있고 목표가 남았습니다. 다음 세션을 바로 시작할 수 있습니다. */
    Ready,

    /** 오늘 목표를 채웠습니다. */
    GoalMet,

    /** 잠근 앱이 없습니다. 앱을 잠그면 세션이 저절로 시작됩니다. */
    NoApps,

    /** 집중 조건을 꺼 두었습니다. */
    ConditionOff,
}

class StepLockViewModel(
    private val repository: SettingsRepository,
    stepTracker: StepTracker,
    private val sleepRepository: SleepRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val installedApps: InstalledAppsRepository,
    private val usageReader: AppUsageReader,
    /** 타이머 알림 서비스를 띄웁니다. 세션을 시작한 쪽이 부릅니다. */
    private val startFocusService: () -> Unit,
) : ViewModel() {

    /**
     * 기기에 깔린 앱 목록. 고르기 화면이 씁니다.
     *
     * PackageManager 조회라 값이 잘 바뀌지 않으니 한 번 읽어 둡니다 — 앱을 새로
     * 설치했으면 스텝락을 다시 켜야 목록에 나타납니다.
     */
    val availableApps: List<InstalledApp> by lazy { installedApps.launchableApps() }

    /** 화면 색 모드. 설정 화면이 지금 고른 칸을 표시하는 데 씁니다. */
    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.System)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    var loginState by mutableStateOf(LoginUiState())
        private set

    var deleteAccountState by mutableStateOf(DeleteAccountUiState())
        private set

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
                blockedUsageMinutes = (
                    prefs.usageToday
                        .filterKeys { it in prefs.settings.blockedAppIds }
                        .values.sum() / 60_000L
                    ).toInt(),
                lockedMinutes = (prefs.lockedSecondsToday / 60L).toInt(),
            )
            // 오늘 기록은 아직 history 에 없을 수 있어 따로 얹어 줘야 연속이 끊기지 않습니다.
            val withToday = prefs.history.filter { it.date != today.date } + today
            StepLockUiState(
                settings = prefs.settings,
                desiredSettings = prefs.desiredSettings,
                settingsApplyOn = prefs.settingsApplyOn,
                today = today,
                weekly = lastDays(7, prefs.history, today),
                monthly = lastDays(HISTORY_DAYS, prefs.history, today),
                onboardingCompleted = prefs.onboardingCompleted,
                authState = prefs.authState,
                streak = StreakCalculator.current(withToday, prefs.settings, today.date),
                longestStreak = StreakCalculator.longest(withToday, prefs.settings),
                temporaryAllowRemaining = prefs.temporaryAllow.remainingToday,
                temporaryAllowBonusRemaining = prefs.temporaryAllow.bonusRemaining,
                blockedApps = installedApps.resolve(prefs.settings.blockedAppIds),
                focusing = prefs.pomodoro.isRunning,
                focusEndsAt = prefs.pomodoro.endsAt,
                temporaryAllowUntil = prefs.temporaryAllow.allowedUntil,
                blockedToday = prefs.blockedToday,
                usageToday = prefs.usageToday,
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
            val settings = prefs.settings
            val remaining = state.endsAt
                ?.let { (it - System.currentTimeMillis()).coerceAtLeast(0L) }
                ?: Pomodoro.SESSION_MS
            PomodoroUiState(
                remainingMs = remaining,
                progress = 1f - (remaining.toFloat() / Pomodoro.SESSION_MS).coerceIn(0f, 1f),
                running = state.isRunning,
                sessionsToday = state.sessionsToday,
                goal = settings.pomodoroGoal,
                phase = when {
                    state.isRunning -> FocusPhase.Running
                    !settings.pomodoroEnabled -> FocusPhase.ConditionOff
                    settings.blockedAppIds.isEmpty() -> FocusPhase.NoApps
                    state.sessionsToday >= settings.pomodoroGoal -> FocusPhase.GoalMet
                    else -> FocusPhase.Ready
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { repository.ensureDeviceUuid() }

        // 세션은 Supabase가 복구해 주고, 우리는 계정 귀속만 따라갑니다.
        viewModelScope.launch {
            authRepository.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        val accountId = user?.id
                        if (accountId != null) {
                            repository.setAccount(
                                accountId = accountId,
                                email = user.email,
                                name = user.userMetadata?.providerName(),
                            )
                            loginState = LoginUiState()
                            syncRepository.sync(accountId)
                        }
                    }

                    is SessionStatus.NotAuthenticated -> repository.clearAccount()
                    else -> Unit
                }
            }
        }

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

    /**
     * 다음 세션을 지금 바로 시작합니다(세션 사이에 기다리지 않고 이어 가고 싶을 때).
     * 첫 세션은 앱을 잠그는 순간 저절로 시작되니 누를 일이 없습니다.
     */
    fun startPomodoro() {
        viewModelScope.launch {
            if (repository.startPomodoro()) startFocusService()
        }
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

    /** 잠근 앱이 있는 채로 집중 조건을 켜면 그때가 잠금이 걸리는 순간이라 세션이 시작됩니다. */
    fun setPomodoroEnabled(enabled: Boolean) =
        edit(autoStartFocus = enabled) { it.copy(pomodoroEnabled = enabled) }

    fun setRequireAllConditions(enabled: Boolean) = edit { it.copy(requireAllConditions = enabled) }

    /** 대기 기간 변경. 늘리면 즉시, 줄이면 현재 기간을 기다립니다. */
    fun setRelaxDelay(delay: RelaxDelay) {
        viewModelScope.launch { repository.setRelaxDelay(delay) }
    }

    fun changeStepGoal(delta: Int) = edit {
        it.copy(stepGoal = (it.stepGoal + delta).coerceIn(1000, 20000))
    }

    fun changeSleepGoal(delta: Float) = edit {
        it.copy(sleepGoalHours = (it.sleepGoalHours + delta).coerceIn(4f, 12f))
    }

    fun changePomodoroGoal(delta: Int) = edit {
        it.copy(pomodoroGoal = (it.pomodoroGoal + delta).coerceIn(1, 8))
    }

    /**
     * 잠글 앱 켜고 끄기. 빼는 건 완화라서 대기 기간이 걸려 있으면 기다립니다.
     *
     * 앱을 **잠그는 순간** 집중 조건이 남아 있으면 세션이 저절로 시작됩니다.
     * 집중 중에는 앱을 **뺄 수 없습니다** — 빼면 세션은 돌아도 막는 게 없어서,
     * 멈출 수 없게 한 의미가 사라집니다. 화면도 막지만 여기서 한 번 더 막습니다.
     */
    fun toggleBlockedApp(packageName: String) {
        viewModelScope.launch {
            val current = repository.preferences.first()
            val removing = packageName in current.desiredSettings.blockedAppIds
            if (removing && current.pomodoro.isRunning) return@launch
            repository.updateSettings {
                it.copy(
                    blockedAppIds = if (removing) {
                        it.blockedAppIds - packageName
                    } else {
                        it.blockedAppIds + packageName
                    },
                    // 다시 잠글 때는 언제나 앱 전체부터 — 예전 "쇼츠만"이 남아 있지 않게.
                    shortFormOnly = it.shortFormOnly - packageName,
                )
            }
            if (!removing && repository.autoStartFocusIfNeeded()) startFocusService()
        }
    }

    /**
     * 앱 전체 / 쇼츠만 바꾸기. "쇼츠만"으로 바꾸는 건 완화라서 대기 기간이 걸려 있으면
     * 기다리고, 집중 중에는 할 수 없습니다(앱을 빼는 것과 같은 이유).
     */
    fun setShortFormOnly(packageName: String, only: Boolean) {
        viewModelScope.launch {
            val current = repository.preferences.first()
            if (only && current.pomodoro.isRunning) return@launch
            repository.updateSettings {
                it.copy(
                    shortFormOnly = if (only) {
                        it.shortFormOnly + packageName
                    } else {
                        it.shortFormOnly - packageName
                    },
                )
            }
        }
    }

    fun signIn(email: String, password: String) = submitCredentials(email, password) { mail, pass ->
        authRepository.signInWithEmail(mail, pass) to LoginError.SignInFailed
    }

    fun signUp(email: String, password: String) {
        val trimmed = email.trim()
        val validationError = validateCredentials(trimmed, password)
        if (validationError != null) {
            loginState = LoginUiState(error = validationError)
            return
        }
        loginState = LoginUiState(submitting = true)
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(trimmed, password)
            loginState = when {
                result.isFailure -> LoginUiState(error = LoginError.SignUpFailed)
                // 인증 메일을 보냈으면 세션이 없어서 화면이 스스로 넘어가지 않습니다.
                // 가만히 있으면 가입이 된 건지 알 수 없으니 안내로 바꿉니다.
                result.getOrDefault(false) -> LoginUiState(confirmSentTo = trimmed)
                else -> LoginUiState()
            }
        }
    }

    /** 로그인 ↔ 회원가입을 오갈 때 앞 화면의 오류·안내를 비웁니다. */
    fun resetLoginState() {
        loginState = LoginUiState()
    }

    fun signInWithSocial(provider: SocialProvider) {
        loginState = LoginUiState(submitting = true)
        viewModelScope.launch {
            val result = when (provider) {
                SocialProvider.Google -> authRepository.signInWithGoogle()
                SocialProvider.Kakao -> authRepository.signInWithKakao()
                SocialProvider.Apple -> authRepository.signInWithApple()
            }
            loginState = if (result.isSuccess) {
                LoginUiState()
            } else {
                LoginUiState(error = LoginError.SocialFailed)
            }
        }
    }

    /** 잠금 화면에서 호출합니다. 하루 한도를 넘으면 false — 화면을 닫지 않아야 합니다. */
    suspend fun useTemporaryAllow(): Boolean = repository.useTemporaryAllow()

    /**
     * 리워드 광고를 끝까지 본 뒤 호출합니다. 횟수만 늘려 두고 잠금은 그대로 둡니다 —
     * 얻은 횟수를 지금 쓸지는 사용자가 한 번 더 누르며 정합니다.
     */
    fun grantTemporaryAllowBonus() {
        viewModelScope.launch { repository.grantTemporaryAllowBonus() }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun askDeleteAccount() {
        deleteAccountState = DeleteAccountUiState(confirming = true)
    }

    fun dismissDeleteAccount() {
        deleteAccountState = DeleteAccountUiState()
    }

    /**
     * 서버에서 계정을 지운 뒤 기기에 남은 데이터까지 정리합니다.
     * 서버 삭제가 실패하면 로컬은 건드리지 않습니다 — 지워지지 않은 계정을
     * 기기에서만 잊으면 사용자가 다시 로그인할 방법을 잃습니다.
     */
    fun confirmDeleteAccount() {
        deleteAccountState = DeleteAccountUiState(deleting = true)
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            if (result.isSuccess) {
                repository.clearAllLocalData()
                deleteAccountState = DeleteAccountUiState()
            } else {
                deleteAccountState = DeleteAccountUiState(confirming = true, failed = true)
            }
        }
    }

    fun requestPasswordReset(email: String) {
        val trimmed = email.trim()
        if (!trimmed.contains('@') || trimmed.length < 5) {
            loginState = LoginUiState(error = LoginError.InvalidEmail)
            return
        }
        loginState = LoginUiState(submitting = true)
        viewModelScope.launch {
            loginState = if (authRepository.sendPasswordReset(trimmed).isSuccess) {
                LoginUiState(notice = LoginNotice.PasswordResetSent)
            } else {
                LoginUiState(error = LoginError.ResetFailed)
            }
        }
    }

    fun dismissLoginError() {
        loginState = loginState.copy(error = null)
    }

    private fun submitCredentials(
        email: String,
        password: String,
        request: suspend (String, String) -> Pair<Result<Unit>, LoginError>,
    ) {
        val trimmed = email.trim()
        val validationError = validateCredentials(trimmed, password)
        if (validationError != null) {
            loginState = LoginUiState(error = validationError)
            return
        }
        loginState = LoginUiState(submitting = true)
        viewModelScope.launch {
            val (result, failure) = request(trimmed, password)
            loginState = if (result.isSuccess) LoginUiState() else LoginUiState(error = failure)
        }
    }

    private fun validateCredentials(email: String, password: String): LoginError? = when {
        !email.contains('@') || email.length < 5 -> LoginError.InvalidEmail
        password.length < 6 -> LoginError.ShortPassword
        else -> null
    }

    fun continueAsGuest() {
        viewModelScope.launch { repository.setGuest() }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.setOnboardingCompleted(true) }
    }

    /** 로그인된 계정이 있을 때만 서버와 맞춥니다. 게스트면 아무것도 하지 않습니다. */
    fun syncNow() {
        val accountId = uiState.value?.settings?.accountId ?: return
        viewModelScope.launch { syncRepository.sync(accountId) }
    }

    /** 통계에 쓰이도록 오늘 값을 이력에 적어 둡니다. 값이 같으면 쓰지 않습니다. */
    fun recordToday() {
        viewModelScope.launch {
            // 화면을 열 때마다 잠근 앱 사용 시간을 새로 읽습니다. 감시 서비스는 1분마다
            // 읽어서, 방금 유튜브를 보다 온 사람에게 1분 전 숫자를 보여 주게 됩니다.
            val state = uiState.value ?: return@launch
            val usage = withContext(Dispatchers.IO) {
                usageReader.todayUsage(state.settings.blockedAppIds)
            }
            repository.writeUsageToday(state.today.date, usage)
            repository.recordDay(
                state.today.copy(blockedUsageMinutes = (usage.values.sum() / 60_000L).toInt()),
            )
        }
    }

    private fun edit(autoStartFocus: Boolean = false, transform: (LockSettings) -> LockSettings) {
        viewModelScope.launch {
            repository.updateSettings(transform)
            if (autoStartFocus && repository.autoStartFocusIfNeeded()) startFocusService()
        }
    }

    /** 오늘로 끝나는 [days] 일치를 오래된 날부터 돌려줍니다. 빈 날은 0으로 채웁니다. */
    private fun lastDays(days: Int, history: List<DailyStat>, today: DailyStat): List<DailyStat> {
        val byDate = history.associateBy { it.date } + (today.date to today)
        return ((days - 1).toLong() downTo 0L).map { offset ->
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
                    authRepository = AuthRepository(),
                    syncRepository = SyncRepository(repository),
                    installedApps = InstalledAppsRepository(app),
                    usageReader = AppUsageReader(app),
                    // 백그라운드에서 막히면(안드로이드 12+) 세션은 저장돼 있으니 잠금과
                    // 집계는 감시 서비스가 이어서 합니다. 알림만 늦게 뜹니다.
                    startFocusService = { runCatching { PomodoroService.start(app) } },
                )
            }
        }
    }
}

/**
 * 로그인 사업자가 준 이름. Supabase 는 사업자마다 다른 이름 필드를 계정 메타데이터로
 * 옮겨 두는데(카카오는 닉네임을 name · full_name · preferred_username 에), 어느 키에
 * 들어올지 사업자마다 달라서 흔한 순서대로 봅니다. 없으면 null.
 */
private fun JsonObject.providerName(): String? =
    listOf("name", "full_name", "nickname", "preferred_username", "user_name")
        .firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        }
