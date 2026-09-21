package com.steplock.app.data

import java.time.LocalDate

/**
 * 연속 달성 일수. 습관을 이어 가는 재미가 이 숫자에 달려 있어서, 하루 중간에
 * 0으로 떨어지지 않게 하는 게 중요합니다.
 *
 * 주의: 달성 여부를 **지금의 설정**으로 다시 판정합니다. 목표를 낮추면 과거
 * 기록도 달성으로 바뀝니다. 그날의 목표를 함께 저장하지 않기 때문인데,
 * 기록이 30일치뿐이고 목표를 자주 바꾸지 않는다는 전제입니다.
 */
object StreakCalculator {

    /**
     * 오늘부터 거꾸로 센 연속 일수.
     * 오늘을 아직 못 채웠으면 어제까지로 셉니다 — 아침에 기록이 0으로 보이면 안 되니까요.
     */
    fun current(
        history: List<DailyStat>,
        settings: LockSettings,
        today: LocalDate = LocalDate.now(),
    ): Int {
        val achieved = achievedDates(history, settings)
        var day = if (today in achieved) today else today.minusDays(1)
        var count = 0
        while (day in achieved) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** 기록에 남아 있는 가장 긴 연속 구간. */
    fun longest(history: List<DailyStat>, settings: LockSettings): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        achievedDates(history, settings).sorted().forEach { date ->
            run = if (previous?.plusDays(1) == date) run + 1 else 1
            previous = date
            if (run > best) best = run
        }
        return best
    }

    private fun achievedDates(history: List<DailyStat>, settings: LockSettings): Set<LocalDate> =
        history.filter { UnlockEvaluator.isUnlocked(settings, it) }
            .map { it.date }
            .toSet()
}
