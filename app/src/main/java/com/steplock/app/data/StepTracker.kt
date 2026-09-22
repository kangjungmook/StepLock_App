package com.steplock.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * TYPE_STEP_COUNTER는 부팅 이후 누적 걸음을 주므로, 그날 첫 값을 기준점으로 저장해
 * 오늘 걸음만 계산합니다. 날짜가 바뀌거나 재부팅으로 누적값이 줄면 기준점을 다시 잡습니다.
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
                emitAll(counterTotals(sensor).map { total -> stepsSinceBaseline(total) })
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

    private suspend fun stepsSinceBaseline(total: Long): Int {
        val today = LocalDate.now()
        val baseline = repository.readStepBaseline()
        if (baseline == null || baseline.date != today || total < baseline.counter) {
            repository.writeStepBaseline(StepBaseline(today, total))
            return 0
        }
        return (total - baseline.counter).toInt()
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
