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

/**
 * 라이트 — "등산로 표식" 팔레트.
 *
 * 차가운 안개색 바탕(색조 230)에 흑연색 글자, 그리고 강조는 **표식 주황 하나**뿐입니다.
 * 산길 나무에 칠해 둔 주황 표식처럼 "여기로 가면 된다"를 가리키는 색이라, 진행·주요
 * 버튼·지금 탭에만 씁니다. 크림색 바탕과 초록 강조(예전)는 요즘 생성된 앱들이 가장
 * 흔히 고르는 조합이라 버렸습니다.
 *
 * "잠김"은 두 번째 강조색을 들이지 않고 **흑연색**으로 말합니다 — 닫힌 것은 어둡고,
 * 열린 것은 주황. amber* 토큰 이름은 그대로 두고 값만 흑연 계열로 바꿨습니다.
 *
 * 주황 버튼 위 글자는 흰색이 아니라 흑연색입니다(흰 글자는 대비 3.6:1로 부족).
 */
val LightPalette = SlPalette(
    background = Color(0xFFF1F3F4), // oklch(96.3% 0.003 230) 안개
    surface = Color(0xFFFBFCFC), // oklch(99% 0.002 230)
    surfaceAlt = Color(0xFFE6EAEC), // oklch(93.5% 0.006 230)
    border = Color(0xFFD8DDE0), // oklch(89.5% 0.008 230)
    borderStrong = Color(0xFFB9C0C5), // oklch(80.5% 0.011 230)
    trackOff = Color(0xFFCDD3D7), // oklch(86% 0.009 230)
    textPrimary = Color(0xFF1A2026), // oklch(23% 0.015 245) 흑연 — 바탕 위 15:1
    textSecondary = Color(0xFF55606A), // oklch(48% 0.02 245) — 6.1:1
    textTertiary = Color(0xFF6B7680), // oklch(56% 0.02 240) — 4.5:1
    brand = Color(0xFFEC5A24), // oklch(65% 0.19 38) 표식 주황
    brandInk = Color(0xFFB23E10), // oklch(52% 0.16 38) — 글자용, 바탕 위 5.6:1
    brandDeep = Color(0xFF8F300A), // oklch(45% 0.14 38)
    onBrand = Color(0xFF1A2026), // 주황 위 흑연 글자 4.6:1
    brandTint = Color(0xFFFADDCF), // oklch(91% 0.045 45)
    brandTintAlt = Color(0xFFFCE6DC), // oklch(93.5% 0.035 45)
    amber = Color(0xFF2B3238), // 잠김 = 흑연
    amberSurface = Color(0xFFE3E7EA),
    amberBorder = Color(0xFFCDD3D8),
    amberText = Color(0xFF1A2026),
    amberSubText = Color(0xFF4E5862),
    onAmber = Color(0xFFF6F8F9),
    error = Color(0xFFB8234F), // oklch(50% 0.18 5) — 주황과 색조가 떨어진 진홍
    onError = Color(0xFFFFF7F9),
    shadow = Color(0xFF3A4550),
    mascotBody = Color(0xFFEC5A24), // = Brand
    mascotShade = Color(0xFFB23E10), // = BrandInk
    mascotEye = Color(0xFF1A2026), // 흑연 눈
)

/**
 * 다크 — 푸른 흑연(색조 235) 위에 한 단계 밝힌 주황.
 *
 * 라이트를 뒤집지 않았습니다. 면은 밝기 18 → 21.5 → 25% 로 밝을수록 앞에 있는 층이고,
 * 글자는 순백이 아닌 91% 입니다. 주황은 어두운 바탕에서 형광처럼 뜨지 않도록 밝기만
 * 올리고 채도는 조금 내렸습니다. 주황 버튼 위 글자는 바탕과 같은 흑연(7.4:1).
 */
val DarkPalette = SlPalette(
    background = Color(0xFF12171B), // oklch(19% 0.012 235)
    surface = Color(0xFF1A2025), // oklch(22.5% 0.013 235)
    surfaceAlt = Color(0xFF222A30), // oklch(26% 0.014 235)
    border = Color(0xFF2C353C), // oklch(30.5% 0.015 235)
    borderStrong = Color(0xFF46515A), // oklch(41% 0.018 235)
    trackOff = Color(0xFF39434B),
    textPrimary = Color(0xFFE3E8EB), // oklch(92% 0.007 230) — 14:1
    textSecondary = Color(0xFFAEB8BF), // 8:1
    textTertiary = Color(0xFF8C979F), // 5.6:1
    brand = Color(0xFFF7743F), // oklch(71% 0.17 42)
    brandInk = Color(0xFFFF9B72), // oklch(78% 0.13 45)
    brandDeep = Color(0xFFFFB79A), // oklch(84% 0.1 48)
    onBrand = Color(0xFF12171B),
    brandTint = Color(0xFF3A2219),
    brandTintAlt = Color(0xFF331F18),
    amber = Color(0xFFC9D2D8), // 잠김 = 밝은 흑연
    amberSurface = Color(0xFF232B31),
    amberBorder = Color(0xFF36414A),
    amberText = Color(0xFFE3E8EB),
    amberSubText = Color(0xFFAEB8BF),
    onAmber = Color(0xFF12171B),
    error = Color(0xFFFF6B8B),
    onError = Color(0xFF22080F),
    shadow = Color(0xFF05080A),
    mascotBody = Color(0xFFF7743F),
    mascotShade = Color(0xFFC14A1C),
    mascotEye = Color(0xFF12171B),
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
        val Background = Color(0xFF12171B) // oklch(19% 0.012 235) — 앱 다크와 같은 흑연
        val Surface = Color(0xFF1A2025)
        val SurfaceAlt = Color(0xFF222A30)
        val Border = Color(0xFF2C353C)

        val TextPrimary = Color(0xFFE9EDEF)
        val TextBright = Color(0xFFE3E8EB)
        val TextChip = Color(0xFFD6DFEC) // oklch(90% 0.02 260)
        val TextIcon = Color(0xFFB6BECB) // oklch(80% 0.02 260)
        val TextMuted = Color(0xFF9DA5B1) // oklch(72% 0.02 260)
        val TextLink = Color(0xFF9199A5) // oklch(68% 0.02 260)

        // 잠금 화면의 강조도 앱과 같은 표식 주황 하나입니다.
        val AmberTint = Color(0xFF3A2219)
        val AmberIcon = Color(0xFFF7743F)
        val AmberText = Color(0xFFFF9B72) // 바탕 위 8.9:1
        // 가장 큰 면적을 차지하는 링 — 주황을 한 단계 눌러 눈이 덜 피로합니다.
        val AmberRing = Color(0xFFE8693A)

        // "채움(달성)"은 두 번째 색 대신 밝은 흑연으로 — 주황은 "아직 가야 할 길"에만.
        val GreenTint = Color(0xFF2A333A)
        val GreenBorder = Color(0xFF46515A)
        val GreenIcon = Color(0xFFE3E8EB)
        val GreenText = Color(0xFFE3E8EB)
        val GreenDeep = Color(0xFFC14A1C)

        /** 잠금 화면의 스텝이 — 앱과 같은 주황 몸. */
        val MascotBody = Color(0xFFF7743F)
        val MascotShade = Color(0xFFC14A1C)
    }
}

