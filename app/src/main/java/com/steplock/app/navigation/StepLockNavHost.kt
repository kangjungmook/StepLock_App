package com.steplock.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.steplock.app.data.BlockedAppCatalog
import com.steplock.app.ui.StepLockUiState
import com.steplock.app.ui.StepLockViewModel
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.screens.HomeScreen
import com.steplock.app.ui.screens.LockOverlayScreen
import com.steplock.app.ui.screens.LoginScreen
import com.steplock.app.ui.screens.OnboardingScreen
import com.steplock.app.ui.screens.SettingsScreen
import com.steplock.app.ui.theme.SlColor

object Route {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
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
            OnboardingScreen(
                onGrantPermission = {
                    viewModel.completeOnboarding()
                    navController.navigate(Route.HOME) {
                        popUpTo(startDestination) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.HOME) {
            HomeScreen(
                userName = state.settings.displayName,
                stat = state.today,
                settings = state.settings,
                apps = viewModel.apps,
                selectedTab = NavTab.Home,
                onTabSelected = { tab ->
                    if (tab == NavTab.Settings) navController.navigate(Route.SETTINGS)
                },
                onManageLocks = { navController.navigate(Route.SETTINGS) },
                onAppClick = { app -> navController.navigate(Route.lock(app.id)) },
            )
        }

        composable(Route.SETTINGS) {
            SettingsScreen(
                settings = state.settings,
                apps = viewModel.apps,
                onBack = { navController.popBackStack() },
                onStepsEnabledChange = viewModel::setStepsEnabled,
                onSleepEnabledChange = viewModel::setSleepEnabled,
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
