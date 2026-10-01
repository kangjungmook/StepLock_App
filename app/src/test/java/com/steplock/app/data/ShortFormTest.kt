package com.steplock.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "쇼츠만 막기" — 화면 판단([ShortFormDetector], [ShortFormState])과
 * 앱 전체 / 쇼츠만 / 안 막음 사이의 엄함 비교.
 */
class ShortFormTest {

    private val youtube = "com.google.android.youtube"

    @Test
    fun `쇼츠 플레이어 이름이 있으면 쇼츠 화면이다`() {
        val ids = sequenceOf("$youtube:id/toolbar", "$youtube:id/reel_recycler", "")
        assertTrue(ShortFormDetector.isShortForm(ShortFormTarget.YouTubeShorts, ids))
    }

    @Test
    fun `홈 피드에는 쇼츠 표식이 없다`() {
        val ids = sequenceOf("$youtube:id/results", "$youtube:id/pivot_bar", "")
        assertFalse(ShortFormDetector.isShortForm(ShortFormTarget.YouTubeShorts, ids))
    }

    @Test
    fun `이름 일부만 같으면 쇼츠로 보지 않는다`() {
        val ids = sequenceOf("$youtube:id/reel_recycler_shelf_title")
        assertFalse(ShortFormDetector.isShortForm(ShortFormTarget.YouTubeShorts, ids))
    }

    @Test
    fun `앱이 앞에 나오기 전에 본 값은 믿지 않는다`() {
        ShortFormState.report(youtube, visible = true, at = 1_000L)
        assertTrue(ShortFormState.isVisible(youtube, since = 1_000L))
        // 잠금 화면에서 홈으로 갔다가 유튜브를 다시 연 경우 — 지난 "쇼츠 보는 중"은 낡은 값.
        assertFalse(ShortFormState.isVisible(youtube, since = 2_000L))
        assertFalse(ShortFormState.isVisible("com.instagram.android", since = 0L))
    }

    private val base = LockSettings(deviceUuid = "test", blockedAppIds = setOf(youtube))

    @Test
    fun `앱 전체를 쇼츠만으로 바꾸면 느슨해진다`() {
        val shortsOnly = base.copy(shortFormOnly = setOf(youtube))
        assertTrue(shortsOnly.isLooserThan(base))
        assertFalse(base.isLooserThan(shortsOnly))
    }

    @Test
    fun `쇼츠만 막던 앱을 빼면 느슨해진다`() {
        val shortsOnly = base.copy(shortFormOnly = setOf(youtube))
        val removed = base.copy(blockedAppIds = emptySet())
        assertTrue(removed.isLooserThan(shortsOnly))
    }

    @Test
    fun `대기 중에는 앱마다 더 엄한 쪽을 쓴다`() {
        val shortsOnly = base.copy(shortFormOnly = setOf(youtube))
        // 앱 전체 ↔ 쇼츠만 → 앱 전체
        assertEquals(emptySet<String>(), base.strictestWith(shortsOnly).shortFormOnly)
        // 쇼츠만 ↔ 안 막음 → 쇼츠만 (앱을 빼려다 오히려 앱 전체가 막히면 안 됩니다)
        val removed = base.copy(blockedAppIds = emptySet())
        val strictest = shortsOnly.strictestWith(removed)
        assertEquals(setOf(youtube), strictest.blockedAppIds)
        assertEquals(setOf(youtube), strictest.shortFormOnly)
    }
}
