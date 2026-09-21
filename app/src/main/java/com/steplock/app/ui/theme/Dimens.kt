package com.steplock.app.ui.theme

import androidx.compose.ui.unit.dp

/** 간격은 4dp 그리드를 따르고, 라운드는 14~20dp 범위를 씁니다. */
object SlDimen {
    val TouchTarget = 44.dp
    val CtaHeight = 56.dp
    val SwitchWidth = 52.dp
    val SwitchHeight = 32.dp
    val SwitchKnob = 26.dp

    val RadiusField = 14.dp
    val RadiusCard = 18.dp
    val RadiusPanel = 20.dp
    val RadiusCta = 16.dp
    val RadiusBadge = 13.dp
    val RadiusBadgeSmall = 12.dp
    val RadiusCheckbox = 8.dp

    /** 하단 탭이나 목록이 있는 화면의 좌우 여백. */
    val ScreenPadding = 20.dp

    /**
     * 하단 탭 없이 내용만 있는 화면(로그인·온보딩·잠금)의 좌우 여백.
     * 가운데 정렬된 내용에 숨 쉴 자리를 더 줍니다.
     */
    val ScreenPaddingWide = 24.dp

    val PanelPadding = 16.dp
}
