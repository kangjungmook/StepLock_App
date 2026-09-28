package com.steplock.app.navigation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.steplock.app.R
import com.steplock.app.data.AuthState
import com.steplock.app.service.AppWatchService
import com.steplock.app.system.AppPermissions
import com.steplock.app.system.PermissionGroup
import com.steplock.app.system.PermissionStep
import com.steplock.app.system.nextPermissionStep
import com.steplock.app.system.permissionStates
import com.steplock.app.ui.StepLockUiState
import com.steplock.app.ui.StepLockViewModel
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.screens.AppPickerScreen
import com.steplock.app.ui.screens.HomeScreen
import com.steplock.app.ui.screens.LoginScreen
import com.steplock.app.ui.screens.LoginTrigger
import com.steplock.app.ui.screens.OnboardingScreen
import com.steplock.app.ui.screens.PomodoroScreen
import com.steplock.app.ui.screens.SettingsScreen
import com.steplock.app.ui.screens.SignUpScreen
import com.steplock.app.ui.screens.StatsScreen
import com.steplock.app.ui.screens.TutorialScreen
import com.steplock.app.ui.screens.messageRes
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.ThemeModeApplier

/**
 * 하단 탭 이동. 홈을 바닥에 두고 그 위에 탭 하나만 올립니다.
 *
 * 전에는 탭을 누를 때마다 화면을 쌓아서, 홈→통계→설정→통계… 를 오가면 뒤로 가기를
 * 그만큼 눌러야 앱을 나갈 수 있었습니다. 이제 어느 탭에서든 뒤로 가기 한 번이면
 * 홈으로, 홈에서 한 번 더 누르면 앱을 나갑니다.
 */
private fun NavController.navigateTab(tab: NavTab) {
    when (tab) {
        NavTab.Home -> popBackStack(Route.HOME, inclusive = false)
        NavTab.Stats, NavTab.Settings -> navigate(
            if (tab == NavTab.Stats) Route.STATS else Route.SETTINGS,
        ) {
            popUpTo(Route.HOME)
            launchSingleTop = true
        }
    }
}

object Route {
    const val LOGIN = "login?trigger={trigger}"
    const val SIGNUP = "signup?trigger={trigger}"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val POMODORO = "pomodoro"
    const val APP_PICKER = "app-picker"
    const val TUTORIAL = "tutorial?replay={replay}"

    fun login(trigger: LoginTrigger = LoginTrigger.AppStart) = "login?trigger=${trigger.name}"

    fun signUp(trigger: LoginTrigger) = "signup?trigger=${trigger.name}"

    fun tutorial(replay: Boolean = false) = "tutorial?replay=$replay"
}

private fun triggerArgument() = navArgument("trigger") {
    type = NavType.StringType
    defaultValue = LoginTrigger.AppStart.name
}

private fun NavBackStackEntry.loginTrigger(): LoginTrigger =
    runCatching { LoginTrigger.valueOf(arguments?.getString("trigger").orEmpty()) }
        .getOrDefault(LoginTrigger.AppStart)

/**
 * 로그인(또는 가입)이 끝났을 때. 앱을 처음 열어 들어온 경우엔 튜토리얼이나 홈으로
 * 넘어가며 로그인·가입 화면을 모두 지우고, 설정 등에서 들어온 경우엔 로그인 화면
 * 앞으로 돌아갑니다 — 가입 화면에서 끝났어도 로그인 화면이 남지 않게.
 */
private fun NavController.leaveAuth(
    trigger: LoginTrigger,
    onboardingCompleted: Boolean,
    startDestination: String,
) {
    if (trigger == LoginTrigger.AppStart) {
        navigate(if (onboardingCompleted) Route.HOME else Route.tutorial()) {
            popUpTo(startDestination) { inclusive = true }
        }
    } else {
        popBackStack(Route.LOGIN, inclusive = true)
    }
}

/**
 * @param openFocus 잠금 화면의 "집중 타이머" 로 들어왔을 때 true. 한 번 이동한 뒤
 *   [onFocusOpened] 로 비웁니다.
 */
