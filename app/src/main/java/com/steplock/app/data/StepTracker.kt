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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
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

    fun todaySteps(): Flow<Int> {
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null || !hasPermission()) return flowOf(0)

        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(event.values.firstOrNull()?.toLong() ?: 0L)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            awaitClose { sensorManager.unregisterListener(listener) }
        }
            .map { total -> stepsSinceBaseline(total) }
            .distinctUntilChanged()
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

fun hasActivityRecognitionPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
