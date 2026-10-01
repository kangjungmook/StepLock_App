package com.steplock.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 간격은 4dp 그리드를 따릅니다.
 *
 * 모서리는 **8dp 하나**로 맞춥니다 — 표지판 판처럼 반듯한 면. 예외는 둘뿐이고 규칙이
 * 있습니다: 떠 있는 하단 탭과 상태 칩·스위치는 알약(완전 둥근 모서리)입니다.
 */
object SlDimen {
    val TouchTarget = 44.dp
    val CtaHeight = 56.dp
    val SwitchWidth = 52.dp
    val SwitchHeight = 32.dp
    val SwitchKnob = 26.dp

    val RadiusField = 8.dp
    val RadiusCard = 8.dp
    val RadiusPanel = 8.dp
    val RadiusCta = 8.dp
    val RadiusBadge = 8.dp
    val RadiusBadgeSmall = 6.dp
    /** 작은 요소와 누르는 영역의 리플 모서리 (체크박스, 텍스트 링크). */
    val RadiusSmall = 6.dp

    /** 하단 탭이나 목록이 있는 화면의 좌우 여백. */
    val ScreenPadding = 20.dp

    /**
     * 하단 탭 없이 내용만 있는 화면(로그인·온보딩·잠금)의 좌우 여백.
     * 가운데 정렬된 내용에 숨 쉴 자리를 더 줍니다.
     */
    val ScreenPaddingWide = 24.dp

    val PanelPadding = 16.dp

    /**
     * 떠 있는 하단 탭에 가리지 않도록 스크롤 끝에 두는 여백 —
     * 탭 높이 64 + 아래 띄움 12 + 숨 쉴 자리 16.
     */
    val FloatingNavReserve = 92.dp
}
