@file:OptIn(ExperimentalTextApi::class)

package com.steplock.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.steplock.app.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val plexSansKr = GoogleFont("IBM Plex Sans KR")
private val barlowCondensed = GoogleFont("Barlow Condensed")

/**
 * 본문·제목 글꼴. IBM Plex Sans KR — 획 끝이 반듯하게 잘린 공학적인 고딕이라,
 * 둥근 기본 고딕(Noto)보다 표지판·계기판 쪽 인상을 줍니다. 가장 굵은 굵기가 Bold 라
 * Black 을 요청한 스타일도 Bold 로 그려집니다.
 *
 * 다운로더블 폰트로 받아옵니다. ttf를 번들하려면 res/font에 넣고 이 정의만 교체하세요.
 */
val PlexSansKr = FontFamily(
    Font(googleFont = plexSansKr, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = plexSansKr, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = plexSansKr, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = plexSansKr, fontProvider = fontProvider, weight = FontWeight.Bold),
)

/**
 * 큰 숫자 전용. Barlow Condensed — 도로 표지판 글자에서 출발한 좁은 고딕이라
 * "2,760"처럼 큰 숫자가 좁은 폭에 시원하게 들어가고, 등산로·이정표라는 이 앱의
 * 세계와도 맞습니다. 숫자에만 씁니다(한글 글리프가 없습니다).
 */
val Numeral = FontFamily(
    Font(googleFont = barlowCondensed, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = barlowCondensed, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = barlowCondensed, fontProvider = fontProvider, weight = FontWeight.Bold),
)

/** 예전 이름을 쓰는 코드를 위해 남겨 둡니다. */
val NotoSansKr = PlexSansKr