@Composable
fun StepLockNavHost(openFocus: Boolean = false, onFocusOpened: () -> Unit = {}) {
    val context = LocalContext.current
    val viewModel: StepLockViewModel = viewModel(factory = StepLockViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val loaded = state
    if (loaded == null) {
        Box(Modifier.fillMaxSize().background(SlColor.Background))
        return
    }
    StepLockNavGraph(
        viewModel = viewModel,
        state = loaded,
        openFocus = openFocus,
        onFocusOpened = onFocusOpened,
    )
}

@Composable
private fun StepLockNavGraph(
    viewModel: StepLockViewModel,
    state: StepLockUiState,
    openFocus: Boolean,
    onFocusOpened: () -> Unit,
) {
    val navController = rememberNavController()
    val startDestination = remember {
        if (state.onboardingCompleted) Route.HOME else Route.login()
    }

    // 잠금 화면에서 "집중 타이머로 풀기" 를 누르고 들어온 경우. 홈 위에 타이머를
    // 올려서, 뒤로 가면 홈이 나오게 합니다.
    LaunchedEffect(openFocus) {
        if (openFocus && state.onboardingCompleted) {
            navController.navigate(Route.POMODORO) { launchSingleTop = true }
            onFocusOpened()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(
            route = Route.LOGIN,
            arguments = listOf(triggerArgument()),
        ) { entry ->
            val trigger = entry.loginTrigger()
            val loginState = viewModel.loginState

            // 이메일·소셜 모두 세션이 붙는 순간 여기로 들어옵니다.
            LaunchedEffect(state.authState) {
                if (state.authState is AuthState.SignedIn) {
                    navController.leaveAuth(trigger, state.onboardingCompleted, startDestination)
                }
            }

            LoginScreen(
                onLogin = { email, password -> viewModel.signIn(email, password) },
                onSignUp = {
                    viewModel.resetLoginState()
                    navController.navigate(Route.signUp(trigger))
                },
                onSocialLogin = viewModel::signInWithSocial,
                onGuestContinue = {
                    viewModel.continueAsGuest()
                    navController.navigate(Route.tutorial())
                },
                onForgotPassword = viewModel::requestPasswordReset,
                trigger = trigger,
                submitting = loginState.submitting,
                errorText = loginState.error?.let { stringResource(it.messageRes()) },
                noticeText = loginState.notice?.let { stringResource(it.messageRes()) },
            )
        }

        composable(
            route = Route.SIGNUP,
            arguments = listOf(triggerArgument()),
        ) { entry ->
            val trigger = entry.loginTrigger()
            val loginState = viewModel.loginState

            // 인증을 끈 프로젝트거나 소셜로 시작하면 바로 세션이 붙습니다.
            // 인증 메일의 링크로 돌아온 경우도 여기로 들어옵니다.
            LaunchedEffect(state.authState) {
                if (state.authState is AuthState.SignedIn) {
                    navController.leaveAuth(trigger, state.onboardingCompleted, startDestination)
                }
            }

            SignUpScreen(
                onSignUp = viewModel::signUp,
                onSocialLogin = viewModel::signInWithSocial,
                onBackToLogin = {
                    viewModel.resetLoginState()
                    navController.popBackStack()
                },
                submitting = loginState.submitting,
                errorText = loginState.error?.let { stringResource(it.messageRes()) },
                confirmSentTo = loginState.confirmSentTo,
            )
        }

        // 권한을 묻기 전에 무엇을 하는 앱인지 캐릭터가 먼저 알려 줍니다.
        composable(
            route = Route.TUTORIAL,
            arguments = listOf(
                navArgument("replay") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val replay = entry.arguments?.getBoolean("replay") ?: false
            TutorialScreen(
                replay = replay,
                onFinish = {
                    if (replay) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Route.ONBOARDING)
                    }
                },
            )
        }

        composable(Route.ONBOARDING) {
            val context = LocalContext.current
            // 설정 화면에 나갔다 돌아오면 다시 확인해야 체크가 붙습니다.
            var granted by remember { mutableStateOf(permissionStates(context)) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                granted = permissionStates(context)
            }
            // 두 번 거절하면 안드로이드가 대화상자를 더 띄우지 않습니다. 그때도
            // 계속 launch() 만 부르면 눌러도 아무 일이 없어 고장처럼 보이므로,
            // 거절당한 뒤에는 앱 정보 화면으로 보내 직접 켜게 합니다.
            var runtimeDenied by remember { mutableStateOf(false) }
            // 걸음 수와 알림은 런타임 권한이라 **대화상자 하나로 함께** 물을 수 있습니다.
            val runtimeRequest = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { result ->
                granted = permissionStates(context)
                if (result.values.any { !it }) runtimeDenied = true
            }

            OnboardingScreen(
                permissions = PermissionGroup.entries.map { it to (granted[it] == true) },
                onPermissionClick = { group ->
                    when (group) {
                        PermissionGroup.Runtime -> if (runtimeDenied) {
                            context.startActivity(AppPermissions.appDetailsSettings(context))
                        } else {
                            runtimeRequest.launch(AppPermissions.runtimePermissions())
                        }

                        PermissionGroup.UsageAccess ->
                            context.startActivity(AppPermissions.usageAccessSettings())

                        PermissionGroup.Overlay ->
                            context.startActivity(AppPermissions.overlaySettings(context))
                    }
                },
                onStart = {
                    // 권한을 건너뛰고 들어올 수도 있습니다. 그때 감시 서비스를
                    // 띄우면 아무것도 감지하지 못하는 알림만 남으니, 갖춰졌을 때만
                    // 시작합니다 — 나중에 허용하면 MainActivity.onStart 가 띄웁니다.
                    if (nextPermissionStep(context) == PermissionStep.Ready) {
                        AppWatchService.start(context)
                    }
                    viewModel.completeOnboarding()
                    navController.navigate(Route.HOME) {
                        popUpTo(startDestination) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.HOME) {
            val context = LocalContext.current
            // 온보딩 이후에도 권한이 꺼질 수 있어(사용자가 끄거나 배터리 최적화가 회수)
            // 홈으로 돌아올 때마다 다시 확인합니다.
            var permissionStep by remember { mutableStateOf(nextPermissionStep(context)) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                permissionStep = nextPermissionStep(context)
                viewModel.refreshSleep()
                viewModel.recordToday()
                viewModel.syncNow()
            }
            HomeScreen(
                userName = state.settings.displayName,
                stat = state.today,
                settings = state.settings,
                apps = state.blockedApps,
                selectedTab = NavTab.Home,
                onTabSelected = navController::navigateTab,
                // 홈의 "잠글 앱" 은 설정을 거치지 않고 바로 고르는 화면으로 갑니다.
                onManageLocks = { navController.navigate(Route.APP_PICKER) },
                onPomodoroClick = { navController.navigate(Route.POMODORO) },
                streak = state.streak,
                focusing = state.focusing,
                temporaryAllowUntil = state.temporaryAllowUntil,
                weekly = state.weekly,
                blockedToday = state.blockedToday,
                temporaryAllowRemaining = state.temporaryAllowRemaining,
                // 걸음 권한이 없으면 걸음만 못 세고, 나머지 둘은 잠금 자체가 멈춥니다.
                warningTitle = when (permissionStep) {
                    PermissionStep.Ready -> null
                    PermissionStep.ActivityRecognition -> stringResource(R.string.home_steps_stopped)
                    else -> stringResource(R.string.home_watch_stopped)
                },
                warningDescription = when (permissionStep) {
                    PermissionStep.Ready -> null
                    PermissionStep.ActivityRecognition ->
                        stringResource(R.string.home_permission_activity)
                    PermissionStep.UsageAccess -> stringResource(R.string.home_permission_usage)
                    PermissionStep.Overlay -> stringResource(R.string.home_permission_overlay)
                },
                onWarningClick = {
                    context.startActivity(
                        when (permissionStep) {
                            PermissionStep.UsageAccess -> AppPermissions.usageAccessSettings()
                            PermissionStep.Overlay -> AppPermissions.overlaySettings(context)
                            else -> AppPermissions.appDetailsSettings(context)
                        },
                    )
                },
            )
        }

        composable(Route.STATS) {
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.recordToday() }
            StatsScreen(
                weekly = state.weekly,
                monthly = state.monthly,
                settings = state.settings,
                streak = state.streak,
                longestStreak = state.longestStreak,
                selectedTab = NavTab.Stats,
                onTabSelected = navController::navigateTab,
            )
        }

        composable(Route.POMODORO) {
            val pomodoro by viewModel.pomodoro.collectAsStateWithLifecycle()
            val pomodoroState = pomodoro
            if (pomodoroState == null) {
                Box(Modifier.fillMaxSize().background(SlColor.Background))
            } else {
                PomodoroScreen(
                    state = pomodoroState,
                    onBack = { navController.popBackStack() },
                    onStart = viewModel::startPomodoro,
                    onPickApps = { navController.navigate(Route.APP_PICKER) },
                )
            }
        }

        composable(Route.SETTINGS) {
            val context = LocalContext.current
            val sleepPermissionRequest = rememberLauncherForActivityResult(
                PermissionController.createRequestPermissionResultContract(),
            ) { granted ->
                if (viewModel.sleepReadPermission in granted) {
                    viewModel.onSleepPermissionGranted()
                } else {
                    Toast.makeText(context, R.string.sleep_permission_denied, Toast.LENGTH_LONG)
                        .show()
                }
            }

            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            SettingsScreen(
                // 설정 화면은 **정해 둔 값**을 보여 줍니다 — 방금 누른 게 반영돼
                // 보이지 않으면 눌리지 않은 것처럼 느껴집니다. 실제 적용 시점은
                // settingsApplyOn 이 안내합니다.
                settings = state.desiredSettings,
                settingsApplyOn = state.settingsApplyOn,
                onRelaxDelayChange = viewModel::setRelaxDelay,
                blockedCount = state.desiredSettings.blockedAppIds.size,
                onPickApps = { navController.navigate(Route.APP_PICKER) },
                onReplayTutorial = { navController.navigate(Route.tutorial(replay = true)) },
                themeMode = themeMode,
                onThemeModeChange = { mode ->
                    viewModel.setThemeMode(mode)
                    ThemeModeApplier.apply(context, mode)
                },
                accountEmail = state.settings.accountEmail,
                accountName = state.settings.displayName,
                onSignIn = { navController.navigate(Route.login(LoginTrigger.Sync)) },
                onSignOut = viewModel::signOut,
                onTabSelected = navController::navigateTab,
                onDeleteAccount = viewModel::askDeleteAccount,
                onDeleteAccountConfirm = viewModel::confirmDeleteAccount,
                onDeleteAccountDismiss = viewModel::dismissDeleteAccount,
                deleteAccountConfirming = viewModel.deleteAccountState.confirming,
                deleteAccountDeleting = viewModel.deleteAccountState.deleting,
                deleteAccountErrorText = stringResource(R.string.delete_account_failed)
                    .takeIf { viewModel.deleteAccountState.failed },
                onStepsEnabledChange = viewModel::setStepsEnabled,
                onSleepEnabledChange = { enabled ->
                    when {
                        !enabled -> viewModel.setSleepEnabled(false)

                        !viewModel.isHealthConnectAvailable() -> Toast.makeText(
                            context,
                            R.string.sleep_health_connect_missing,
                            Toast.LENGTH_LONG,
                        ).show()

                        else -> viewModel.enableSleepIfPermitted {
                            sleepPermissionRequest.launch(setOf(viewModel.sleepReadPermission))
                        }
                    }
                },
                onPomodoroEnabledChange = viewModel::setPomodoroEnabled,
                onRequireAllChange = viewModel::setRequireAllConditions,
                onStepGoalChange = viewModel::changeStepGoal,
                onSleepGoalChange = viewModel::changeSleepGoal,
                onPomodoroGoalChange = viewModel::changePomodoroGoal,
                focusing = state.focusing,
            )
        }

        composable(Route.APP_PICKER) {
            AppPickerScreen(
                // 목록을 읽는 데 잠깐 걸려서 ViewModel 이 한 번만 읽어 들고 있습니다.
                apps = viewModel.availableApps,
                // 고른 값이 바로 체크로 보여야 하니 **정해 둔 값**을 씁니다.
                selected = state.desiredSettings.blockedAppIds,
                // 체크를 풀어도 완화 대기가 끝나야 실제로 열립니다 — 그 사이에
                // 무엇이 아직 막혀 있는지 화면에서 알려 줘야 합니다.
                stillBlocked = state.settings.blockedAppIds,
                applyOn = state.settingsApplyOn,
                onToggle = viewModel::toggleBlockedApp,
                onBack = { navController.popBackStack() },
                focusing = state.focusing,
                focusStartsOnLock = !state.focusing &&
                    state.settings.pomodoroEnabled &&
                    state.today.pomodoroSessions < state.settings.pomodoroGoal,
            )
        }
    }
}
