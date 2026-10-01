package com.steplock.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steplock.app.R
import com.steplock.app.data.TemporaryAllow
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.components.TrailMap
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import kotlinx.coroutines.launch

/** 튜토리얼 한 장. 한 장에 한 가지만 말합니다 — 글이 길면 넘기기만 하고 읽지 않습니다. */
private data class TutorialPage(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val mood: MascotMood,
    /** 본문 아래에 늘어놓을 조건 칩. 조건을 설명하는 장에서만 씁니다. */
    val showConditions: Boolean = false,
)

private val pages = listOf(
    TutorialPage(R.string.tutorial_hello_title, R.string.tutorial_hello_body, MascotMood.Walking),
    TutorialPage(
        R.string.tutorial_unlock_title,
        R.string.tutorial_unlock_body,
        MascotMood.Walking,
        showConditions = true,
    ),
    TutorialPage(R.string.tutorial_allow_title, R.string.tutorial_allow_body, MascotMood.Resting),
    TutorialPage(R.string.tutorial_ready_title, R.string.tutorial_ready_body, MascotMood.Focusing),
)

/**
 * 첫 실행 튜토리얼 — **넘길 때마다 스텝이가 등산로를 한 구간씩 오릅니다.**
 *
 * 위는 홈과 같은 등산로, 아래는 한 장의 글. 네 장을 다 넘기면 스텝이가 정상의
 * 자물쇠에 닿고 자물쇠가 주황으로 바뀝니다 — "채우면 열린다"를 글보다 먼저
 * 그림으로 한 번 겪게 합니다. 말풍선 + 가운데 캐릭터 구조를 버렸습니다.
 *
 * 권한 화면보다 **먼저** 둡니다. 무엇을 하는 앱인지 모르는 채로 "사용 정보 접근"
 * 같은 권한을 요구받으면 거절하기 쉽고, 거절하면 앱이 아무것도 못 합니다.
 *
 * @param replay 설정에서 다시 보는 경우. 마지막 버튼이 권한 화면 대신 닫기가 됩니다.
 */
@Composable
fun TutorialScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    replay: Boolean = false,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex
    val page = pages[pagerState.currentPage]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background)
            .safeDrawingPadding(),
    ) {
        // 몇 번째 장인지(왼쪽)와 건너뛰기(오른쪽). 건너뛰기는 마지막 장에서 사라집니다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SlDimen.TouchTarget)
                .padding(start = SlDimen.ScreenPadding, end = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PageMarks(
                count = pages.size,
                current = pagerState.currentPage,
                modifier = Modifier.weight(1f),
            )
            if (!isLast) {
                TextLink(
                    text = stringResource(R.string.tutorial_skip),
                    onClick = onFinish,
                    style = SlText.LinkSm,
                    color = SlColor.TextSecondary,
                    underline = false,
                )
            }
        }

        // 길 — 장마다 한 구간. 그림이라 읽는 도구에는 숨깁니다.
        TrailMap(
            progress = (pagerState.currentPage + 1f) / pages.size,
            mood = page.mood,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = SlDimen.ScreenPadding)
                .clearAndSetSemantics {},
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) { index ->
            TutorialPageText(page = pages[index])
        }

        PrimaryButton(
            text = stringResource(
                when {
                    !isLast -> R.string.tutorial_next
                    replay -> R.string.tutorial_done
                    else -> R.string.tutorial_start
                },
            ),
            onClick = {
                if (isLast) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.padding(
                start = SlDimen.ScreenPadding,
                end = SlDimen.ScreenPadding,
                top = 24.dp,
                bottom = 24.dp,
            ),
        )
    }
}

/** 한 장의 글 — 왼쪽 정렬 큰 제목과 본문. 최소 높이를 맞춰 장마다 버튼이 튀지 않습니다. */
@Composable
private fun TutorialPageText(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TEXT_HEIGHT)
            .padding(horizontal = SlDimen.ScreenPadding),
    ) {
        Text(
            text = stringResource(page.titleRes),
            style = SlText.HomeBig.copy(fontSize = 30.sp, lineHeight = 38.sp),
            color = SlColor.TextPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(page.bodyRes, TemporaryAllow.MINUTES, TemporaryAllow.DAILY_LIMIT),
            style = SlText.Tagline,
            color = SlColor.TextSecondary,
        )
        if (page.showConditions) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConditionChip(SlIcons.Steps, stringResource(R.string.condition_steps))
                ConditionChip(SlIcons.Moon, stringResource(R.string.condition_sleep))
                ConditionChip(SlIcons.Timer, stringResource(R.string.condition_pomodoro))
            }
        }
    }
}

private val TEXT_HEIGHT = 216.dp

@Composable
private fun ConditionChip(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(CircleShape)
            .border(1.dp, SlColor.Border, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SlColor.TextPrimary,
            modifier = Modifier.size(14.dp),
        )
        Text(text = label, style = SlText.Chip, color = SlColor.TextPrimary)
    }
}

/**
 * 지금 몇 번째 장인지 — 등산로 표식과 같은 납작한 칸. 지난 장 · 지금 장은 칠하고
 * 남은 장은 흐리게, 옆에 "1 / 4".
 */
@Composable
private fun PageMarks(count: Int, current: Int, modifier: Modifier = Modifier) {
    val position = stringResource(R.string.tutorial_position, current + 1, count)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = position },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            index == current -> SlColor.Brand
                            index < current -> SlColor.TextPrimary
                            else -> SlColor.TrackOff
                        },
                    ),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${current + 1} / $count",
            style = SlText.TrailTick.copy(fontSize = 15.sp, color = SlColor.TextSecondary),
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TutorialScreenPreview() {
    StepLockTheme {
        TutorialScreen(onFinish = {})
    }
}

/** 다크 모드 — 같은 데이터로 색만 바뀌는지 봅니다. */
@Preview(widthDp = 412, heightDp = 892, name = "Dark")
@Composable
private fun TutorialScreenPreviewDark() {
    StepLockTheme(darkTheme = true) {
        TutorialScreen(onFinish = {})
    }
}
