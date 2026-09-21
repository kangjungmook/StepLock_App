package com.steplock.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = SlText.SectionLabel,
        color = SlColor.TextSecondary,
        modifier = modifier,
    )
}

/** 시안의 섹션 패널 — 카드를 겹치지 않고 하나의 면 안에서 행을 구분합니다. */
@Composable
fun SlPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SlDimen.RadiusCard),
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SlDimen.PanelPadding,
        vertical = 4.dp,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SlColor.Surface)
            .border(1.dp, SlColor.Border, shape)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun SlDivider(modifier: Modifier = Modifier, color: Color = SlColor.Border) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = SlDimen.CtaHeight,
    shape: Shape = RoundedCornerShape(SlDimen.RadiusCta),
    containerColor: Color = SlColor.Brand,
    contentColor: Color = SlColor.OnBrand,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = SlColor.TrackOff,
            disabledContentColor = SlColor.TextSecondary,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(text = text, style = SlText.Cta)
    }
}

/**
 * "눌러서 더 볼 수 있다"를 나타내는 셰브론. 목록 줄·빈 상태·경고 줄에서
 * 같은 크기·같은 색으로 써야 눌리는 줄과 아닌 줄이 구분됩니다.
 */
@Composable
fun SlChevron(modifier: Modifier = Modifier, tint: Color = SlColor.TextTertiary) {
    Icon(
        imageVector = SlIcons.ChevronRight,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(18.dp),
    )
}

/** 텍스트 링크 — 44dp 터치 영역을 확보합니다. */
@Composable
fun TextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = SlText.Link,
    color: Color = SlColor.TextSecondary,
    underline: Boolean = true,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = SlDimen.TouchTarget)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            textAlign = TextAlign.Center,
            textDecoration = if (underline) TextDecoration.Underline else null,
        )
    }
}

@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    background: Color,
    size: Dp,
    shape: Shape,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** 아이콘만 있는 버튼 — 터치 영역 44dp, 원형 리플. */
@Composable
fun IconTapTarget(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SlColor.TextPrimary,
    iconSize: Dp = 22.dp,
    background: Color = Color.Transparent,
) {
    Box(
        modifier = modifier
            .size(SlDimen.TouchTarget)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** 앱 식별 뱃지 — 이니셜 한 글자와 서비스 색. */
@Composable
fun AppBadge(initial: String, color: Color, size: Dp, modifier: Modifier = Modifier) {
    val large = size >= 40.dp
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(if (large) SlDimen.RadiusBadge else SlDimen.RadiusBadgeSmall))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = if (large) SlText.BadgeInitial else SlText.BadgeInitialSmall,
            color = SlColor.OnAmber,
        )
    }
}

@Composable
fun SlSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) SlDimen.SwitchWidth - SlDimen.SwitchKnob - 6.dp else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "switchKnob",
    )
    Box(
        modifier = modifier
            .size(width = SlDimen.SwitchWidth, height = SlDimen.TouchTarget)
            .semantics { this.contentDescription = contentDescription }
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(width = SlDimen.SwitchWidth, height = SlDimen.SwitchHeight)
                .clip(CircleShape)
                .background(if (checked) SlColor.Brand else SlColor.TrackOff)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(SlDimen.SwitchKnob)
                    .shadow(
                        elevation = 2.dp,
                        shape = CircleShape,
                        ambientColor = SlColor.Shadow,
                        spotColor = SlColor.Shadow,
                    )
                    .clip(CircleShape)
                    .background(SlColor.Surface),
            )
        }
    }
}

/** 체크 상태만 그리는 체크박스 — 클릭은 감싸는 행이 받습니다. */
@Composable
fun CheckboxMark(
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    cornerRadius: Dp = SlDimen.RadiusCheckbox,
    borderWidth: Dp = 2.dp,
    checkIcon: ImageVector = SlIcons.CheckBold,
    checkSize: Dp = 14.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (checked) SlColor.Brand else Color.Transparent)
            .border(borderWidth, if (checked) SlColor.Brand else SlColor.BorderStrong, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = checkIcon,
                contentDescription = null,
                tint = SlColor.OnBrand,
                modifier = Modifier.size(checkSize),
            )
        }
    }
}

/** Steplock 워드마크 — 첫 글자만 브랜드 그린. */
@Composable
fun Wordmark(text: String, modifier: Modifier = Modifier, style: TextStyle = SlText.Wordmark) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = SlColor.Brand)) { append(text.take(1)) }
            append(text.drop(1))
        },
        style = style,
        color = SlColor.TextPrimary,
        modifier = modifier,
    )
}
