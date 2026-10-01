package com.steplock.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.data.LockSettings
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.util.UnlockCondition
import com.steplock.app.ui.util.enabledConditions
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.sleepGoalLabel

/**
 * 잠금 규칙을 **한 문장**으로 보여 주고, 문장 속 밑줄 친 값을 눌러 바꿉니다.
 *
 * > 하루에 **8,000보** 걷거나 **7시간** 자거나 **집중 3번**을 하면 **앱 3개**를 열어 줄게요.
 *
 * 스위치와 스테퍼가 줄줄이 늘어선 설정 표 대신, 규칙 전체를 사람의 말로 한 번에 읽게
 * 했습니다. 표에서는 "조건 셋 + 전부 만족 스위치 + 앱 목록"을 머릿속에서 합쳐야 규칙이
 * 되지만, 문장은 그 자체가 규칙입니다. 값(밑줄)을 누르면 아래에서 시트가 올라와
 * 그 값 하나만 고칩니다. 꺼 둔 조건은 문장에서 빠지고 "＋ 조건 추가"로 남습니다.
 *
 * "하나만 / 모두"는 연결어로 드러납니다 — 하나만이면 "걷거나 … 자거나", 모두면
 * "걷고 … 자고".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditor(
    settings: LockSettings,
    blockedCount: Int,
    onPickApps: () -> Unit,
    onStepsEnabledChange: (Boolean) -> Unit,
    onSleepEnabledChange: (Boolean) -> Unit,
    onPomodoroEnabledChange: (Boolean) -> Unit,
    onRequireAllChange: (Boolean) -> Unit,
    onStepGoalChange: (Int) -> Unit,
    onSleepGoalChange: (Float) -> Unit,
    onPomodoroGoalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf<UnlockCondition?>(null) }
    val enabled = enabledConditions(settings)
    val all = settings.requireAllConditions

    val tokenStyle = TextLinkStyles(
        style = SpanStyle(color = SlColor.BrandInk, textDecoration = TextDecoration.Underline),
    )
    val steps = stringResource(R.string.rule_steps_token, settings.stepGoal.formatThousands())
    val sleep = sleepGoalLabel(settings.sleepGoalHours)
    val pomodoro = stringResource(R.string.rule_pomodoro_token, settings.pomodoroGoal)
    val apps = stringResource(R.string.rule_apps_token, blockedCount)
    val prefix = stringResource(R.string.rule_prefix)
    val ending = stringResource(R.string.rule_ending)

    Column(modifier = modifier) {
        if (enabled.isEmpty()) {
            Text(
                text = stringResource(R.string.rule_none),
                style = SlText.RuleSentence,
                color = SlColor.TextPrimary,
            )
        } else {
            Text(
                text = buildAnnotatedString {
                    append(prefix)
                    enabled.forEachIndexed { index, condition ->
                        val last = index == enabled.lastIndex
                        withLink(
                            LinkAnnotation.Clickable(tag = condition.name, styles = tokenStyle) {
                                editing = condition
                            },
                        ) {
                            append(
                                when (condition) {
                                    UnlockCondition.Steps -> steps
                                    UnlockCondition.Sleep -> sleep
                                    UnlockCondition.Pomodoro -> pomodoro
                                },
                            )
                        }
                        append(verb(condition, last = last, all = all))
                        append(if (last) " " else "\n")
                    }
                    withLink(LinkAnnotation.Clickable(tag = "apps", styles = tokenStyle) { onPickApps() }) {
                        append(apps)
                    }
                    withStyle(SpanStyle()) { append(ending) }
                },
                style = SlText.RuleSentence,
                color = SlColor.TextPrimary,
            )
        }

        // 조건이 둘 이상일 때만 "하나만 / 모두"가 의미가 있습니다.
        if (enabled.size >= 2) {
            Spacer(Modifier.height(20.dp))
            SlSegmented(
                options = listOf(
                    stringResource(R.string.rule_mode_any),
                    stringResource(R.string.rule_mode_all),
                ),
                selectedIndex = if (all) 1 else 0,
                onSelect = { onRequireAllChange(it == 1) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.rule_hint),
            style = SlText.Caption,
            color = SlColor.TextTertiary,
        )

        // 꺼 둔 조건 — 누르면 같은 시트가 열려 켤 수 있습니다.
        val off = UnlockCondition.entries.filter { it !in enabled }
        if (off.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                off.forEach { condition ->
                    val shape = RoundedCornerShape(SlDimen.RadiusCard)
                    Row(
                        modifier = Modifier
                            .clip(shape)
                            .border(1.dp, SlColor.BorderStrong, shape)
                            .clickable(role = Role.Button) { editing = condition }
                            .heightIn(min = SlDimen.TouchTarget)
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = SlIcons.Plus,
                            contentDescription = null,
                            tint = SlColor.TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.rule_add, stringResource(condition.titleRes)),
                            style = SlText.Label,
                            color = SlColor.TextPrimary,
                        )
                    }
                }
            }
        }
    }

    val current = editing
    if (current != null) {
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SlColor.Surface,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        ) {
            ConditionSheet(
                condition = current,
                settings = settings,
                onEnabledChange = when (current) {
                    UnlockCondition.Steps -> onStepsEnabledChange
                    UnlockCondition.Sleep -> onSleepEnabledChange
                    UnlockCondition.Pomodoro -> onPomodoroEnabledChange
                },
                onDecrease = when (current) {
                    UnlockCondition.Steps -> { { onStepGoalChange(-500) } }
                    UnlockCondition.Sleep -> { { onSleepGoalChange(-0.5f) } }
                    UnlockCondition.Pomodoro -> { { onPomodoroGoalChange(-1) } }
                },
                onIncrease = when (current) {
                    UnlockCondition.Steps -> { { onStepGoalChange(500) } }
                    UnlockCondition.Sleep -> { { onSleepGoalChange(0.5f) } }
                    UnlockCondition.Pomodoro -> { { onPomodoroGoalChange(1) } }
                },
                valueText = when (current) {
                    UnlockCondition.Steps -> steps
                    UnlockCondition.Sleep -> sleep
                    UnlockCondition.Pomodoro -> pomodoro
                },
                onDone = { editing = null },
            )
        }
    }
}

/** 조건 뒤에 붙는 말 — 하나만이면 "~거나", 모두면 "~고", 마지막은 "~면". */
@Composable
private fun verb(condition: UnlockCondition, last: Boolean, all: Boolean): String = stringResource(
    when (condition) {
        UnlockCondition.Steps -> when {
            last -> R.string.rule_steps_last
            all -> R.string.rule_steps_and
            else -> R.string.rule_steps_or
        }
        UnlockCondition.Sleep -> when {
            last -> R.string.rule_sleep_last
            all -> R.string.rule_sleep_and
            else -> R.string.rule_sleep_or
        }
        UnlockCondition.Pomodoro -> when {
            last -> R.string.rule_pomodoro_last
            all -> R.string.rule_pomodoro_and
            else -> R.string.rule_pomodoro_or
        }
    },
)

