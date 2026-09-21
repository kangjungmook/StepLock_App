package com.steplock.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.steplock.app.R
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

fun Int.formatThousands(): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(this)

/** 목표 수면은 30분 단위로만 조절되므로 "7시간" 또는 "7시간 30분"이 됩니다. */
@Composable
fun sleepGoalLabel(hours: Float): String {
    val whole = hours.toInt()
    val half = (hours - whole) >= 0.5f
    return if (half) {
        stringResource(R.string.duration_hours_half, whole)
    } else {
        stringResource(R.string.duration_hours, whole)
    }
}

@Composable
fun durationLabel(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) {
        stringResource(R.string.duration_hours, hours)
    } else {
        stringResource(R.string.duration_hours_minutes, hours, rest)
    }
}

fun sleepGoalMinutes(hours: Float): Int = (hours * 60).roundToInt()

/** 타이머 표시용 mm:ss. */
fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0L)
    return "%02d:%02d".format(Locale.KOREA, totalSeconds / 60, totalSeconds % 60)
}

/** 받침 유무에 따라 은/는을 붙입니다. "쇼츠는", "틱톡은". */
fun withTopicParticle(word: String): String {
    val last = word.lastOrNull() ?: return word
    val isHangulSyllable = last.code in 0xAC00..0xD7A3
    val hasFinalConsonant = isHangulSyllable && (last.code - 0xAC00) % 28 != 0
    return word + if (hasFinalConsonant) "은" else "는"
}
