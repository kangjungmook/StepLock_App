package com.steplock.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.steplock.app.data.AuthRepository
import com.steplock.app.data.AuthState
import com.steplock.app.data.BlockedAppCatalog
import com.steplock.app.data.DailyStat
import com.steplock.app.data.HISTORY_DAYS
import com.steplock.app.data.LockSettings
import com.steplock.app.data.Pomodoro
import com.steplock.app.data.RelaxDelay
import com.steplock.app.data.SettingsRepository
import com.steplock.app.data.SleepRepository
import com.steplock.app.data.StreakCalculator
import com.steplock.app.data.StepTracker
import com.steplock.app.data.SyncRepository
import com.steplock.app.ui.components.SocialProvider
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val paused: Boolean,
    val sessionsToday: Int,
    val goal: Int,
)

class StepLockViewModel(
    private val repository: SettingsRepository,
    stepTracker: StepTracker,
    private val sleepRepository: SleepRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    val apps = BlockedAppCatalog.apps

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

        // 세션은 Supabase가 복구해 주고, 우리는 계정 귀속만 따라갑니다.
        viewModelScope.launch {
            authRepository.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        val accountId = user?.id
                        if (accountId != null) {
                            repository.setAccount(accountId = accountId, email = user.email)
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

    fun toggleBlockedApp(appId: String) = edit {
        val blocked = it.blockedAppIds
        it.copy(blockedAppIds = if (appId in blocked) blocked - appId else blocked + appId)
    }

    fun signIn(email: String, password: String) = submitCredentials(email, password) { mail, pass ->
        authRepository.signInWithEmail(mail, pass) to LoginError.SignInFailed
    }

    fun signUp(email: String, password: String) = submitCredentials(email, password) { mail, pass ->
        authRepository.signUpWithEmail(mail, pass) to LoginError.SignUpFailed
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
        val validationError = when {
            !trimmed.contains('@') || trimmed.length < 5 -> LoginError.InvalidEmail
            password.length < 6 -> LoginError.ShortPassword
            else -> null
        }
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
            uiState.value?.let { repository.recordDay(it.today) }
        }
    }

    private fun edit(transform: (LockSettings) -> LockSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
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
                )
            }
        }
    }
}
