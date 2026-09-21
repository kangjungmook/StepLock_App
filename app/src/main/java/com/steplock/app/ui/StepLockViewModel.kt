package com.steplock.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.steplock.app.data.AuthState
import com.steplock.app.data.SampleData
import com.steplock.app.navigation.Route

/**
 * 화면 확인용 상태 보관소. 센서·저장소·인증 연결 전이라 값은 데모 데이터에서 시작합니다.
 * 목표값을 바꾸면 홈 게이지와 잠금 화면 문구가 함께 갱신됩니다.
 */
class StepLockViewModel : ViewModel() {

    var settings by mutableStateOf(SampleData.settings)
        private set

    var authState by mutableStateOf<AuthState>(AuthState.Unknown)
        private set

    var onboardingCompleted by mutableStateOf(false)
        private set

    val today = SampleData.today
    val apps = SampleData.apps
    val userName = SampleData.USER_NAME

    /** 온보딩을 마친 기기는 로그인을 건너뛰고 홈으로 들어갑니다. */
    val startDestination: String
        get() = when {
            onboardingCompleted -> Route.HOME
            authState == AuthState.Unknown -> Route.LOGIN
            else -> Route.ONBOARDING
        }

    fun setStepsEnabled(enabled: Boolean) {
        settings = settings.copy(stepsEnabled = enabled)
    }

    fun setSleepEnabled(enabled: Boolean) {
        settings = settings.copy(sleepEnabled = enabled)
    }

    fun setPomodoroEnabled(enabled: Boolean) {
        settings = settings.copy(pomodoroEnabled = enabled)
    }

    fun setRequireAllConditions(enabled: Boolean) {
        settings = settings.copy(requireAllConditions = enabled)
    }

    fun changeStepGoal(delta: Int) {
        settings = settings.copy(stepGoal = (settings.stepGoal + delta).coerceIn(1000, 20000))
    }

    fun changeSleepGoal(delta: Float) {
        settings = settings.copy(sleepGoalHours = (settings.sleepGoalHours + delta).coerceIn(4f, 12f))
    }

    fun changePomodoroGoal(delta: Int) {
        settings = settings.copy(pomodoroGoal = (settings.pomodoroGoal + delta).coerceIn(1, 8))
    }

    fun toggleBlockedApp(appId: String) {
        val blocked = settings.blockedAppIds
        settings = settings.copy(
            blockedAppIds = if (appId in blocked) blocked - appId else blocked + appId,
        )
    }

    fun continueAsGuest() {
        authState = AuthState.Guest
    }

    fun completeOnboarding() {
        onboardingCompleted = true
    }
}
