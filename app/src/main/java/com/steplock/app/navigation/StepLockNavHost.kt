package com.steplock.app.navigation

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import com.steplock.app.data.BlockedAppCatalog
import com.steplock.app.service.AppWatchService
import com.steplock.app.service.PomodoroService
import com.steplock.app.system.AppPermissions
import com.steplock.app.system.PermissionStep
import com.steplock.app.system.nextPermissionStep
import com.steplock.app.ui.StepLockUiState
import com.steplock.app.ui.StepLockViewModel
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.screens.HomeScreen
import com.steplock.app.ui.screens.LockOverlayScreen
import com.steplock.app.ui.screens.LoginScreen
import com.steplock.app.ui.screens.OnboardingScreen
import com.steplock.app.ui.screens.PomodoroScreen
import com.steplock.app.ui.screens.SettingsScreen
import com.steplock.app.ui.screens.StatsScreen
import com.steplock.app.ui.theme.SlColor

object Route {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val POMODORO = "pomodoro"
    const val LOCK = "lock/{appId}"

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
        if (state.onboardingCompleted) Route.HOME else Route.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Route.LOGIN) {
            LoginScreen(
                onLogin = { _, _, _ -> navController.navigate(Route.ONBOARDING) },
                onSocialLogin = { navController.navigate(Route.ONBOARDING) },
                onGuestContinue = {
                    viewModel.continueAsGuest()
                    navController.navigate(Route.ONBOARDING)
                },
                onForgotPassword = {},
                onSignUp = {},
            )
        }

        composable(Route.ONBOARDING) {
            val context = LocalContext.current
            var step by remember { mutableStateOf(nextPermissionStep(context)) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                step = nextPermissionStep(context)
            }
            val activityRecognitionRequest = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { step = nextPermissionStep(context) }

            OnboardingScreen(
                ctaText = stringResource(step.ctaRes),
                onCtaClick = {
                    when (step) {
                        PermissionStep.ActivityRecognition ->
                            activityRecognitionRequest.launch(Manifest.permission.ACTIVITY_RECOGNITION)

                        PermissionStep.UsageAccess ->
                            context.startActivity(AppPermissions.usageAccessSettings())

                        PermissionStep.Overlay ->
                            context.startActivity(AppPermissions.overlaySettings(context))

                        PermissionStep.Ready -> {
                            AppWatchService.start(context)
                            viewModel.completeOnboarding()
                            navController.navigate(Route.HOME) {
                                popUpTo(startDestination) { inclusive = true }
                            }
                        }
                    }
                },
            )
        }

        composable(Route.HOME) {
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSleep()
                viewModel.recordToday()
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
            )
        }

        composable(Route.STATS) {
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.recordToday() }
            StatsScreen(
                weekly = state.weekly,
                settings = state.settings,
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
                settings = state.settings,
                apps = viewModel.apps,
                onBack = { navController.popBackStack() },
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
                onDismiss = { navController.popBackStack() },
                onTemporaryAllow = { navController.popBackStack() },
            )
        }
    }
}
