package com.steplock.app.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Health Connect에서 지난 24시간의 수면 세션을 읽어 분으로 합칩니다.
 * 세션을 창에 맞춰 잘라 더하므로, 자정을 넘긴 수면도 한 번만 셉니다.
 * 읽은 값은 DataStore에 캐시해 감시 서비스가 매초 조회하지 않게 합니다.
 */
class SleepRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    val readPermission: String = HealthPermission.getReadPermission(SleepSessionRecord::class)

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermission(): Boolean {
        if (!isAvailable()) return false
        return runCatching {
            HealthConnectClient.getOrCreate(context)
                .permissionController
                .getGrantedPermissions()
                .contains(readPermission)
        }.getOrDefault(false)
    }

    /** 성공하면 캐시를 갱신하고 읽은 분을 돌려줍니다. 권한이 없으면 null. */
    suspend fun refresh(): Int? {
        if (!hasPermission()) return null
        val windowEnd = Instant.now()
        val windowStart = windowEnd.minus(WINDOW)

        val minutes = runCatching {
            HealthConnectClient.getOrCreate(context)
                .readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(windowStart, windowEnd),
                    ),
                )
                .records
                .sumOf { record ->
                    val start = maxOf(record.startTime, windowStart)
                    val end = minOf(record.endTime, windowEnd)
                    Duration.between(start, end).toMinutes().coerceAtLeast(0L)
                }
                .toInt()
        }.getOrNull() ?: return null

        settingsRepository.writeSleepMinutes(LocalDate.now(), minutes)
        return minutes
    }

    private companion object {
        val WINDOW: Duration = Duration.ofHours(24)
    }
}
