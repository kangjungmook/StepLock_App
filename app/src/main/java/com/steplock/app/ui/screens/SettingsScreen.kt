package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.BuildConfig
import com.steplock.app.R
import com.steplock.app.ads.Ads
import com.steplock.app.ads.findActivity
import com.steplock.app.data.BlockedApp
import com.steplock.app.data.LockSettings
import com.steplock.app.data.SampleData
import com.steplock.app.ui.components.AppListItem
import com.steplock.app.ui.components.CheckboxMark
import com.steplock.app.ui.components.ConditionSettingCard
import com.steplock.app.ui.components.IconTapTarget
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlChevron
import com.steplock.app.ui.components.SlConfirmDialog
import com.steplock.app.ui.components.SlDetailRow
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
    onDeleteAccount: () -> Unit,
    onDeleteAccountConfirm: () -> Unit,
    onDeleteAccountDismiss: () -> Unit,
    deleteAccountConfirming: Boolean,
    deleteAccountDeleting: Boolean,
    deleteAccountErrorText: String?,
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
                // 되돌릴 수 없는 동작이라 계정 줄과 떼어 놓고 눈에 덜 띄게 둡니다.
                if (accountEmail != null) {
                    TextLink(
                        text = stringResource(R.string.settings_account_delete),
                        onClick = onDeleteAccount,
                        style = SlText.LinkSm,
                        color = SlColor.TextSecondary,
                        underline = false,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
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

            AdsSection()
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

    if (deleteAccountConfirming) {
        SlConfirmDialog(
            title = stringResource(R.string.delete_account_title),
            description = stringResource(R.string.delete_account_desc),
            confirmText = if (deleteAccountDeleting) {
                stringResource(R.string.delete_account_deleting)
            } else {
                stringResource(R.string.delete_account_confirm)
            },
            cancelText = stringResource(R.string.delete_account_cancel),
            onConfirm = onDeleteAccountConfirm,
            onDismiss = onDeleteAccountDismiss,
            errorText = deleteAccountErrorText,
            busy = deleteAccountDeleting,
        )
    }
}

/**
 * 광고에 관해 사용자가 할 수 있는 일과 알아야 할 사실만 둡니다.
 *
 * 보여 줄 게 없으면 섹션 자체가 나타나지 않습니다. 동의 재설정은 그게 필요한
 * 지역에서만 뜨고, 테스트 광고 안내는 테스트 ID로 빌드했을 때만 뜹니다 —
 * 한국에서 실 광고로 배포하면 이 섹션은 보이지 않습니다.
 */
@Composable
private fun AdsSection() {
    val context = LocalContext.current
    val showConsentRow = Ads.privacyOptionsRequired
    val showTestNotice = BuildConfig.ADMOB_TEST_IDS
    if (!showConsentRow && !showTestNotice) return

    Column {
        SectionLabel(
            text = stringResource(R.string.settings_section_ads),
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
        )
        SlPanel {
            if (showConsentRow) {
                SlDetailRow(
                    title = stringResource(R.string.settings_ads_consent),
                    description = stringResource(R.string.settings_ads_consent_desc),
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            // Activity 가 없으면(이론상) 아무것도 하지 않습니다 —
                            // 동의 폼은 Activity 위에만 뜹니다.
                            context.findActivity()?.let { Ads.showPrivacyOptions(it) }
                        }
                        .padding(vertical = 16.dp),
                    trailing = { SlChevron() },
                )
            }
            if (showConsentRow && showTestNotice) SlDivider()
            if (showTestNotice) {
                SlDetailRow(
                    title = stringResource(R.string.settings_ads_test_title),
                    description = stringResource(R.string.settings_ads_test_desc),
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun AccountRow(accountEmail: String?, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    SlPanel(
        containerColor = SlColor.SurfaceAlt,
        borderColor = Color.Transparent,
        contentPadding = PaddingValues(
            start = SlDimen.PanelPadding,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp,
        ),
    ) {
        SlDetailRow(
            title = accountEmail ?: stringResource(R.string.settings_account_guest),
            description = if (accountEmail != null) {
                stringResource(R.string.settings_account_synced)
            } else {
                stringResource(R.string.settings_account_guest_desc)
            },
        ) {
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
            )
        }
    }
}

@Composable
private fun StrictModeRow(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    SlPanel(
        containerColor = SlColor.SurfaceAlt,
        borderColor = Color.Transparent,
        contentPadding = PaddingValues(SlDimen.PanelPadding),
    ) {
        SlDetailRow(
            title = stringResource(R.string.settings_strict_title),
            description = stringResource(R.string.settings_strict_desc),
        ) {
            SlSwitch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                contentDescription = stringResource(R.string.settings_strict_title),
            )
        }
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
            onDeleteAccount = {},
            onDeleteAccountConfirm = {},
            onDeleteAccountDismiss = {},
            deleteAccountConfirming = false,
            deleteAccountDeleting = false,
            deleteAccountErrorText = null,
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
