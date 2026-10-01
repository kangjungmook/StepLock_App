package com.steplock.app.data

import com.steplock.app.data.UsageEvent.Kind.AllStopped
import com.steplock.app.data.UsageEvent.Kind.Paused
import com.steplock.app.data.UsageEvent.Kind.Resumed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 사용 기록 → 앱별 사용 시간 규칙([foregroundDurations]). */
class AppUsageTest {

    private val yt = "com.google.android.youtube"
    private val ig = "com.instagram.android"
    private val min = 60_000L

    private fun ev(pkg: String, at: Long, kind: UsageEvent.Kind, screen: String = "Main") =
        UsageEvent(pkg, screen, at, kind)

    @Test
    fun `열고 닫은 사이만 센다`() {
        val result = foregroundDurations(
            listOf(ev(yt, 10 * min, Resumed), ev(yt, 25 * min, Paused)),
            from = 0,
            to = 60 * min,
        )
        assertEquals(15 * min, result[yt])
    }

    @Test
    fun `여러 번 열면 더한다`() {
        val result = foregroundDurations(
            listOf(
                ev(yt, 0, Resumed), ev(yt, 5 * min, Paused),
                ev(yt, 20 * min, Resumed), ev(yt, 30 * min, Paused),
            ),
            from = 0,
            to = 60 * min,
        )
        assertEquals(15 * min, result[yt])
    }

    @Test
    fun `지금도 열려 있으면 지금까지 센다`() {
        val result = foregroundDurations(listOf(ev(yt, 50 * min, Resumed)), from = 0, to = 60 * min)
        assertEquals(10 * min, result[yt])
    }

    @Test
    fun `자정 전에 열어 둔 앱은 자정부터 센다`() {
        val result = foregroundDurations(listOf(ev(yt, 7 * min, Paused)), from = 0, to = 60 * min)
        assertEquals(7 * min, result[yt])
    }

    @Test
    fun `앱 안에서 화면을 옮겨도 두 번 세지 않는다`() {
        // 새 화면이 먼저 열리고 옛 화면이 나중에 닫히는 순서.
        val result = foregroundDurations(
            listOf(
                ev(yt, 0, Resumed, "Home"),
                ev(yt, 10 * min, Resumed, "Shorts"),
                ev(yt, 10 * min + 100, Paused, "Home"),
                ev(yt, 20 * min, Paused, "Shorts"),
            ),
            from = 0,
            to = 60 * min,
        )
        assertEquals(20 * min, result[yt])
    }

    @Test
    fun `화면이 꺼지면 열린 앱을 닫는다`() {
        val result = foregroundDurations(
            listOf(ev(yt, 0, Resumed), ev("android", 12 * min, AllStopped)),
            from = 0,
            to = 60 * min,
        )
        assertEquals(12 * min, result[yt])
    }

    @Test
    fun `앱마다 따로 센다`() {
        val result = foregroundDurations(
            listOf(
                ev(yt, 0, Resumed), ev(yt, 10 * min, Paused),
                ev(ig, 10 * min, Resumed), ev(ig, 13 * min, Paused),
            ),
            from = 0,
            to = 60 * min,
        )
        assertEquals(10 * min, result[yt])
        assertEquals(3 * min, result[ig])
    }

    @Test
    fun `기록이 없으면 비어 있다`() {
        assertTrue(foregroundDurations(emptyList(), from = 0, to = 60 * min).isEmpty())
    }
}