private fun slStyle(
    weight: FontWeight,
    size: Float,
    lineHeight: Float = size * 1.35f,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = PlexSansKr,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

private fun numeralStyle(weight: FontWeight, size: Float, lineHeight: Float) =
    slStyle(weight, size, lineHeight).copy(
        fontFamily = Numeral,
        // 자릿수가 바뀌어도 숫자 폭이 같아 카운트다운이 흔들리지 않습니다.
        fontFeatureSettings = "tnum",
    )

object SlText {
    val Wordmark = slStyle(FontWeight.Black, 32f, 36f, -1f)
    val AppTitle = slStyle(FontWeight.Black, 34f, 39f, -0.6f)
    val Greeting = slStyle(FontWeight.Black, 24f, 31f, -0.4f)

    /**
     * 홈 맨 위 한 문장("2,760보만 더 걸으면 잠금이 풀려요"). 화면에서 가장 큰 글자라
     * 인사(24)보다 한 단계 위에 둡니다 — 누구인지보다 지금 무엇을 하면 되는지가 먼저입니다.
     */
    val HeroHeadline = slStyle(FontWeight.Black, 26f, 34f, -0.5f)

    /**
     * 홈의 큰 숫자("2,760보"). 화면에서 가장 먼저 읽혀야 하는 **할 일의 양**이라
     * 모듈러 스케일(1.25)로 HeroHeadline 26 의 두 단계 위(≈ 40 → 52)에 둡니다.
     */
    val HomeBig = slStyle(FontWeight.Bold, 40f, 46f, -0.8f)

    /** 홈의 큰 숫자("2,760"). 표지판 숫자체로 88sp — 화면에서 단 하나의 큰 글자입니다. */
    val HomeNumeral = numeralStyle(FontWeight.SemiBold, 88f, 88f)

    /** 큰 숫자 옆 단위("보", "번"). 숫자와 같은 줄에 작게. */
    val HomeUnit = slStyle(FontWeight.Bold, 26f, 30f)

    /** 큰 숫자 바로 아래 한 줄("더 걸으면 잠금이 풀려요"). */
    val HomeLine = slStyle(FontWeight.Bold, 22f, 30f, -0.3f)

    /** 홈 "오늘 한눈에" 2×2 칸의 숫자. */
    val GlanceValue = numeralStyle(FontWeight.SemiBold, 32f, 34f)

    /** 등산로 이정표(2k · 4k · 6k). */
    val TrailTick = numeralStyle(FontWeight.SemiBold, 13f, 16f)

    /** 길 아래 "5,240 / 8,000보" 의 앞 숫자. */
    val TrailProgress = numeralStyle(FontWeight.SemiBold, 22f, 26f)

    /** 앱 줄 오른쪽의 사용 시간("32분"). */
    val RowNumeral = numeralStyle(FontWeight.SemiBold, 28f, 30f)

    /**
     * 화면 안 구역 제목("오늘", "잠근 앱을 쓴 시간"). 작은 회색 라벨 대신 본문보다 한 단계
     * 큰 제목으로 — 구역이 라벨이 아니라 문장 덩어리로 읽힙니다.
     */
    val SectionTitle = slStyle(FontWeight.Bold, 17f, 24f)

    /** 설정 첫머리의 규칙 문장. */
    val RuleSentence = slStyle(FontWeight.Bold, 24f, 42f, -0.3f)

    /** 홈 "오늘" 줄의 숫자(거리·막은 횟수·남은 허용). 라벨보다 먼저 읽히도록 굵게. */
    val FigureValue = slStyle(FontWeight.Black, 20f, 26f, -0.3f)
    val LockTitle = slStyle(FontWeight.Black, 25f, 32f, -0.4f)
    val RingValue = numeralStyle(FontWeight.SemiBold, 48f, 48f)
    val LoginHeading = slStyle(FontWeight.Black, 20f, 26f)
    val DialogTitle = slStyle(FontWeight.Black, 19f, 26f)
    val StatusTitle = slStyle(FontWeight.Black, 18f, 25f)

    val ScreenTitle = slStyle(FontWeight.Bold, 18f, 24f)
    val RowTitle = slStyle(FontWeight.Bold, 15f, 21f)
    val Cta = slStyle(FontWeight.Bold, 16f, 22f)
    val StepperValue = slStyle(FontWeight.Bold, 17f, 22f)
    val BadgeInitial = slStyle(FontWeight.Bold, 16f, 20f)
    val BadgeInitialSmall = slStyle(FontWeight.Bold, 15f, 20f)
    val Chip = slStyle(FontWeight.Bold, 12f, 16f)
    val Remaining = slStyle(FontWeight.Bold, 14f, 20f)
    val RingPercent = slStyle(FontWeight.Bold, 11f, 14f)
    val SectionLabel = slStyle(FontWeight.Bold, 12f, 16f, 0.6f)
    val NavLabelActive = slStyle(FontWeight.Bold, 11f, 14f)

    val Tagline = slStyle(FontWeight.Normal, 15f, 24f)
    val Body = slStyle(FontWeight.Normal, 14f, 22f)
    val BodySm = slStyle(FontWeight.Normal, 13f, 20f)
    val RowValue = slStyle(FontWeight.Normal, 13f, 18f)
    val LoginNote = slStyle(FontWeight.Normal, 13.5f, 22f)
    val Input = slStyle(FontWeight.Normal, 14f, 20f)
    val Caption = slStyle(FontWeight.Normal, 12f, 19f)
    val Signup = slStyle(FontWeight.Normal, 13f, 20f)

    val ListItem = slStyle(FontWeight.Medium, 15f, 20f)
    val StepperLabel = slStyle(FontWeight.Medium, 14f, 20f)
    val CaptionMedium = slStyle(FontWeight.Medium, 13f, 21f)
    val Label = slStyle(FontWeight.Medium, 12.5f, 18f)
    val LabelSm = slStyle(FontWeight.Medium, 12f, 16f)
    val Link = slStyle(FontWeight.Medium, 13.5f, 20f)
    val LinkSm = slStyle(FontWeight.Medium, 13f, 20f)
    val NavLabel = slStyle(FontWeight.Medium, 11f, 14f)
}

val SlTypography = Typography(
    headlineLarge = SlText.AppTitle,
    headlineMedium = SlText.Greeting,
    titleLarge = SlText.ScreenTitle,
    titleMedium = SlText.RowTitle,
    bodyLarge = SlText.Tagline,
    bodyMedium = SlText.Body,
    bodySmall = SlText.BodySm,
    labelLarge = SlText.Cta,
    labelMedium = SlText.Label,
    labelSmall = SlText.Caption,
)
