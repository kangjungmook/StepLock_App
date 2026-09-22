package com.steplock.app.data

import java.time.LocalDate

/** @Preview 전용 값. 실제 화면은 DataStore와 걸음 센서에서 채웁니다. */
object SampleData {
    private const val PREVIEW_UUID = "preview-device"

    /** 프리뷰용 차단 앱. 실제 목록은 기기에서 읽습니다. */
    val apps = listOf(
        InstalledApp("com.google.android.youtube", "YouTube"),
        InstalledApp("com.instagram.android", "Instagram"),
        InstalledApp("com.ss.android.ugc.tiktok.lite", "TikTok Lite"),
    )

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
        blockedAppIds = setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.ss.android.ugc.tiktok.lite",
        ),
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
    private val lastSevenValues = listOf(
        Triple(6420, 390, 1),
        Triple(8210, 445, 3),
        Triple(8600, 430, 3),
        Triple(9040, 470, 4),
        Triple(8150, 425, 3),
        Triple(8320, 440, 3),
        Triple(5240, 440, 2),
    )

    /**
     * 그 앞 23일. 목표에 못 미친 날과 아예 기록이 없는 날(0)을 섞었습니다 —
     * 프리뷰에서 차트의 빈 막대와 100%가 아닌 달성률을 확인할 수 있어야
     * 기능이 고장 난 것처럼 보이는 조합을 미리 걸러냅니다.
     */
    private val earlierValues = listOf(
        Triple(7100, 400, 2), Triple(0, 0, 0), Triple(9300, 455, 4),
        Triple(8050, 410, 3), Triple(6200, 380, 1), Triple(8800, 460, 3),
        Triple(10400, 470, 5), Triple(5600, 395, 2), Triple(0, 0, 0),
        Triple(8150, 430, 3), Triple(7400, 420, 2), Triple(9100, 450, 4),
        Triple(8600, 435, 3), Triple(4900, 360, 1), Triple(8300, 445, 3),
        Triple(11200, 480, 5), Triple(7800, 425, 2), Triple(8450, 440, 3),
        Triple(6700, 405, 2), Triple(9600, 465, 4), Triple(8200, 430, 3),
        Triple(7300, 415, 2), Triple(8900, 450, 3),
    )

    val weekly: List<DailyStat> = lastSevenValues.toDays()

    /**
     * 통계 화면에서 30일 기간을 골랐을 때. 마지막 이레가 [weekly] 와 같은 값이라
     * 기간을 바꿔도 "오늘 n보"가 달라지지 않습니다.
     */
    val monthly: List<DailyStat> = (earlierValues + lastSevenValues).toDays()

    /** 오늘로 끝나는 목록으로 바꿉니다 — 마지막 항목이 오늘입니다. */
    private fun List<Triple<Int, Int, Int>>.toDays(): List<DailyStat> =
        mapIndexed { index, (steps, sleepMinutes, sessions) ->
            DailyStat(
                deviceUuid = PREVIEW_UUID,
                date = LocalDate.now().minusDays((size - 1 - index).toLong()),
                steps = steps,
                sleepMinutes = sleepMinutes,
                pomodoroSessions = sessions,
            )
        }
}
