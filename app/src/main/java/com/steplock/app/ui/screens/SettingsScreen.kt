package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.steplock.app.data.RelaxDelay
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
import com.steplock.app.ui.components.SlSegmented
import com.steplock.app.ui.components.SlSwitch
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.components.appBadgeColor
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.sleepGoalLabel
import java.time.LocalDate

/**
 * 잠금 조건 설정.
 *
 * [settings] 는 **사용자가 정해 둔 값**(예약 포함)입니다 — 방금 누른 게 화면에
 * 반영돼 보여야 하니까요. 실제로 적용되는 시점은 [settingsApplyOn] 이 알려 줍니다.
 */
@Composable
fun SettingsScreen(
    settings: LockSettings,
    /** 예약된 완화가 적용되는 날. null 이면 정해 둔 값이 이미 적용 중입니다. */
    settingsApplyOn: LocalDate?,
    onRelaxDelayChange: (RelaxDelay) -> Unit,
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
            // 맨 위에 둡니다. 목표를 낮췄는데 홈에서 아무 변화가 없으면 고장으로
            // 보이므로, 설정 화면을 열자마자 언제 적용되는지 알려 줘야 합니다.
            if (settingsApplyOn != null) {
                PendingRelaxNotice(applyOn = settingsApplyOn)
            }

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

            RelaxDelaySection(
                selected = settings.relaxDelay,
                onSelect = onRelaxDelayChange,
            )

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
 * 예약된 완화 안내.
 *
 * 앰버 팔레트를 씁니다 — 오류(빨강)가 아니고, 다 됐다는 신호(초록)도 아니라
 * "기다리는 중"이라는 세 번째 상태입니다. 잠금 화면의 남은 조건 안내와 같은 색입니다.
 */
@Composable
private fun PendingRelaxNotice(applyOn: LocalDate) {
    val today = LocalDate.now()
    val whenText = if (applyOn == today.plusDays(1)) {
        stringResource(R.string.settings_pending_tomorrow)
    } else {
        stringResource(R.string.settings_pending_date, applyOn.monthValue, applyOn.dayOfMonth)
    }

    SlPanel(
        containerColor = SlColor.AmberSurface,
        borderColor = SlColor.AmberBorder,
        contentPadding = PaddingValues(SlDimen.PanelPadding),
    ) {
        Text(
            text = stringResource(R.string.settings_pending_title, whenText),
            style = SlText.RowTitle,
            color = SlColor.AmberText,
        )
        Text(
            text = stringResource(R.string.settings_pending_desc),
            style = SlText.RowValue,
            color = SlColor.AmberSubText,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * 완화 대기 기간 고르기.
 *
 * 이 값을 **줄이는 것도** 대기 대상이라, 7일로 뒀다가 마음이 바뀌면 0으로
 * 돌아가는 데도 7일이 걸립니다. 그 사실을 아래 줄에 미리 적습니다 — 나중에
 * 알게 되면 앱이 고장 난 것처럼 느껴집니다.
 */
@Composable
private fun RelaxDelaySection(selected: RelaxDelay, onSelect: (RelaxDelay) -> Unit) {
    val options = listOf(
        RelaxDelay.Immediate to stringResource(R.string.settings_relax_immediate),
        RelaxDelay.NextDay to stringResource(R.string.settings_relax_next_day),
        RelaxDelay.ThreeDays to stringResource(R.string.settings_relax_three_days),
        RelaxDelay.SevenDays to stringResource(R.string.settings_relax_seven_days),
    )

    Column {
        SectionLabel(
            text = stringResource(R.string.settings_section_relax),
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
        )
        SlPanel(contentPadding = PaddingValues(SlDimen.PanelPadding)) {
            Text(
                text = stringResource(R.string.settings_relax_title),
                style = SlText.RowTitle,
                color = SlColor.TextPrimary,
            )
            Text(
                text = stringResource(R.string.settings_relax_desc),
                style = SlText.RowValue,
                color = SlColor.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(16.dp))
            SlSegmented(
                options = options.map { it.second },
                selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0),
                onSelect = { onSelect(options[it].first) },
                modifier = Modifier.fillMaxWidth(),
            )
            // 지금이 "바로"면 아직 스스로를 묶지 않은 상태라, 자기 참조 규칙을
            // 미리 꺼낼 필요가 없습니다.
            if (selected != RelaxDelay.Immediate) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_relax_self_note),
                    style = SlText.Caption,
                    color = SlColor.TextTertiary,
                )
            }
        }
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
            settingsApplyOn = null,
            onRelaxDelayChange = {},
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

/** 목표를 낮춰서 완화가 예약된 상태. 맨 위 안내와 대기 기간 칸을 같이 확인합니다. */
@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun SettingsScreenPendingPreview() {
    StepLockTheme {
        SettingsScreen(
            settings = SampleData.settings.copy(
                stepGoal = 5000,
                relaxDelay = RelaxDelay.ThreeDays,
            ),
            settingsApplyOn = LocalDate.now().plusDays(3),
            onRelaxDelayChange = {},
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
