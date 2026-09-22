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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.steplock.app.R
import com.steplock.app.data.AuthState
import com.steplock.app.data.BlockedAppCatalog
import com.steplock.app.service.AppWatchService
import com.steplock.app.service.PomodoroService
import com.steplock.app.system.AppPermissions
import com.steplock.app.system.PermissionGroup
import com.steplock.app.system.PermissionStep
import com.steplock.app.system.nextPermissionStep
import com.steplock.app.system.permissionStates
import com.steplock.app.ui.StepLockUiState
import com.steplock.app.ui.StepLockViewModel
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.screens.HomeScreen
import com.steplock.app.ui.screens.LockOverlayScreen
import com.steplock.app.ui.screens.LoginScreen
import com.steplock.app.ui.screens.LoginTrigger
import com.steplock.app.ui.screens.OnboardingScreen
import com.steplock.app.ui.screens.PomodoroScreen
import com.steplock.app.ui.screens.SettingsScreen
import com.steplock.app.ui.screens.StatsScreen
import com.steplock.app.ui.screens.messageRes
import com.steplock.app.ui.theme.SlColor

object Route {
    const val LOGIN = "login?trigger={trigger}"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val POMODORO = "pomodoro"
    const val LOCK = "lock/{appId}"

    fun login(trigger: LoginTrigger = LoginTrigger.AppStart) = "login?trigger=${trigger.name}"

    fun lock(appId: String) = "lock/$appId"
}

@Composable
fun StepLockNavHost() {
    val context = LocalContext.current
    val viewModel: StepLockViewModel = viewModel(factory = StepLockViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val loaded = state
    if (loaded == null) {
        Box(Modifier.fillMaxSize().background(SlColor.Background))
        return
    }
    StepLockNavGraph(viewModel = viewModel, state = loaded)
}

@Composable
private fun StepLockNavGraph(viewModel: StepLockViewModel, state: StepLockUiState) {
    val navController = rememberNavController()
    val startDestination = remember {
        if (state.onboardingCompleted) Route.HOME else Route.login()
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(
            route = Route.LOGIN,
            arguments = listOf(
                navArgument("trigger") {
                    type = NavType.StringType
                    defaultValue = LoginTrigger.AppStart.name
                },
            ),
        ) { entry ->
            val trigger = runCatching {
                LoginTrigger.valueOf(entry.arguments?.getString("trigger").orEmpty())
            }.getOrDefault(LoginTrigger.AppStart)
            val loginState = viewModel.loginState

            // 이메일·소셜 모두 세션이 붙는 순간 여기로 들어옵니다.
            LaunchedEffect(state.authState) {
                if (state.authState is AuthState.SignedIn) {
                    if (trigger == LoginTrigger.AppStart) {
                        navController.navigate(
                            if (state.onboardingCompleted) Route.HOME else Route.ONBOARDING,
                        ) {
                            popUpTo(startDestination) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            }

            LoginScreen(
                onLogin = { email, password, _ -> viewModel.signIn(email, password) },
                onSignUp = { email, password -> viewModel.signUp(email, password) },
                onSocialLogin = viewModel::signInWithSocial,
                onGuestContinue = {
                    viewModel.continueAsGuest()
                    navController.navigate(Route.ONBOARDING)
                },
                onForgotPassword = viewModel::requestPasswordReset,
                trigger = trigger,
                submitting = loginState.submitting,
                errorText = loginState.error?.let { stringResource(it.messageRes()) },
                noticeText = loginState.notice?.let { stringResource(it.messageRes()) },
            )
        }

        composable(Route.ONBOARDING) {
            val context = LocalContext.current
            // 설정 화면에 나갔다 돌아오면 다시 확인해야 체크가 붙습니다.
            var granted by remember { mutableStateOf(permissionStates(context)) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                granted = permissionStates(context)
            }
            // 걸음 수와 알림은 런타임 권한이라 **대화상자 하나로 함께** 물을 수 있습니다.
            val runtimeRequest = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { granted = permissionStates(context) }

            OnboardingScreen(
                permissions = PermissionGroup.entries.map { it to (granted[it] == true) },
                onPermissionClick = { group ->
                    when (group) {
                        PermissionGroup.Runtime ->
                            runtimeRequest.launch(AppPermissions.runtimePermissions())

                        PermissionGroup.UsageAccess ->
                            context.startActivity(AppPermissions.usageAccessSettings())

                        PermissionGroup.Overlay ->
                            context.startActivity(AppPermissions.overlaySettings(context))
                    }
                },
                onStart = {
                    AppWatchService.start(context)
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
                apps = viewModel.apps,
                selectedTab = NavTab.Home,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.Home -> Unit
                        NavTab.Stats -> navController.navigate(Route.STATS)
                        NavTab.Settings -> navController.navigate(Route.SETTINGS)
                    }
                },
                onManageLocks = { navController.navigate(Route.SETTINGS) },
                onAppClick = { app -> navController.navigate(Route.lock(app.id)) },
                onPomodoroClick = { navController.navigate(Route.POMODORO) },
                streak = state.streak,
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
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.Stats -> Unit
                        NavTab.Home -> navController.popBackStack(Route.HOME, false)
                        NavTab.Settings -> navController.navigate(Route.SETTINGS)
                    }
                },
            )
        }

        composable(Route.POMODORO) {
            val context = LocalContext.current
            val pomodoro by viewModel.pomodoro.collectAsStateWithLifecycle()
            val pomodoroState = pomodoro
            if (pomodoroState == null) {
                Box(Modifier.fillMaxSize().background(SlColor.Background))
            } else {
                PomodoroScreen(
                    state = pomodoroState,
                    onBack = { navController.popBackStack() },
                    onStart = {
                        viewModel.startPomodoro()
                        PomodoroService.start(context)
                    },
                    onPause = { viewModel.pausePomodoro() },
                    onResume = {
                        viewModel.resumePomodoro()
                        PomodoroService.start(context)
                    },
                    onReset = {
                        viewModel.resetPomodoro()
                        PomodoroService.stop(context)
                    },
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

            SettingsScreen(
                // 설정 화면은 **정해 둔 값**을 보여 줍니다 — 방금 누른 게 반영돼
                // 보이지 않으면 눌리지 않은 것처럼 느껴집니다. 실제 적용 시점은
                // settingsApplyOn 이 안내합니다.
                settings = state.desiredSettings,
                settingsApplyOn = state.settingsApplyOn,
                onRelaxDelayChange = viewModel::setRelaxDelay,
                apps = viewModel.apps,
                accountEmail = state.settings.accountEmail,
                onSignIn = { navController.navigate(Route.login(LoginTrigger.Sync)) },
                onSignOut = viewModel::signOut,
                onBack = { navController.popBackStack() },
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
                onToggleApp = viewModel::toggleBlockedApp,
                onSave = { navController.popBackStack() },
            )
        }

        composable(
            route = Route.LOCK,
            arguments = listOf(navArgument("appId") { type = NavType.StringType }),
        ) { entry ->
            val appId = entry.arguments?.getString("appId").orEmpty()
            val app = BlockedAppCatalog.byId(appId) ?: BlockedAppCatalog.apps.first()
            LockOverlayScreen(
                appName = app.name,
                stat = state.today,
                settings = state.settings,
                temporaryAllowRemaining = state.temporaryAllowRemaining,
                onDismiss = { navController.popBackStack() },
                onTemporaryAllow = { navController.popBackStack() },
            )
        }
    }
}
