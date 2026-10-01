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

/**
 * 사용 시간·잠긴 시간처럼 분 단위로 짧을 수 있는 시간. 한 시간이 안 되면 "32분"으로만
 * 씁니다 — [durationLabel] 은 수면용이라 "0시간 32분"이 됩니다.
 */
@Composable
fun minutesLabel(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
    else -> durationLabel(minutes)
}

fun sleepGoalMinutes(hours: Float): Int = (hours * 60).roundToInt()

/** 타이머 표시용 mm:ss. */
fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0L)
    return "%02d:%02d".format(Locale.KOREA, totalSeconds / 60, totalSeconds % 60)
}

/**
 * 받침 유무에 따라 은/는을 붙입니다. "유튜브는", "틱톡은".
 *
 * 잠글 앱을 사용자가 고르게 되면서 영문 이름("Instagram")도 들어옵니다. 영문은
 * 받침을 글자로 알 수 없어 **소리로 받침이 되는 끝소리**만 추려 봅니다 —
 * Instagram·TikTok·Reddit 은 "은", YouTube·Netflix·Discord 는 "는".
 * "-st"(Pinterest → 핀터레스트)처럼 모음이 붙어 읽히는 끝은 "는"으로 둡니다.
 * 완벽할 수는 없고, 틀리더라도 "는" 쪽으로 틀리게 해 둡니다.
 */
fun withTopicParticle(word: String): String {
    val trimmed = word.trimEnd()
    val last = trimmed.lastOrNull() ?: return word
    val hasFinalConsonant = when {
        last.code in 0xAC00..0xD7A3 -> (last.code - 0xAC00) % 28 != 0
        last in 'A'..'Z' || last in 'a'..'z' -> {
            val lower = trimmed.lowercase()
            when {
                lower.endsWith("st") -> false
                lower.endsWith("ng") -> true
                else -> lower.last() in LATIN_FINAL_CONSONANTS
            }
        }
        else -> false
    }
    return trimmed + if (hasFinalConsonant) "은" else "는"
}

/** 한국어로 읽을 때 받침이 되는 영문 끝소리. */
private const val LATIN_FINAL_CONSONANTS = "bklmnpt"
