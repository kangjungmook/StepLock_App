package com.steplock.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor

/**
 * 시안과 같은 stroke 벡터 아이콘 세트입니다. 이모지나 비트맵은 쓰지 않습니다.
 * stroke는 검정으로 그려 두고 Icon(tint = ...)으로 색을 입힙니다.
 */
object SlIcons {

    val Mail = strokeVector(
        name = "Mail",
        strokeWidth = 1.8f,
        paths = listOf(
            roundRect(x = 3f, y = 5.5f, w = 18f, h = 13f, r = 2.5f),
            "m3.6 7 8.4 6 8.4-6",
        ),
    )

    val PasswordLock = strokeVector(
        name = "PasswordLock",
        strokeWidth = 1.8f,
        paths = listOf(
            roundRect(x = 5f, y = 10.5f, w = 14f, h = 9.5f, r = 2.5f),
            "M8.2 10.5V7.6a3.8 3.8 0 0 1 7.6 0v2.9",
        ),
    )

    val Eye = strokeVector(
        name = "Eye",
        strokeWidth = 1.8f,
        paths = listOf(
            "M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z",
            circle(cx = 12f, cy = 12f, r = 3.2f),
        ),
    )

    val EyeOff = strokeVector(
        name = "EyeOff",
        strokeWidth = 1.8f,
        paths = listOf(
            "M4 4l16 16",
            "M9.6 6.1A9.6 9.6 0 0 1 12 5.8c6 0 9.5 6.2 9.5 6.2a17 17 0 0 1-2.9 3.7",
            "M6.4 8.1A16.6 16.6 0 0 0 2.5 12s3.5 6.2 9.5 6.2c1 0 1.9-.15 2.7-.4",
            "M9.8 12a2.2 2.2 0 0 0 3 2.1",
        ),
    )

    private const val CHECK = "m5 12.8 4.4 4.4L19 7.6"

    val CheckThin = strokeVector("CheckThin", 2f, listOf(CHECK))
    val CheckMedium = strokeVector("CheckMedium", 2.4f, listOf(CHECK))
    val CheckBold = strokeVector("CheckBold", 3f, listOf(CHECK))
    val CheckExtraBold = strokeVector("CheckExtraBold", 3.4f, listOf(CHECK))

    val Lock = strokeVector(
        name = "Lock",
        strokeWidth = 2f,
        paths = listOf(
            roundRect(x = 4.5f, y = 10f, w = 15f, h = 10.5f, r = 3f),
            "M8 10V7a4 4 0 0 1 8 0v3",
        ),
    )

    val Steps = strokeVector(
        name = "Steps",
        strokeWidth = 1.8f,
        paths = listOf(
            circle(cx = 13.5f, cy = 4.2f, r = 2f),
            "M10 21.5l2.2-6.1-2.7-2.6V8.4L14 7l2.1 3.2 3 1",
            "M9.6 12.8L7.4 17",
        ),
    )

    val Moon = strokeVector(
        name = "Moon",
        strokeWidth = 1.8f,
        paths = listOf("M20.2 14.6A8.4 8.4 0 0 1 9.4 3.8a8.6 8.6 0 1 0 10.8 10.8z"),
    )

    val Timer = strokeVector(
        name = "Timer",
        strokeWidth = 1.8f,
        paths = listOf(
            circle(cx = 12f, cy = 13.5f, r = 7.5f),
            "M12 9.8v3.7l2.4 1.6",
            "M9.6 2.8h4.8",
        ),
    )

    val TimerCompact = strokeVector(
        name = "TimerCompact",
        strokeWidth = 2f,
        paths = listOf(
            circle(cx = 12f, cy = 13.5f, r = 7.5f),
            "M12 9.8v3.7l2.4 1.6",
        ),
    )

    val ChevronRight = strokeVector("ChevronRight", 2f, listOf("m9 5 7 7-7 7"))

    val ArrowLeft = strokeVector("ArrowLeft", 2f, listOf("M19 12H5", "m11 6-6 6 6 6"))

    val Minus = strokeVector("Minus", 2.2f, listOf("M5 12h14"))

    val Plus = strokeVector("Plus", 2.2f, listOf("M5 12h14", "M12 5v14"))

    /** 잠글 앱 고르기 화면의 검색 칸. 돋보기 — 원 하나와 손잡이. */
    val Search = strokeVector(
        "Search",
        2f,
        listOf(
            "M10.5 3.5a7 7 0 1 1 0 14 7 7 0 0 1 0-14z",
            "m15.6 15.6 4.9 4.9",
        ),
    )

