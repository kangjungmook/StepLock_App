package com.steplock.app.data

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

@Serializable
data class LockSettingsRow(
    @SerialName("user_id") val userId: String,
    @SerialName("device_uuid") val deviceUuid: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("step_goal") val stepGoal: Int,
    @SerialName("sleep_goal_hours") val sleepGoalHours: Float,
    @SerialName("pomodoro_goal") val pomodoroGoal: Int,
    @SerialName("steps_enabled") val stepsEnabled: Boolean,
    @SerialName("sleep_enabled") val sleepEnabled: Boolean,
    @SerialName("pomodoro_enabled") val pomodoroEnabled: Boolean,
    @SerialName("require_all_conditions") val requireAllConditions: Boolean,
    @SerialName("blocked_app_ids") val blockedAppIds: List<String>,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class DailyStatRow(
    @SerialName("user_id") val userId: String,
    @SerialName("stat_date") val statDate: String,
    @SerialName("device_uuid") val deviceUuid: String,
    @SerialName("steps") val steps: Int,
    @SerialName("sleep_minutes") val sleepMinutes: Int,
    @SerialName("pomodoro_sessions") val pomodoroSessions: Int,
)

/**
 * 로그인한 계정의 설정·기록을 Supabase와 맞춥니다.
 *
 * 설정은 `updated_at`이 최신인 쪽이 통째로 이깁니다(항목별 병합은 하지 않습니다).
 * 일별 기록은 로컬 것을 업서트한 뒤, 원격에만 있는 날짜를 로컬로 채웁니다 —
 * 같은 날짜가 양쪽에 있으면 로컬 값을 정답으로 둡니다. 기기가 하나라는 전제입니다.
 */
class SyncRepository(private val settingsRepository: SettingsRepository) {

    private val client = SupabaseProvider.client

    suspend fun sync(accountId: String): Result<Unit> = runCatching {
        val local = settingsRepository.preferences.first()
        val remoteSettings = fetchSettings(accountId)
        val localUpdatedAt = local.settingsUpdatedAt

        if (remoteSettings != null && remoteSettings.updatedAtMillis() > localUpdatedAt) {
            settingsRepository.applyRemoteSettings(
                settings = remoteSettings.toSettings(local.settings),
                updatedAt = remoteSettings.updatedAtMillis(),
            )
        } else {
            client.from(TABLE_SETTINGS).upsert(local.settings.toRow(accountId, localUpdatedAt))
        }

        val localHistory = local.history
        if (localHistory.isNotEmpty()) {
            client.from(TABLE_DAILY).upsert(localHistory.map { it.toRow(accountId) })
        }

        val knownDates = localHistory.map { it.date }.toSet()
        val missing = fetchDailyStats(accountId)
            .mapNotNull { it.toDailyStat(local.settings.deviceUuid) }
            .filter { it.date !in knownDates }
        if (missing.isNotEmpty()) settingsRepository.mergeHistory(missing)
    }

    private suspend fun fetchSettings(accountId: String): LockSettingsRow? =
        client.from(TABLE_SETTINGS)
            .select { filter { eq("user_id", accountId) } }
            .decodeSingleOrNull()

    private suspend fun fetchDailyStats(accountId: String): List<DailyStatRow> =
        client.from(TABLE_DAILY)
            .select { filter { eq("user_id", accountId) } }
            .decodeList()

    private companion object {
        const val TABLE_SETTINGS = "lock_settings"
        const val TABLE_DAILY = "daily_stats"
    }
}

private fun LockSettings.toRow(accountId: String, updatedAt: Long) = LockSettingsRow(
    userId = accountId,
    deviceUuid = deviceUuid,
    displayName = displayName,
    stepGoal = stepGoal,
    sleepGoalHours = sleepGoalHours,
    pomodoroGoal = pomodoroGoal,
    stepsEnabled = stepsEnabled,
    sleepEnabled = sleepEnabled,
    pomodoroEnabled = pomodoroEnabled,
    requireAllConditions = requireAllConditions,
    blockedAppIds = blockedAppIds.toList(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
)

private fun LockSettingsRow.toSettings(local: LockSettings) = local.copy(
    stepGoal = stepGoal,
    sleepGoalHours = sleepGoalHours,
    pomodoroGoal = pomodoroGoal,
    stepsEnabled = stepsEnabled,
    sleepEnabled = sleepEnabled,
    pomodoroEnabled = pomodoroEnabled,
    requireAllConditions = requireAllConditions,
    blockedAppIds = blockedAppIds.toSet(),
)

private fun LockSettingsRow.updatedAtMillis(): Long =
    runCatching { OffsetDateTime.parse(updatedAt).toInstant().toEpochMilli() }.getOrDefault(0L)

private fun DailyStat.toRow(accountId: String) = DailyStatRow(
    userId = accountId,
    statDate = date.toString(),
    deviceUuid = deviceUuid,
    steps = steps,
    sleepMinutes = sleepMinutes,
    pomodoroSessions = pomodoroSessions,
)

private fun DailyStatRow.toDailyStat(deviceUuid: String): DailyStat? = runCatching {
    DailyStat(
        deviceUuid = deviceUuid,
        date = LocalDate.parse(statDate),
        steps = steps,
        sleepMinutes = sleepMinutes,
        pomodoroSessions = pomodoroSessions,
    )
}.getOrNull()
