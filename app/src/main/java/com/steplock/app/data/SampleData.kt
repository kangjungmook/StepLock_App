package com.steplock.app.data

import java.time.LocalDate

/** @Preview 전용 값. 실제 화면은 DataStore와 걸음 센서에서 채웁니다. */
object SampleData {
    private const val PREVIEW_UUID = "preview-device"

    val apps = BlockedAppCatalog.apps

    val settings = LockSettings(
        deviceUuid = PREVIEW_UUID,
        displayName = "지우",
        // 프리뷰는 세 조건을 모두 켠 사용자를 기준으로 합니다.
        // 전부 만족 모드까지 켜 둔 이유: 걸음(진행 중)과 수면(달성)이 한 화면에
        // 같이 보여서 조건 링의 두 상태를 모두 확인할 수 있고, 홈과 잠금 화면이
        // 같은 데이터에서 같은 결론(아직 잠김)을 내놓습니다.
        sleepEnabled = true,
        pomodoroEnabled = true,
        requireAllConditions = true,
        blockedAppIds = setOf("shorts", "reels", "tiktok"),
    )

    val today = DailyStat(
        deviceUuid = PREVIEW_UUID,
        date = LocalDate.now(),
        steps = 5240,
        sleepMinutes = 440,
        pomodoroSessions = 2,
    )

    /**
     * 최근 닷새는 세 조건을 다 채웠고 오늘은 아직 못 채운 상태.
     * 연속 달성이 5일로 나오고, 오늘은 잠긴 채로 수면만 달성이라
     * 조건 링의 두 상태(퍼센트·체크)가 한 화면에 같이 보입니다.
     */
    val weekly: List<DailyStat> = listOf(
        Triple(6420, 390, 1),
        Triple(8210, 445, 3),
        Triple(8600, 430, 3),
        Triple(9040, 470, 4),
        Triple(8150, 425, 3),
        Triple(8320, 440, 3),
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
