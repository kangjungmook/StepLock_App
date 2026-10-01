package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.data.InstalledApp
import com.steplock.app.data.Pomodoro
import com.steplock.app.data.ShortFormTarget
import com.steplock.app.ui.components.AppIcon
import com.steplock.app.ui.components.CheckboxMark
import com.steplock.app.ui.components.IconTapTarget
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlEmptyState
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SlConfirmDialog
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.components.SlSegmented
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.components.UnderlineTextField
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import java.time.LocalDate

/**
 * 기기에 깔린 앱에서 잠글 앱을 고릅니다.
 *
 * 전에는 쇼츠·릴스·틱톡·엑스 네 개만 목록에 있었고 패키지 이름이 코드에 박혀
 * 있었습니다. 그래서 **틱톡 라이트처럼 패키지가 다른 앱은 골라도 걸리지 않았습니다.**
 *
 * 고른 앱을 맨 위로 올립니다 — 수십 개짜리 목록에서 지금 뭘 골랐는지 확인하려고
 * 끝까지 내려야 하면 안 됩니다. 검색은 이름과 패키지 이름을 함께 봅니다.
 */
@Composable
fun AppPickerScreen(
    apps: List<InstalledApp>,
    selected: Set<String>,
    /**
     * **지금 실제로 막고 있는** 앱. 완화 대기가 걸려 있으면 [selected] 에서 빼도
     * 대기가 끝날 때까지 여기 남아 있습니다.
     */
    stillBlocked: Set<String>,
    /** 대기가 끝나 목록이 실제로 줄어드는 날. null 이면 대기가 없습니다. */
    applyOn: LocalDate?,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * 집중 세션이 도는 중. 이 동안에는 잠근 앱을 **뺄 수 없고** 더 잠그는 것만 됩니다 —
     * 빼면 세션은 돌아도 막는 게 없어서, 멈출 수 없게 한 의미가 사라집니다.
     */
    focusing: Boolean = false,
    /**
     * 앱을 잠그면 집중 세션이 곧바로 시작되는 상태(집중 조건을 켰고 오늘 목표가 남음).
     * 멈출 수 없는 25분이 시작되니 누르기 전에 미리 알려 줍니다.
     */
    focusStartsOnLock: Boolean = false,
    /** "쇼츠만" 막는 앱(정해 둔 값). */
    shortFormOnly: Set<String> = emptySet(),
    /** 쇼츠 화면을 알아보는 접근성 서비스가 켜져 있는지. */
    shortFormAccessOn: Boolean = false,
    onShortFormChange: (packageName: String, only: Boolean) -> Unit = { _, _ -> },
    /** 접근성 설정을 엽니다. 안내 대화상자에서 동의한 뒤에만 부릅니다. */
    onOpenShortFormAccess: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    // "쇼츠만"을 눌렀는데 접근성 권한이 없을 때 — 무엇을 읽는지 먼저 알리고 동의를 받습니다.
    var disclosureFor by remember { mutableStateOf<String?>(null) }

    disclosureFor?.let { packageName ->
        SlConfirmDialog(
            title = stringResource(R.string.short_form_disclosure_title),
            description = stringResource(R.string.short_form_disclosure_body),
            confirmText = stringResource(R.string.short_form_disclosure_confirm),
            cancelText = stringResource(R.string.short_form_disclosure_cancel),
            onConfirm = {
                onShortFormChange(packageName, true)
                onOpenShortFormAccess()
                disclosureFor = null
            },
            onDismiss = { disclosureFor = null },
            confirmContainerColor = SlColor.Brand,
            confirmContentColor = SlColor.OnBrand,
        )
    }

    // 체크를 풀었지만 아직 막혀 있는 앱. 이걸 알려 주지 않으면 사용자는 체크를
    // 풀었는데도 잠금이 떠서 앱이 고장 난 줄 압니다.
    val pending = remember(selected, stillBlocked) { stillBlocked - selected }

    val visible = remember(apps, selected, pending, query) {
        val keyword = query.trim()
        apps
            .filter {
                keyword.isEmpty() ||
                    it.label.contains(keyword, ignoreCase = true) ||
                    it.packageName.contains(keyword, ignoreCase = true)
            }
            // 고른 앱과 해제를 기다리는 앱을 맨 위로, 그다음 이름 순.
            .sortedWith(
                compareByDescending<InstalledApp> {
                    it.packageName in selected || it.packageName in pending
                }.thenBy { it.label },
            )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background)
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                text = stringResource(R.string.app_picker_title),
                style = SlText.ScreenTitle,
                color = SlColor.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.app_picker_selected, selected.size),
                style = SlText.LabelSm,
                color = if (selected.isEmpty()) SlColor.TextSecondary else SlColor.BrandDeep,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Column(modifier = Modifier.padding(horizontal = SlDimen.ScreenPadding)) {
            UnderlineTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.app_picker_search_label),
                placeholder = stringResource(R.string.app_picker_search_hint),
                leadingIcon = SlIcons.Search,
                imeAction = ImeAction.Search,
            )
            Spacer(Modifier.height(20.dp))
        }

        if (visible.isEmpty()) {
            Column(modifier = Modifier.padding(horizontal = SlDimen.ScreenPadding)) {
                SlPanel {
                    SlEmptyState(
                        title = stringResource(
                            if (apps.isEmpty()) {
                                R.string.app_picker_none_title
                            } else {
                                R.string.app_picker_no_match_title
                            },
                        ),
                        description = stringResource(
                            if (apps.isEmpty()) {
                                R.string.app_picker_none_desc
                            } else {
                                R.string.app_picker_no_match_desc
                            },
                        ),
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = SlDimen.ScreenPadding,
                end = SlDimen.ScreenPadding,
                bottom = 24.dp,
            ),
        ) {
            if (focusing) {
                item {
                    FocusNotice(
                        title = stringResource(R.string.app_picker_focus_title),
                        description = stringResource(R.string.app_picker_focus_desc),
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            } else if (focusStartsOnLock) {
                item {
                    FocusNotice(
                        title = stringResource(
                            R.string.app_picker_focus_start_title,
                            Pomodoro.SESSION_MINUTES,
                        ),
                        description = stringResource(R.string.app_picker_focus_start_desc),
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            }
            // 체크를 풀었는데도 아직 막히는 앱이 있으면 목록보다 먼저 알립니다.
            if (pending.isNotEmpty() && applyOn != null) {
                item {
                    PendingUnlockNotice(
                        count = pending.size,
                        applyOn = applyOn,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            }
            item {
                SectionLabel(
                    text = stringResource(R.string.app_picker_hint),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(visible, key = { it.packageName }) { app ->
                val checked = app.packageName in selected
                AppPickerRow(
                    app = app,
                    checked = checked,
                    stillBlocked = app.packageName in pending,
                    // 집중 중에는 체크를 풀 수 없습니다. 더 잠그는 건 됩니다.
                    locked = focusing && checked,
                    onToggle = { onToggle(app.packageName) },
                )
                val target = ShortFormTarget.of(app.packageName)
                if (checked && target != null) {
                    ShortFormChoice(
                        target = target,
                        only = app.packageName in shortFormOnly,
                        accessOn = shortFormAccessOn,
                        // 집중 중에는 "쇼츠만"으로 느슨하게 바꿀 수 없습니다.
                        canLoosen = !focusing,
                        onChange = { only ->
                            if (only && !shortFormAccessOn) {
                                disclosureFor = app.packageName
                            } else {
                                onShortFormChange(app.packageName, only)
                            }
                        },
                        onOpenAccess = { disclosureFor = app.packageName },
                    )
                }
                SlDivider()
            }
        }
    }
}

/** 앱 한 줄. 아이콘 없이는 "라이트" 같은 변종을 구분할 수 없어 아이콘을 함께 씁니다. */
@Composable
private fun AppPickerRow(
    app: InstalledApp,
    checked: Boolean,
    /** 체크는 풀렸지만 완화 대기 때문에 아직 막고 있는 앱. */
    stillBlocked: Boolean,
    /** 집중 중이라 체크를 풀 수 없는 앱. */
    locked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = !locked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.packageName, label = app.label, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.label, style = SlText.ListItem, color = SlColor.TextPrimary)
            Text(
                // 아직 막고 있다는 사실이 패키지 이름보다 중요합니다.
                text = when {
                    locked -> stringResource(R.string.app_picker_focus_locked)
                    stillBlocked -> stringResource(R.string.app_picker_still_blocked)
                    else -> app.packageName
                },
                style = SlText.LabelSm,
                color = when {
                    locked -> SlColor.BrandDeep
                    stillBlocked -> SlColor.AmberText
                    else -> SlColor.TextTertiary
                },
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        CheckboxMark(checked = checked)
    }
}

/**
 * 유튜브·인스타그램처럼 짧은 영상 화면을 알아볼 수 있는 앱에서만 보이는 선택.
 * 앱 줄 바로 아래, 아이콘 열에 맞춰 들여 둡니다.
 */
@Composable
private fun ShortFormChoice(
    target: ShortFormTarget,
    only: Boolean,
    accessOn: Boolean,
    canLoosen: Boolean,
    onChange: (Boolean) -> Unit,
    onOpenAccess: () -> Unit,
) {
    val onlyLabel = stringResource(target.onlyRes)
    Column(modifier = Modifier.padding(start = 54.dp, bottom = 12.dp)) {
        SlSegmented(
            options = listOf(stringResource(R.string.short_form_whole), onlyLabel),
            selectedIndex = if (only) 1 else 0,
            onSelect = { index ->
                val wantOnly = index == 1
                if (wantOnly != only && (!wantOnly || canLoosen)) onChange(wantOnly)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (only) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (accessOn) {
                        stringResource(R.string.short_form_on_note, stringResource(target.labelRes))
                    } else {
                        stringResource(R.string.short_form_off_note)
                    },
                    style = SlText.LabelSm,
                    color = if (accessOn) SlColor.TextSecondary else SlColor.Error,
                    modifier = Modifier.weight(1f),
                )
                if (!accessOn) {
                    TextLink(
                        text = stringResource(R.string.short_form_open_settings),
                        onClick = onOpenAccess,
                        style = SlText.Label,
                        color = SlColor.BrandInk,
                        underline = false,
                    )
                }
            }
        }
    }
}

/**
 * 집중 세션에 관한 안내. 경고가 아니라 규칙 설명이라 브랜드 틴트를 씁니다 —
 * 해제 대기(앰버)와 색이 달라야 둘이 함께 떠도 구분됩니다.
 */
@Composable
private fun FocusNotice(title: String, description: String, modifier: Modifier = Modifier) {
    SlPanel(
        modifier = modifier,
        containerColor = SlColor.BrandTintAlt,
        borderColor = SlColor.BrandTintAlt,
        contentPadding = PaddingValues(SlDimen.PanelPadding),
    ) {
        Text(text = title, style = SlText.RowTitle, color = SlColor.BrandDeep)
        Text(
            text = description,
            style = SlText.RowValue,
            color = SlColor.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 해제를 기다리는 앱이 있다는 알림. 설정 화면의 예약 알림과 같은 모양을 씁니다. */
@Composable
private fun PendingUnlockNotice(count: Int, applyOn: LocalDate, modifier: Modifier = Modifier) {
    val whenText = if (applyOn == LocalDate.now().plusDays(1)) {
        stringResource(R.string.settings_pending_tomorrow)
    } else {
        stringResource(R.string.settings_pending_date, applyOn.monthValue, applyOn.dayOfMonth)
    }

    SlPanel(
        modifier = modifier,
        containerColor = SlColor.AmberSurface,
        borderColor = SlColor.AmberBorder,
        contentPadding = PaddingValues(SlDimen.PanelPadding),
    ) {
        Text(
            text = stringResource(R.string.app_picker_pending_title, count, whenText),
            style = SlText.RowTitle,
            color = SlColor.AmberText,
        )
        Text(
            text = stringResource(R.string.app_picker_pending_desc),
            style = SlText.RowValue,
            color = SlColor.AmberSubText,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun AppPickerScreenPreview() {
    StepLockTheme {
        AppPickerScreen(
            apps = listOf(
                InstalledApp("com.ss.android.ugc.tiktok.lite", "TikTok Lite"),
                InstalledApp("com.google.android.youtube", "YouTube"),
                InstalledApp("com.instagram.android", "Instagram"),
                InstalledApp("com.kakao.talk", "카카오톡"),
                InstalledApp("com.nhn.android.search", "네이버"),
            ),
            selected = setOf("com.ss.android.ugc.tiktok.lite", "com.google.android.youtube"),
            stillBlocked = setOf("com.ss.android.ugc.tiktok.lite", "com.google.android.youtube"),
            applyOn = null,
            onToggle = {},
            onBack = {},
        )
    }
}

/**
 * 인스타그램의 체크를 풀었지만 완화 대기가 걸려 아직 막혀 있는 상태.
 * 알림과 줄 표시가 같이 보여야 "체크를 풀었는데 왜 잠기지" 가 생기지 않습니다.
 */
@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun AppPickerScreenPendingPreview() {
    StepLockTheme {
        AppPickerScreen(
            apps = listOf(
                InstalledApp("com.ss.android.ugc.tiktok.lite", "TikTok Lite"),
                InstalledApp("com.google.android.youtube", "YouTube"),
                InstalledApp("com.instagram.android", "Instagram"),
                InstalledApp("com.kakao.talk", "카카오톡"),
            ),
            selected = setOf("com.ss.android.ugc.tiktok.lite", "com.google.android.youtube"),
            stillBlocked = setOf(
                "com.ss.android.ugc.tiktok.lite",
                "com.google.android.youtube",
                "com.instagram.android",
            ),
            applyOn = LocalDate.now().plusDays(3),
            onToggle = {},
            onBack = {},
        )
    }
}
