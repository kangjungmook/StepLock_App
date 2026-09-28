package com.steplock.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * 센서 누적값 → 오늘 걸음 규칙([resolveTodaySteps]).
 *
 * 재부팅과 자정은 실기기에서 재현하기 어려워서 여기서 확인합니다.
 */
class StepCountingTest {

    private val today = LocalDate.of(2026, 9, 28)
    private val yesterday = today.minusDays(1)

    @Test
    fun `그날 처음 읽으면 0부터 센다`() {
        val reading = resolveTodaySteps(
            total = 12_000,
            today = today,
            bootCount = 7,
            bootedToday = false,
            baseline = null,
            recordedToday = 0,
        )

        assertEquals(0, reading.steps)
        assertEquals(StepBaseline(today, counter = 12_000, offset = 0, bootCount = 7), reading.newBaseline)
    }

    @Test
    fun `같은 날에는 기준점 이후 걸음만 센다`() {
        val reading = resolveTodaySteps(
            total = 12_500,
            today = today,
            bootCount = 7,
            bootedToday = false,
            baseline = StepBaseline(today, counter = 12_000, offset = 0, bootCount = 7),
            recordedToday = 400,
        )

        assertEquals(500, reading.steps)
        assertNull("기준점이 그대로면 저장소에 쓰지 않습니다", reading.newBaseline)
    }

    @Test
    fun `자정이 지나면 새로 센다`() {
        val reading = resolveTodaySteps(
            total = 20_000,
            today = today,
            bootCount = 7,
            bootedToday = false,
            baseline = StepBaseline(yesterday, counter = 12_000, offset = 0, bootCount = 7),
            recordedToday = 0,
        )

        assertEquals(0, reading.steps)
        assertEquals(20_000L, reading.newBaseline?.counter)
    }

    @Test
    fun `오늘 켠 폰은 켠 뒤 걸음을 모두 오늘로 센다`() {
        // 아침 7시에 재부팅하고 정오에 앱을 처음 열어도 그사이 3,000보를 버리지 않습니다.
        val reading = resolveTodaySteps(
            total = 3_000,
            today = today,
            bootCount = 8,
            bootedToday = true,
            baseline = StepBaseline(yesterday, counter = 12_000, offset = 0, bootCount = 7),
            recordedToday = 0,
        )

        assertEquals(3_000, reading.steps)
        assertEquals(0L, reading.newBaseline?.counter)
    }

    @Test
    fun `재부팅해도 오늘 걸음이 0으로 돌아가지 않는다`() {
        // 5,000보 걷고 재부팅 → 센서는 0부터. 재부팅 뒤 40보를 더 걸었습니다.
        val reading = resolveTodaySteps(
            total = 40,
            today = today,
            bootCount = 8,
            bootedToday = true,
            baseline = StepBaseline(today, counter = 12_000, offset = 0, bootCount = 7),
            recordedToday = 5_000,
        )

        assertEquals(5_040, reading.steps)
        assertEquals(StepBaseline(today, counter = 0, offset = 5_000, bootCount = 8), reading.newBaseline)
    }

    @Test
    fun `재부팅 뒤 기준보다 많이 걸었어도 부팅 횟수로 알아챈다`() {
        // 기준 누적값(100)보다 재부팅 뒤 누적값(800)이 커서, 누적값만 보면 재부팅을
        // 놓치고 700보로 잘못 셉니다. 부팅 횟수가 바뀐 걸로 알아챕니다.
        val reading = resolveTodaySteps(
            total = 800,
            today = today,
            bootCount = 8,
            bootedToday = true,
            baseline = StepBaseline(today, counter = 100, offset = 0, bootCount = 7),
            recordedToday = 5_000,
        )

        assertEquals(5_800, reading.steps)
    }

    @Test
    fun `부팅 횟수를 모르는 기기는 누적값이 줄어든 걸로 알아챈다`() {
        val reading = resolveTodaySteps(
            total = 40,
            today = today,
            bootCount = null,
            bootedToday = true,
            baseline = StepBaseline(today, counter = 12_000, offset = 0, bootCount = null),
            recordedToday = 5_000,
        )

        assertEquals(5_040, reading.steps)
    }

    @Test
    fun `재부팅 뒤에는 이어 붙인 걸음에서 계속 센다`() {
        val reading = resolveTodaySteps(
            total = 300,
            today = today,
            bootCount = 8,
            bootedToday = true,
            baseline = StepBaseline(today, counter = 0, offset = 5_000, bootCount = 8),
            recordedToday = 5_040,
        )

        assertEquals(5_300, reading.steps)
        assertNull(reading.newBaseline)
    }

    @Test
    fun `하루에 두 번 재부팅해도 앞서 이어 붙인 걸음을 잃지 않는다`() {
        // 기록(1분마다)이 아직 따라오지 못해 기준점의 offset 보다 작을 때도 큰 쪽을 씁니다.
        val reading = resolveTodaySteps(
            total = 20,
            today = today,
            bootCount = 9,
            bootedToday = true,
            baseline = StepBaseline(today, counter = 0, offset = 5_000, bootCount = 8),
            recordedToday = 4_800,
        )

        assertEquals(5_020, reading.steps)
        assertEquals(5_000, reading.newBaseline?.offset)
    }

    @Test
    fun `어제 재부팅해 이어 붙였던 걸음은 오늘로 넘어오지 않는다`() {
        val reading = resolveTodaySteps(
            total = 9_000,
            today = today,
            bootCount = 8,
            bootedToday = false,
            baseline = StepBaseline(yesterday, counter = 0, offset = 5_000, bootCount = 8),
            recordedToday = 0,
        )

        assertEquals(0, reading.steps)
        assertEquals(0, reading.newBaseline?.offset)
    }
}