    val Home = strokeVector(
        name = "Home",
        strokeWidth = 2f,
        paths = listOf("M4 10.4 12 4l8 6.4V20a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1z"),
    )

    val Stats = strokeVector(
        name = "Stats",
        strokeWidth = 2f,
        paths = listOf("M4 20V10", "M10 20V5", "M16 20v-7", "M22 20H2"),
    )

    val Settings = strokeVector(
        name = "Settings",
        strokeWidth = 2f,
        paths = listOf(
            circle(cx = 12f, cy = 12f, r = 3.2f),
            "M19.2 14.8a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
        ),
    )

    val KakaoTalk = strokeVector(
        name = "KakaoTalk",
        strokeWidth = 1.8f,
        paths = listOf(
            "M12 4.6c-4.4 0-8 2.7-8 6.1 0 2.15 1.45 4.04 3.65 5.15l-.8 3.1 3.4-2.1c.56.08 1.15.12 1.75.12 4.4 0 8-2.72 8-6.1S16.4 4.6 12 4.6z",
        ),
    )

    val Apple = filledVector(
        name = "Apple",
        paths = listOf(
            "M16.3 12.75c.02 2.5 2.2 3.33 2.22 3.34-.02.06-.35 1.2-1.15 2.37-.7 1.02-1.42 2.03-2.56 2.05-1.12.02-1.48-.66-2.76-.66-1.28 0-1.68.64-2.74.68-1.1.04-1.94-1.1-2.64-2.11-1.52-2.2-2.68-6.22-1.12-8.93.78-1.35 2.16-2.2 3.66-2.22 1.08-.02 2.1.73 2.76.73.66 0 1.9-.9 3.2-.77.54.02 2.08.2 3.06 1.65-.08.05-1.84 1.08-1.82 3.2zM14.2 4.6c.6-.72 1-1.72.9-2.72-.87.04-1.92.58-2.54 1.3-.55.63-1.03 1.65-.9 2.62.97.08 1.94-.49 2.54-1.2z",
        ),
    )

    /** 구글 마크는 식별을 위해 브랜드 4색을 유지합니다 — tint 없이 Image로 그립니다. */
    val GoogleMark: ImageVector = builder("GoogleMark").apply {
        addFilled(
            "M21.6 12.2c0-.7-.06-1.35-.18-2H12v3.8h5.4a4.7 4.7 0 0 1-2 3.1v2.5h3.2c1.9-1.7 3-4.3 3-7.4z",
            SlColor.Social.GoogleBlue,
        )
        addFilled(
            "M12 22c2.7 0 4.96-.9 6.6-2.4l-3.2-2.5c-.9.6-2 .96-3.4.96a5.95 5.95 0 0 1-5.6-4.1H3.1v2.6A10 10 0 0 0 12 22z",
            SlColor.Social.GoogleGreen,
        )
        addFilled(
            "M6.4 13.96a6 6 0 0 1 0-3.9V7.5H3.1a10 10 0 0 0 0 9l3.3-2.54z",
            SlColor.Social.GoogleYellow,
        )
        addFilled(
            "M12 6a5.4 5.4 0 0 1 3.8 1.5l2.85-2.85A9.6 9.6 0 0 0 12 2 10 10 0 0 0 3.1 7.5l3.3 2.56A5.95 5.95 0 0 1 12 6z",
            SlColor.Social.GoogleRed,
        )
    }.build()
}

private fun builder(name: String) = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
)

private fun nodes(pathData: String) = PathParser().parsePathString(pathData).toNodes()

private fun ImageVector.Builder.addFilled(pathData: String, color: Color) = addPath(
    pathData = nodes(pathData),
    fill = SolidColor(color),
)

private fun strokeVector(
    name: String,
    strokeWidth: Float,
    paths: List<String>,
): ImageVector = builder(name).apply {
    paths.forEach { path ->
        addPath(
            pathData = nodes(path),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()

private fun filledVector(name: String, paths: List<String>): ImageVector = builder(name).apply {
    paths.forEach { path -> addFilled(path, Color.Black) }
}.build()

private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
    val horizontal = w - 2 * r
    val vertical = h - 2 * r
    return "M${x + r},$y h$horizontal a$r,$r 0 0 1 $r,$r v$vertical " +
        "a$r,$r 0 0 1 ${-r},$r h${-horizontal} a$r,$r 0 0 1 ${-r},${-r} " +
        "v${-vertical} a$r,$r 0 0 1 $r,${-r} z"
}

private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r},$cy a$r,$r 0 1 0 ${2 * r},0 a$r,$r 0 1 0 ${-2 * r},0 z"
