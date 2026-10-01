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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.BuildConfig
import com.steplock.app.R
import com.steplock.app.ads.Ads
import com.steplock.app.ads.findActivity
import com.steplock.app.data.LockSettings
import com.steplock.app.data.Pomodoro
import com.steplock.app.data.RelaxDelay
import com.steplock.app.data.ThemeMode
import com.steplock.app.data.SampleData
import com.steplock.app.ui.components.BottomNavBar
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.components.RuleEditor
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlChevron
import com.steplock.app.ui.components.SlConfirmDialog
import com.steplock.app.ui.components.SlDetailRow
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.components.SlSegmented
import com.steplock.app.ui.components.SlSwitch
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
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
    /** 지금 고른 앱 개수. 목록은 별도 화면에서 고릅니다. */
    blockedCount: Int,
    onPickApps: () -> Unit,
    onReplayTutorial: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    accountEmail: String?,
    /** 계정 표시 이름(카카오 닉네임 등). 있으면 계정 줄 제목으로, 이메일은 그 아래에. */
    accountName: String? = null,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    /** 홈·통계와 같은 하단 탭. 설정도 탭 중 하나라 뒤로 가기 대신 탭으로 오갑니다. */
    onTabSelected: (NavTab) -> Unit,
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
    modifier: Modifier = Modifier,
    /** 집중 세션이 도는 중. 계정 삭제를 잠시 막습니다. */
    focusing: Boolean = false,
) {
    // 탭은 내용 위에 떠 있습니다 — 내용이 그 뒤로 흘러가도록 겹쳐 둡니다.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background),
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 탭 화면이라 뒤로 가기 화살표를 두지 않습니다 — 홈·통계와 같은 자리에
        // 제목만 둡니다. 화살표가 있으면 "하위 화면"으로 읽혀 탭이 사라진 것처럼 보입니다.
        Text(
            text = stringResource(R.string.settings_title),
            // 통계 탭 제목과 같은 크기·위치 — 탭을 바꿔도 제목이 튀지 않습니다.
            style = SlText.Greeting,
            color = SlColor.TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = SlDimen.ScreenPadding,
                    end = SlDimen.ScreenPadding,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SlDimen.ScreenPadding,
                    end = SlDimen.ScreenPadding,
                    top = 4.dp,
                    bottom = SlDimen.FloatingNavReserve,
                )
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 맨 위에 둡니다. 목표를 낮췄는데 홈에서 아무 변화가 없으면 고장으로
            // 보이므로, 설정 화면을 열자마자 언제 적용되는지 알려 줘야 합니다.
            if (settingsApplyOn != null) {
                PendingRelaxNotice(applyOn = settingsApplyOn)
            }

            // 규칙은 한 문장으로 — 밑줄 친 값을 누르면 시트에서 그 값만 고칩니다.
            RuleEditor(
                settings = settings,
                blockedCount = blockedCount,
                onPickApps = onPickApps,
                onStepsEnabledChange = onStepsEnabledChange,
                onSleepEnabledChange = onSleepEnabledChange,
                onPomodoroEnabledChange = onPomodoroEnabledChange,
                onRequireAllChange = onRequireAllChange,
                onStepGoalChange = onStepGoalChange,
                onSleepGoalChange = onSleepGoalChange,
                onPomodoroGoalChange = onPomodoroGoalChange,
                modifier = Modifier.padding(top = 4.dp),
            )
            Column {
                // 잠근 앱이 있는 채로 집중 조건을 켜면 멈출 수 없는 세션이 곧바로
                // 시작됩니다. 스위치를 누르기 전에 알아야 하는 일이라 바로 아래에 둡니다.
                Text(
                    text = stringResource(R.string.settings_pomodoro_rule, Pomodoro.SESSION_MINUTES),
                    style = SlText.Caption,
                    color = SlColor.TextTertiary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }

            RelaxDelaySection(
                selected = settings.relaxDelay,
                onSelect = onRelaxDelayChange,
            )

            Column {
                SectionLabel(
                    text = stringResource(R.string.settings_section_account),
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                )
                AccountRow(
                    accountEmail = accountEmail,
                    accountName = accountName,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut,
                )
                // 되돌릴 수 없는 동작이라 계정 줄과 떼어 놓고 눈에 덜 띄게 둡니다.
                // 집중 중에는 숨깁니다 — 계정을 지우면 잠근 앱 목록까지 지워져서,
                // 멈출 수 없는 세션을 앱 안에서 끝내는 뒷문이 됩니다.
                if (accountEmail != null && focusing) {
                    Text(
                        text = stringResource(R.string.settings_account_delete_focusing),
                        style = SlText.Caption,
                        color = SlColor.TextTertiary,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    )
                } else if (accountEmail != null) {
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

            ThemeModeSection(selected = themeMode, onSelect = onThemeModeChange)

            Column {
                SectionLabel(
                    text = stringResource(R.string.settings_section_help),
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                )
                SlPanel {
                    SlDetailRow(
                        title = stringResource(R.string.settings_tutorial_replay),
                        description = stringResource(R.string.settings_tutorial_replay_desc),
                        modifier = Modifier
                            .clickable(role = Role.Button, onClick = onReplayTutorial)
                            .padding(vertical = 16.dp),
                        trailing = { SlChevron() },
                    )
                }
            }

            AdsSection()

            // "저장" 버튼을 두지 않습니다. 바꾸는 순간 저장되는데 버튼이 있으면,
            // 누르지 않고 나가면 취소된다고 읽혀서 괜히 한 번 더 누르게 되고,
            // 반대로 "저장했으니 바로 적용"이라고 오해하게 만듭니다(완화는 대기).
            Text(
                text = stringResource(R.string.settings_autosave_note),
                style = SlText.Caption,
                color = SlColor.TextTertiary,
            )
        }

    }

        BottomNavBar(
            selected = NavTab.Settings,
            onSelect = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
 * 화면 모드 고르기. 잠금 조건이 아니라 취향이라 바로 적용되고, 대기도 없습니다.
 *
 * "기기 설정 따르기"를 맨 앞(기본값)에 둡니다 — 대부분은 기기에서 이미 정해 두었고,
 * 앱만 따로 다르게 쓰고 싶은 사람이 나머지 둘을 고릅니다.
 */
@Composable
private fun ThemeModeSection(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.System to stringResource(R.string.settings_theme_system),
        ThemeMode.Light to stringResource(R.string.settings_theme_light),
        ThemeMode.Dark to stringResource(R.string.settings_theme_dark),
    )
    Column {
        SectionLabel(
            text = stringResource(R.string.settings_section_display),
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
        )
        SlPanel(contentPadding = PaddingValues(SlDimen.PanelPadding)) {
            Text(
                text = stringResource(R.string.settings_theme_title),
                style = SlText.RowTitle,
                color = SlColor.TextPrimary,
            )
            Text(
                text = stringResource(R.string.settings_theme_desc),
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
private fun AccountRow(
    accountEmail: String?,
    accountName: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    // 다른 섹션과 같은 표면을 씁니다. 계정 줄만 회색 판이면 눌러야 할 것처럼
    // 도드라지는데, 이 화면의 주인공은 잠금 조건입니다.
    SlPanel(
        contentPadding = PaddingValues(
            start = SlDimen.PanelPadding,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp,
        ),
    ) {
        SlDetailRow(
            // 로그인했으면 이름을 제목으로, 이메일을 그 아래에 — 누구 계정인지와 어느
            // 계정인지를 함께 보여 줍니다. 이름이 이메일 앞부분과 같으면 이메일만.
            title = when {
                accountEmail == null -> stringResource(R.string.settings_account_guest)
                accountName != null && accountName != accountEmail.substringBefore('@') -> accountName
                else -> accountEmail
            },
            description = when {
                accountEmail == null -> stringResource(R.string.settings_account_guest_desc)
                accountName != null && accountName != accountEmail.substringBefore('@') -> accountEmail
                else -> stringResource(R.string.settings_account_synced)
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

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun SettingsScreenPreview() {
    StepLockTheme {
        SettingsScreen(
            settings = SampleData.settings,
            settingsApplyOn = null,
            onRelaxDelayChange = {},
            blockedCount = SampleData.settings.blockedAppIds.size,
            onPickApps = {},
            onReplayTutorial = {},
            themeMode = ThemeMode.System,
            onThemeModeChange = {},
            accountEmail = "jiwoo@example.com",
            onSignIn = {},
            onSignOut = {},
            onTabSelected = {},
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
        )
    }
}

/** 다크 모드 — 같은 데이터로 색만 바뀌는지 봅니다. */
@Preview(widthDp = 412, heightDp = 892, name = "Dark")
@Composable
private fun SettingsScreenPreviewDark() {
    StepLockTheme(darkTheme = true) {
        SettingsScreen(
            settings = SampleData.settings,
            settingsApplyOn = null,
            onRelaxDelayChange = {},
            blockedCount = SampleData.settings.blockedAppIds.size,
            onPickApps = {},
            onReplayTutorial = {},
            themeMode = ThemeMode.System,
            onThemeModeChange = {},
            accountEmail = "jiwoo@example.com",
            onSignIn = {},
            onSignOut = {},
            onTabSelected = {},
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
            blockedCount = SampleData.settings.blockedAppIds.size,
            onPickApps = {},
            onReplayTutorial = {},
            themeMode = ThemeMode.System,
            onThemeModeChange = {},
            accountEmail = "jiwoo@example.com",
            onSignIn = {},
            onSignOut = {},
            onTabSelected = {},
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
        )
    }
}
