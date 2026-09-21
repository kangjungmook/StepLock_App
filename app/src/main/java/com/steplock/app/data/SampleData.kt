package com.steplock.app.data

import java.time.LocalDate

/** @Preview 전용 값. 실제 화면은 DataStore와 걸음 센서에서 채웁니다. */
object SampleData {
    private const val PREVIEW_UUID = "preview-device"

    val apps = BlockedAppCatalog.apps

    val settings = LockSettings(
        deviceUuid = PREVIEW_UUID,
        displayName = "지우",
        sleepEnabled = true,
        blockedAppIds = setOf("shorts", "reels", "tiktok"),
    )

    val today = DailyStat(
        deviceUuid = PREVIEW_UUID,
        date = LocalDate.now(),
        steps = 5240,
        sleepMinutes = 440,
        pomodoroSessions = 2,
    )

    val weekly: List<DailyStat> = listOf(
        Triple(6420, 390, 1),
        Triple(8210, 445, 3),
        Triple(3180, 420, 0),
        Triple(9040, 470, 2),
        Triple(7650, 400, 4),
        Triple(0, 505, 0),
        Triple(5240, 440, 2),
    ).mapIndexed { index, (steps, sleepMinutes, sessions) ->
        DailyStat(
            deviceUuid = PREVIEW_UUID,
            date = LocalDate.now().minusDays((6 - index).toLong()),
            steps = steps,
            sleepMinutes = sleepMinutes,
            pomodoroSessions = sessions,
        )
    }
}
