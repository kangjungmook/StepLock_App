package com.steplock.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 시안은 OKLCH로 정의되어 있으므로 sRGB 변환값과 원본 토큰을 함께 남겨 둡니다.
 */
object SlColor {
    val Background = Color(0xFFF5F2E9) // oklch(96% 0.012 90)
    val Surface = Color(0xFFFDFCF7) // oklch(99% 0.006 90)
    val SurfaceAlt = Color(0xFFEDE8D9) // oklch(93% 0.02 90)
    val Border = Color(0xFFDBD7CD) // oklch(88% 0.015 90)
    val BorderStrong = Color(0xFFC1BDB3) // oklch(80% 0.015 90)
    val TrackOff = Color(0xFFD1CDC3) // oklch(85% 0.015 90)

    val TextPrimary = Color(0xFF171D26) // oklch(23% 0.02 260)
    val TextSecondary = Color(0xFF5D646F) // oklch(50% 0.02 260)
    val TextTertiary = Color(0xFF737B87) // oklch(58% 0.02 260)

    val Brand = Color(0xFF009267) // oklch(58% 0.13 165)
    val BrandInk = Color(0xFF006944) // oklch(45% 0.12 165)
    val BrandDeep = Color(0xFF005A38) // oklch(40% 0.115 165)
    val OnBrand = Color(0xFFF6FEFA) // oklch(99% 0.01 165)
    val BrandTint = Color(0xFFC7F0DC) // oklch(92% 0.05 165)
    val BrandTintAlt = Color(0xFFCDF2E0) // oklch(93% 0.045 165)

    val Amber = Color(0xFFE48233) // oklch(70% 0.15 55)
    val AmberSurface = Color(0xFFFFE7C3) // oklch(94% 0.055 75)
    val AmberBorder = Color(0xFFF8CB9C) // oklch(87% 0.08 68)
    val AmberText = Color(0xFF492B0F) // oklch(32% 0.06 60)
    val AmberSubText = Color(0xFF6A4F38) // oklch(45% 0.05 62)
    val OnAmber = Color(0xFFFEFCF4) // oklch(99% 0.01 90)

    val Error = Color(0xFFC53637) // oklch(55% 0.18 25)
    // 계정 삭제처럼 되돌릴 수 없는 버튼의 글자색 (Error 대비 5.2:1)
    val OnError = Color(0xFFFFF9F8) // oklch(99% 0.01 25)

    val Shadow = Color(0xFF414853) // oklch(40% 0.02 260)

    /** 앱 뱃지는 식별을 위해 각 서비스 색을 유지합니다. */
    object Social {
        val GoogleBlue = Color(0xFF4285F4)
        val GoogleGreen = Color(0xFF34A853)
        val GoogleYellow = Color(0xFFFBBC05)
        val GoogleRed = Color(0xFFEA4335)
        val Kakao = Color(0xFFF6D653) // oklch(88% 0.15 95)
        val KakaoInk = Color(0xFF362512) // oklch(28% 0.04 70)
        val AppleInk = Color(0xFFFAF8F5) // oklch(98% 0.005 90)
    }

    /** 다크 팔레트는 잠금 오버레이 전용입니다. 순수 블랙은 사용하지 않습니다. */
    object Dark {
        val Background = Color(0xFF0F141D) // oklch(19% 0.02 260)
        val Surface = Color(0xFF1C222B) // oklch(25% 0.02 260)
        val SurfaceAlt = Color(0xFF212730) // oklch(27% 0.02 260)
        val Border = Color(0xFF2F3640) // oklch(33% 0.02 260)

        val TextPrimary = Color(0xFFF4F2EA) // oklch(96% 0.01 90)
        val TextBright = Color(0xFFEEEBE4) // oklch(94% 0.01 90)
        val TextChip = Color(0xFFD6DFEC) // oklch(90% 0.02 260)
        val TextIcon = Color(0xFFB6BECB) // oklch(80% 0.02 260)
        val TextMuted = Color(0xFF9DA5B1) // oklch(72% 0.02 260)
        val TextLink = Color(0xFF9199A5) // oklch(68% 0.02 260)

        val AmberTint = Color(0xFF46280C) // oklch(31% 0.06 60)
        val AmberIcon = Color(0xFFF79643) // oklch(76% 0.15 58)
        val AmberText = Color(0xFFFBA962) // oklch(80% 0.13 60)
        // 잠금 화면에서 가장 큰 면적을 차지하는 링 — 채도를 낮춰 눈이 덜 피로합니다.
        val AmberRing = Color(0xFFE59656) // oklch(74% 0.125 58)

        val GreenTint = Color(0xFF0A3123) // oklch(28% 0.05 165)
        val GreenBorder = Color(0xFF134E39) // oklch(38% 0.07 165)
        val GreenIcon = Color(0xFF3CC998) // oklch(75% 0.14 165)
        val GreenText = Color(0xFF94E1BF) // oklch(85% 0.09 165)
        // 어두운 배경 위 캐릭터의 그림자 면 — 밝은 GreenIcon 과 짝을 이룹니다.
        val GreenDeep = Color(0xFF0D8963) // oklch(56% 0.115 165)
    }
}
