package com.steplock.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 폰의 걸음 센서(TYPE_STEP_COUNTER)로 오늘 걸음을 셉니다 — 만보기 앱들이 쓰는 그 센서입니다.
 *
 * 센서는 부팅 이후 누적 걸음을 주므로 날짜별 기준점으로 오늘 걸음만 계산합니다
 * ([resolveTodaySteps]). 재부팅하면 센서가 0부터 다시 세는데, 그 전까지 센 오늘 걸음을
 * 이어 붙여서 **재부팅해도 오늘 걸음이 0으로 돌아가지 않습니다.**
 *
 * 센서는 **누군가 듣고 있는 동안에만** 셉니다(안드로이드 문서: 등록이 풀리면 세지 않음).
 * 앱 화면을 닫아도 계속 세는 건 감시 서비스가 이 흐름을 늘 듣고 있기 때문입니다.
 */
class StepTracker(
    private val context: Context,
    private val repository: SettingsRepository,
) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)

    fun hasPermission(): Boolean = hasActivityRecognitionPermission(context)

    /**
     * 오늘 걸음 수.
     *
     * 권한과 센서는 **수집을 시작할 때** 확인합니다. 플로우를 만드는 시점에 한 번만
     * 확인하면 안 됩니다 — 뷰모델은 온보딩보다 먼저 만들어지므로, 그때는 걸음 권한이
     * 아직 없습니다. 예전에는 거기서 `flowOf(0)` 을 반환해 버려서, 온보딩에서 권한을
     * 준 뒤에도 **앱을 완전히 종료하기 전까지 걸음이 0에서 멈춰 있었습니다.**
     *
     * 그래서 권한이 없으면 0을 내보내며 기다리다가, 생기는 즉시 센서로 넘어갑니다.
     */
    fun todaySteps(): Flow<Int> = flow {
        // 첫 값을 바로 내보냅니다. 이 플로우가 combine 의 한쪽이라서, 여기서
        // 아무것도 내보내지 않으면 홈 화면 전체가 빈 채로 대기합니다.
        emit(0)

        while (true) {
            val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            // 센서가 없는 기기에서는 기다려도 생기지 않습니다.
            if (sensor == null) return@flow
            if (hasPermission()) {
                emitAll(
                    counterTotals(sensor).map { total ->
                        val today = LocalDate.now()
                        repository.countTodaySteps(
                            total = total,
                            today = today,
                            bootCount = currentBootCount(),
                            bootedToday = bootedOn(today),
                        )
                    },
                )
                return@flow
            }
            delay(PERMISSION_POLL_MS)
        }
    }.distinctUntilChanged()

    /** 센서가 주는 부팅 이후 누적 걸음. */
    private fun counterTotals(sensor: Sensor): Flow<Long> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values.firstOrNull()?.toLong() ?: 0L)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager?.unregisterListener(listener) }
    }

    /**
     * 지금의 부팅 횟수. 재부팅을 가장 정확하게 알아채는 값입니다 — 누적값이 줄었는지만
     * 보면, 재부팅 뒤 기준보다 많이 걸은 다음에 앱이 읽었을 때 재부팅을 놓칩니다.
     * 제조사가 값을 두지 않았으면 null 이고, 그때는 누적값으로만 판단합니다.
     */
    private fun currentBootCount(): Int? = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
    }.getOrNull()

    /** 폰이 [today] 에 켜졌는지. 그렇다면 센서 누적값이 전부 오늘 걸음입니다. */
    private fun bootedOn(today: LocalDate): Boolean {
        val bootedAt = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime())
        return bootedAt.atZone(ZoneId.systemDefault()).toLocalDate() == today
    }
}

/** 권한이 생겼는지 다시 확인하는 간격. 온보딩에서 허용한 직후 곧 반영됩니다. */
private const val PERMISSION_POLL_MS = 1_000L

fun hasActivityRecognitionPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
