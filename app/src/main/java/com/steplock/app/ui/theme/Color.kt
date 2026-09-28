package com.steplock.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 화면이 쓰는 색 한 벌. 라이트와 다크가 같은 이름을 갖고, [StepLockTheme] 이
 * 시스템 설정에 맞는 쪽을 [LocalSlPalette] 로 내려 줍니다.
 *
 * 시안은 OKLCH 로 정의되어 있어 sRGB 변환값 옆에 원본 토큰을 남깁니다.
 */
@Immutable
class SlPalette(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val borderStrong: Color,
    val trackOff: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val brand: Color,
    val brandInk: Color,
    val brandDeep: Color,
    val onBrand: Color,
    val brandTint: Color,
    val brandTintAlt: Color,
    val amber: Color,
    val amberSurface: Color,
    val amberBorder: Color,
    val amberText: Color,
    val amberSubText: Color,
    val onAmber: Color,
    val error: Color,
    val onError: Color,
    val shadow: Color,
    val mascotBody: Color,
    val mascotShade: Color,
    val mascotEye: Color,
)

/** 라이트 — 브랜드 기운이 살짝 도는 크림색 뉴트럴. */
val LightPalette = SlPalette(
    background = Color(0xFFF5F2E9), // oklch(96% 0.012 90)
    surface = Color(0xFFFDFCF7), // oklch(99% 0.006 90)
    surfaceAlt = Color(0xFFEDE8D9), // oklch(93% 0.02 90)
    border = Color(0xFFDBD7CD), // oklch(88% 0.015 90)
    borderStrong = Color(0xFFC1BDB3), // oklch(80% 0.015 90)
    trackOff = Color(0xFFD1CDC3), // oklch(85% 0.015 90)
    textPrimary = Color(0xFF171D26), // oklch(23% 0.02 260)
    textSecondary = Color(0xFF5D646F), // oklch(50% 0.02 260)
    textTertiary = Color(0xFF737B87), // oklch(58% 0.02 260)
    brand = Color(0xFF009267), // oklch(58% 0.13 165)
    brandInk = Color(0xFF006944), // oklch(45% 0.12 165)
    brandDeep = Color(0xFF005A38), // oklch(40% 0.115 165)
    onBrand = Color(0xFFF6FEFA), // oklch(99% 0.01 165)
    brandTint = Color(0xFFC7F0DC), // oklch(92% 0.05 165)
    brandTintAlt = Color(0xFFCDF2E0), // oklch(93% 0.045 165)
    amber = Color(0xFFE48233), // oklch(70% 0.15 55)
    amberSurface = Color(0xFFFFE7C3), // oklch(94% 0.055 75)
    amberBorder = Color(0xFFF8CB9C), // oklch(87% 0.08 68)
    amberText = Color(0xFF492B0F), // oklch(32% 0.06 60)
    amberSubText = Color(0xFF6A4F38), // oklch(45% 0.05 62)
    onAmber = Color(0xFFFEFCF4), // oklch(99% 0.01 90)
    error = Color(0xFFC53637), // oklch(55% 0.18 25)
    onError = Color(0xFFFFF9F8), // oklch(99% 0.01 25)
    shadow = Color(0xFF414853), // oklch(40% 0.02 260)
    mascotBody = Color(0xFF009267), // = Brand
    mascotShade = Color(0xFF006944), // = BrandInk
    mascotEye = Color(0xFF005A38), // = BrandDeep
)

/**
 * 다크 — 짙은 회색에 브랜드 초록(165) 기운만 아주 살짝 섞었습니다.
 *
 * 라이트를 뒤집지 않았습니다. 뒤집으면 초록이 형광처럼 뜨고 크림색이 누렇게
 * 가라앉습니다. 역할별로 다시 골랐습니다:
 * - 면은 밝기 18.5 → 22.5 → 26.5% 로 **밝을수록 앞**에 있는 층이 됩니다(그림자 대신).
 * - 글자는 순백이 아니라 90% — 어두운 면 위 순백은 번져 보이고 눈이 쉽게 지칩니다.
 * - 브랜드 버튼은 밝은 초록 위 **어두운 글자**로 바꿨습니다. 어두운 배경에서 흰 글자
 *   초록 버튼은 버튼이 배경 속으로 가라앉습니다.
 * - 강조 글자(BrandInk·BrandDeep)는 어두운 초록이 읽히지 않아 밝은 초록으로 올렸습니다.
 *
 * 대비는 모두 WCAG AA 이상입니다(본문 13:1, 보조 글자 5.3:1 이상).
 */
