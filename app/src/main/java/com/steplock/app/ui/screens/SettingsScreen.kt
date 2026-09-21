package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.data.BlockedApp
import com.steplock.app.data.LockSettings
import com.steplock.app.data.SampleData
import com.steplock.app.ui.components.AppListItem
import com.steplock.app.ui.components.CheckboxMark
import com.steplock.app.ui.components.ConditionSettingCard
import com.steplock.app.ui.components.IconTapTarget
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.components.SlSwitch
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.components.appBadgeColor
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.sleepGoalLabel

@Composable
fun SettingsScreen(
    settings: LockSettings,
    apps: List<BlockedApp>,
    accountEmail: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onStepsEnabledChange: (Boolean) -> Unit,
    onSleepEnabledChange: (Boolean) -> Unit,
    onPomodoroEnabledChange: (Boolean) -> Unit,
    onRequireAllChange: (Boolean) -> Unit,
    onStepGoalChange: (Int) -> Unit,
    onSleepGoalChange: (Float) -> Unit,
    onPomodoroGoalChange: (Int) -> Unit,
    onToggleApp: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTapTarget(
                icon = SlIcons.ArrowLeft,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
            )
            Text(
                text = stringResource(R.string.settings_title),
                style = SlText.ScreenTitle,
                color = SlColor.TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SlDimen.ScreenPadding,
                    end = SlDimen.ScreenPadding,
                    top = 4.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                SectionLabel(
                    text = stringResource(R.string.settings_section_account),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                AccountRow(
                    accountEmail = accountEmail,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut,
                )
            }

            val stepsTitle = stringResource(R.string.condition_steps)
            val sleepTitle = stringResource(R.string.condition_sleep)
            val pomodoroTitle = stringResource(R.string.condition_pomodoro)
            val stepsGoalLabel = stringResource(R.string.settings_steps_goal)
            val sleepGoalName = stringResource(R.string.settings_sleep_goal)
            val pomodoroGoalName = stringResource(R.string.settings_pomodoro_goal)

            ConditionSettingCard(
                icon = SlIcons.Steps,
                title = stepsTitle,
                enabled = settings.stepsEnabled,
                onEnabledChange = onStepsEnabledChange,
                toggleLabel = stringResource(R.string.settings_condition_toggle, stepsTitle),
                goalLabel = stepsGoalLabel,
                goalValue = stringResource(R.string.unit_steps, settings.stepGoal.formatThousands()),
                decreaseLabel = stringResource(R.string.settings_goal_decrease, stepsGoalLabel),
                increaseLabel = stringResource(R.string.settings_goal_increase, stepsGoalLabel),
                onDecrease = { onStepGoalChange(-500) },
                onIncrease = { onStepGoalChange(500) },
            )

            ConditionSettingCard(
                icon = SlIcons.Moon,
                title = sleepTitle,
                enabled = settings.sleepEnabled,
                onEnabledChange = onSleepEnabledChange,
                toggleLabel = stringResource(R.string.settings_condition_toggle, sleepTitle),
                goalLabel = sleepGoalName,
                goalValue = sleepGoalLabel(settings.sleepGoalHours),
                decreaseLabel = stringResource(R.string.settings_goal_decrease, sleepGoalName),
                increaseLabel = stringResource(R.string.settings_goal_increase, sleepGoalName),
                onDecrease = { onSleepGoalChange(-0.5f) },
                onIncrease = { onSleepGoalChange(0.5f) },
            )

            ConditionSettingCard(
                icon = SlIcons.Timer,
                title = pomodoroTitle,
                enabled = settings.pomodoroEnabled,
                onEnabledChange = onPomodoroEnabledChange,
                toggleLabel = stringResource(R.string.settings_condition_toggle, pomodoroTitle),
                goalLabel = pomodoroGoalName,
                goalValue = stringResource(R.string.unit_sessions, settings.pomodoroGoal),
                decreaseLabel = stringResource(R.string.settings_goal_decrease, pomodoroGoalName),
                increaseLabel = stringResource(R.string.settings_goal_increase, pomodoroGoalName),
                onDecrease = { onPomodoroGoalChange(-1) },
                onIncrease = { onPomodoroGoalChange(1) },
            )

            StrictModeRow(
                enabled = settings.requireAllConditions,
                onEnabledChange = onRequireAllChange,
            )

            Column {
                SectionLabel(
                    text = stringResource(R.string.settings_section_apps),
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                )
                SlPanel {
                    apps.forEachIndexed { index, app ->
                        if (index > 0) SlDivider()
                        val checked = app.id in settings.blockedAppIds
                        AppListItem(
                            name = app.name,
                            badgeInitial = app.initial,
                            badgeColor = appBadgeColor(app.id),
                            badgeSize = 36.dp,
                            nameStyle = SlText.ListItem,
                            verticalPadding = 12.dp,
                            modifier = Modifier.toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { onToggleApp(app.id) },
                            ),
                            trailing = { CheckboxMark(checked = checked) },
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.background(SlColor.Surface)) {
            SlDivider()
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = SlDimen.ScreenPadding, end = SlDimen.ScreenPadding, top = 12.dp, bottom = 16.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.settings_save),
                    onClick = onSave,
                )
            }
        }
    }
}

@Composable
private fun AccountRow(accountEmail: String?, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SlDimen.RadiusCard))
            .background(SlColor.SurfaceAlt)
            .padding(start = SlDimen.PanelPadding, top = 8.dp, end = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accountEmail ?: stringResource(R.string.settings_account_guest),
                style = SlText.RowTitle,
                color = SlColor.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (accountEmail != null) {
                    stringResource(R.string.settings_account_synced)
                } else {
                    stringResource(R.string.settings_account_guest_desc)
                },
                style = SlText.Caption,
                color = SlColor.TextSecondary,
            )
        }
        TextLink(
            text = if (accountEmail != null) {
                stringResource(R.string.settings_account_sign_out)
            } else {
                stringResource(R.string.settings_account_sign_in)
            },
            onClick = if (accountEmail != null) onSignOut else onSignIn,
            style = SlText.LinkSm,
            color = SlColor.BrandInk,
            underline = false,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun StrictModeRow(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SlDimen.RadiusCard))
            .background(SlColor.SurfaceAlt)
            .padding(SlDimen.PanelPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_strict_title),
                style = SlText.RowTitle,
                color = SlColor.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_strict_desc),
                style = SlText.Caption,
                color = SlColor.TextSecondary,
            )
        }
        SlSwitch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            contentDescription = stringResource(R.string.settings_strict_title),
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun SettingsScreenPreview() {
    StepLockTheme {
        SettingsScreen(
            settings = SampleData.settings,
            apps = SampleData.apps,
            accountEmail = "jiwoo@example.com",
            onSignIn = {},
            onSignOut = {},
            onBack = {},
            onStepsEnabledChange = {},
            onSleepEnabledChange = {},
            onPomodoroEnabledChange = {},
            onRequireAllChange = {},
            onStepGoalChange = {},
            onSleepGoalChange = {},
            onPomodoroGoalChange = {},
            onToggleApp = {},
            onSave = {},
        )
    }
}
