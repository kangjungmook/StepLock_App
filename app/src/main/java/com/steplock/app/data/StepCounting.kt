package com.steplock.app.data

import java.time.LocalDate

/**
 * 걸음 센서의 누적값을 "오늘 걸음"으로 바꿀 때 쓰는 기준점.
 *
 * 센서(TYPE_STEP_COUNTER)는 **부팅 이후 누적 걸음**을 주고, 재부팅하면 0부터 다시
 * 셉니다. 그래서 날짜마다 기준점을 두고 이렇게 셉니다.
 *
 * `오늘 걸음 = offset + (지금 누적 − counter)`
 */
data class StepBaseline(
    val date: LocalDate,
    /** 기준이 된 센서 누적값. */
    val counter: Long,
    /** 재부팅 전에 이미 센 오늘 걸음. 재부팅 뒤에는 여기에 이어서 더합니다. */
    val offset: Int = 0,
    /** 기준을 잡을 때의 부팅 횟수. 지금과 다르면 그사이 재부팅한 것입니다. 모르면 null. */
    val bootCount: Int? = null,
)

/** [resolveTodaySteps] 의 결과 — 오늘 걸음과, 바뀌었다면 새 기준점(아니면 null). */
data class StepReading(val steps: Int, val newBaseline: StepBaseline?)

/**
 * 센서 누적값 하나를 오늘 걸음으로 바꿉니다.
 *
 * 안드로이드 API 를 쓰지 않는 순수 함수로 둔 것은 **단위 테스트로 검증하려고**입니다.
 * 재부팅과 자정은 실기기에서 재현하기 어렵습니다(StepCountingTest).
 *
 * @param total 센서가 준 부팅 이후 누적 걸음.
 * @param bootCount 지금의 부팅 횟수(Settings.Global.BOOT_COUNT). 모르면 null.
 * @param bootedToday 폰이 오늘 켜졌는지. 그렇다면 센서 숫자가 전부 오늘 걸음입니다.
 * @param recordedToday 오늘 날짜로 이미 기록해 둔 걸음(1분마다 저장). 재부팅 전 값을
 *   잇는 데 씁니다.
 */
fun resolveTodaySteps(
    total: Long,
    today: LocalDate,
    bootCount: Int?,
    bootedToday: Boolean,
    baseline: StepBaseline?,
    recordedToday: Int,
): StepReading {
    // 새 날이거나 처음 — 오늘 기준점을 새로 잡습니다.
    if (baseline == null || baseline.date != today) {
        // 오늘 켜진 폰이면 센서 숫자가 전부 오늘 걸음이라 0을 기준으로 삼습니다.
        // 아침에 재부팅하고 앱을 늦게 열어도 그사이 걸음을 버리지 않습니다.
        val counter = if (bootedToday) 0L else total
        return StepReading(
            steps = (total - counter).toStepCount(),
            newBaseline = StepBaseline(today, counter, offset = 0, bootCount = bootCount),
        )
    }

    // 재부팅 — 부팅 횟수가 바뀌었거나(가장 정확), 모를 때는 누적값이 기준보다 작아졌을 때.
    // 누적값만 보면 재부팅 뒤 기준보다 많이 걸은 경우를 놓치므로 부팅 횟수를 먼저 봅니다.
    val bootChanged = bootCount != null && baseline.bootCount != null && bootCount != baseline.bootCount
    if (bootChanged || total < baseline.counter) {
        // 재부팅 전까지 센 오늘 걸음을 이어 붙입니다. 기준점에 남은 값과 기록해 둔 값 중
        // 큰 쪽 — 하루에 두 번 재부팅해도 앞서 이어 붙인 걸음을 잃지 않습니다.
        // 재부팅은 오늘 일어났으므로 센서 숫자(total)도 전부 오늘 걸음입니다.
        val carried = maxOf(baseline.offset, recordedToday)
        return StepReading(
            steps = carried + total.toStepCount(),
            newBaseline = StepBaseline(today, counter = 0L, offset = carried, bootCount = bootCount),
        )
    }

    return StepReading(
        steps = baseline.offset + (total - baseline.counter).toStepCount(),
        newBaseline = null,
    )
}

private fun Long.toStepCount(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
