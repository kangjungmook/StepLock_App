package com.steplock.app.data

import java.time.LocalDate
import java.util.UUID

/** 화면 확인용 데모 값. 실제로는 센서·DataStore·PackageManager에서 채웁니다. */
object SampleData {
    const val USER_NAME = "지우"

    private val deviceUuid = UUID.randomUUID().toString()

    val apps = listOf(
        BlockedApp(id = "shorts", name = "쇼츠", subtitle = "YouTube · 짧은 영상", initial = "S"),
        BlockedApp(id = "reels", name = "릴스", subtitle = "Instagram · 짧은 영상", initial = "R"),
        BlockedApp(id = "tiktok", name = "틱톡", subtitle = "TikTok · 짧은 영상", initial = "T"),
        BlockedApp(id = "x", name = "엑스", subtitle = "X · 짧은 영상", initial = "X"),
    )

    val settings = LockSettings(
        deviceUuid = deviceUuid,
        blockedAppIds = setOf("shorts", "reels", "tiktok"),
    )

    val today = DailyStat(
        deviceUuid = deviceUuid,
        date = LocalDate.now(),
        steps = 5240,
        sleepMinutes = 440,
        pomodoroSessions = 2,
    )
}