val DarkPalette = SlPalette(
    background = Color(0xFF0E1411), // oklch(18.5% 0.011 165)
    surface = Color(0xFF171E1A), // oklch(22.5% 0.012 165)
    surfaceAlt = Color(0xFF202724), // oklch(26.5% 0.013 165)
    border = Color(0xFF2A322F), // oklch(31% 0.013 165)
    borderStrong = Color(0xFF444D49), // oklch(41% 0.013 165)
    trackOff = Color(0xFF373F3B), // oklch(36% 0.013 165)
    textPrimary = Color(0xFFD9E0DD), // oklch(90% 0.008 165) — 13:1, 흰 글씨의 눈부심을 피합니다
    textSecondary = Color(0xFFAAB4AF), // oklch(76% 0.012 165)
    textTertiary = Color(0xFF89928D), // oklch(65% 0.012 165) — 표면 위 5.3:1
    brand = Color(0xFF2EB184), // oklch(68% 0.13 165) — 어두운 글자와 6.8:1
    brandInk = Color(0xFF69D6AA), // oklch(80% 0.12 165)
    brandDeep = Color(0xFF8CE3BE), // oklch(85% 0.1 165)
    onBrand = Color(0xFF051810), // oklch(19% 0.03 165)
    brandTint = Color(0xFF13382A), // oklch(31% 0.05 165)
    brandTintAlt = Color(0xFF123226), // oklch(29% 0.045 165)
    amber = Color(0xFFED914C), // oklch(74% 0.14 55)
    amberSurface = Color(0xFF38240F), // oklch(28% 0.045 65)
    amberBorder = Color(0xFF5B3A1B), // oklch(38% 0.065 62)
    amberText = Color(0xFFF7CC9B), // oklch(87% 0.08 70)
    amberSubText = Color(0xFFCEAE8C), // oklch(77% 0.06 68)
    onAmber = Color(0xFF1E1006), // oklch(19% 0.03 60)
    error = Color(0xFFF5746D), // oklch(71% 0.16 25)
    onError = Color(0xFF200E0D), // oklch(19% 0.03 25)
    shadow = Color(0xFF010201), // oklch(8% 0.01 165)
    mascotBody = Color(0xFF3CC998), // = 잠금 화면의 Dark.GreenIcon
    mascotShade = Color(0xFF0D8963), // = Dark.GreenDeep
    mascotEye = Color(0xFF0A3123), // = Dark.GreenTint
)

val LocalSlPalette = staticCompositionLocalOf { LightPalette }

/**
 * 색 토큰. 이름은 그대로 두고 값만 테마를 따라 바뀝니다 — 화면 코드는
 * `SlColor.Background` 처럼 예전과 똑같이 씁니다.
 *
 * 컴포저블 안에서만 읽을 수 있습니다. `Canvas { }` 처럼 그리기 블록 안에서
 * 쓰려면 블록 밖에서 먼저 꺼내 두세요.
 */
object SlColor {
    val Background: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.background
    val Surface: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.surface
    val SurfaceAlt: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.surfaceAlt
    val Border: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.border
    val BorderStrong: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.borderStrong
    val TrackOff: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.trackOff
    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.textPrimary
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.textSecondary
    val TextTertiary: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.textTertiary
    val Brand: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.brand
    val BrandInk: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.brandInk
    val BrandDeep: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.brandDeep
    val OnBrand: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.onBrand
    val BrandTint: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.brandTint
    val BrandTintAlt: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.brandTintAlt
    val Amber: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.amber
    val AmberSurface: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.amberSurface
    val AmberBorder: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.amberBorder
    val AmberText: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.amberText
    val AmberSubText: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.amberSubText
    val OnAmber: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.onAmber
    val Error: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.error
    val OnError: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.onError
    val Shadow: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.shadow
    val MascotBody: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.mascotBody
    val MascotShade: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.mascotShade
    val MascotEye: Color
        @Composable @ReadOnlyComposable get() = LocalSlPalette.current.mascotEye

    /** 앱 뱃지는 식별을 위해 각 서비스 색을 유지합니다. */
    object Social {
        val GoogleBlue = Color(0xFF4285F4)
        val GoogleGreen = Color(0xFF34A853)
        val GoogleYellow = Color(0xFFFBBC05)
        val GoogleRed = Color(0xFFEA4335)
        val Kakao = Color(0xFFF6D653) // oklch(88% 0.15 95)
        val KakaoInk = Color(0xFF362512) // oklch(28% 0.04 70)
    }

    /**
     * 잠금 화면 전용 색. 앱의 다크 모드([DarkPalette])와 별개로 **언제나** 이 색입니다 —
     * 잠금 화면은 "여기서 멈춤"을 알리는 자리라, 라이트 모드에서도 어둡게 떠야 앱과
     * 구분됩니다. 순수 검정은 쓰지 않습니다.
     */
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