/** 값 하나를 고치는 시트 — 쓰기 스위치, 큰 숫자와 −/+, 적용 시점 안내. */
@Composable
private fun ConditionSheet(
    condition: UnlockCondition,
    settings: LockSettings,
    onEnabledChange: (Boolean) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    valueText: String,
    onDone: () -> Unit,
) {
    val enabled = when (condition) {
        UnlockCondition.Steps -> settings.stepsEnabled
        UnlockCondition.Sleep -> settings.sleepEnabled
        UnlockCondition.Pomodoro -> settings.pomodoroEnabled
    }
    val title = stringResource(
        when (condition) {
            UnlockCondition.Steps -> R.string.rule_sheet_steps
            UnlockCondition.Sleep -> R.string.rule_sheet_sleep
            UnlockCondition.Pomodoro -> R.string.rule_sheet_pomodoro
        },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = SlDimen.ScreenPadding, end = SlDimen.ScreenPadding, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = SlText.SectionTitle,
                color = SlColor.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            SlSwitch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                contentDescription = stringResource(R.string.rule_sheet_on),
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetStepButton(
                icon = SlIcons.Minus,
                label = stringResource(R.string.settings_goal_decrease, title),
                onClick = onDecrease,
                enabled = enabled,
            )
            Text(
                text = valueText,
                style = SlText.HomeNumeral.copy(fontSize = 56.sp(), lineHeight = 60.sp()),
                color = if (enabled) SlColor.TextPrimary else SlColor.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            SheetStepButton(
                icon = SlIcons.Plus,
                label = stringResource(R.string.settings_goal_increase, title),
                onClick = onIncrease,
                enabled = enabled,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.rule_sheet_note),
            style = SlText.Caption,
            color = SlColor.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = stringResource(R.string.rule_sheet_close), onClick = onDone)
    }
}

@Composable
private fun SheetStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val shape = RoundedCornerShape(SlDimen.RadiusCard)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .border(1.dp, SlColor.Border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) SlColor.TextPrimary else SlColor.TextTertiary,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
