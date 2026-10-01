package com.steplock.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.steplock.app.system.AppPermissions
import java.time.LocalDate
import java.time.ZoneId

/** 사용 기록 한 줄을 계산에 필요한 만큼만 옮겨 둔 것. 안드로이드 타입과 떼어 단위 테스트합니다. */
data class UsageEvent(
    val packageName: String,
    /** 같은 앱의 화면(액티비티) 이름. 한 앱 안에서 화면을 옮길 때 겹침을 가리는 데 씁니다. */
    val className: String?,
    val timeMs: Long,
    val kind: Kind,
) {
    enum class Kind {
        /** 앱 화면이 앞으로 나왔습니다. */
        Resumed,

        /** 앱 화면이 뒤로 갔습니다(다른 앱으로 이동, 홈, 잠금 화면이 덮음). */
        Paused,

        /** 화면이 꺼졌거나 기기가 꺼집니다 — 앞에 있던 앱이 모두 끝난 것으로 봅니다. */
        AllStopped,
    }
}

/**
 * 앱마다 [from] ~ [to] 사이에 화면 앞에 있었던 시간(ms).
 *
 * 규칙:
 * - 같은 앱의 화면이 하나라도 앞에 있으면 그 앱을 쓰는 중입니다. 앱 안에서 화면을
 *   옮길 때 "새 화면 열림"이 "옛 화면 닫힘"보다 먼저 오기도 해서, 화면 이름별로 셉니다.
 * - 첫 기록이 "닫힘"이면 [from] 전부터 열려 있던 것이라 [from] 부터 셉니다
 *   (자정 전에 열어 둔 앱).
 * - [to] 까지 닫히지 않은 앱은 지금도 쓰는 중이라 [to] 까지 셉니다.
 * - 화면이 꺼지면 열려 있던 앱을 모두 닫습니다.
 *
 * 잠금 화면이 잠긴 앱을 덮으면 그 앱은 "닫힘"이 되므로, 잠금 화면을 보고 있던
 * 시간은 앱 사용 시간에 들어가지 않습니다.
 */
fun foregroundDurations(events: List<UsageEvent>, from: Long, to: Long): Map<String, Long> {
    val totals = mutableMapOf<String, Long>()
    val openSince = mutableMapOf<String, Long>()
    val openScreens = mutableMapOf<String, MutableSet<String>>()
    val seen = mutableSetOf<String>()

    fun close(pkg: String, at: Long) {
        val since = openSince.remove(pkg) ?: return
        openScreens.remove(pkg)
        val start = since.coerceAtLeast(from)
        val end = at.coerceAtMost(to)
        if (end > start) totals[pkg] = (totals[pkg] ?: 0L) + (end - start)
    }

    for (event in events.sortedBy { it.timeMs }) {
        if (event.timeMs > to) break
        val pkg = event.packageName
        val screen = event.className ?: ""
        when (event.kind) {
            UsageEvent.Kind.Resumed -> {
                seen += pkg
                val screens = openScreens.getOrPut(pkg) { mutableSetOf() }
                if (screens.isEmpty()) openSince[pkg] = event.timeMs
                screens += screen
            }

            UsageEvent.Kind.Paused -> {
                if (pkg !in seen) {
                    // 구간 시작 전부터 열려 있던 앱.
                    seen += pkg
                    openSince[pkg] = from
                    openScreens[pkg] = mutableSetOf(screen)
                }
                val screens = openScreens[pkg] ?: continue
                screens -= screen
                if (screens.isEmpty()) close(pkg, event.timeMs)
            }

            UsageEvent.Kind.AllStopped -> openSince.keys.toList().forEach { close(it, event.timeMs) }
        }
    }
    openSince.keys.toList().forEach { close(it, to) }
    return totals
}

/**
 * 안드로이드의 사용 기록(UsageStatsManager)에서 오늘 앱별 사용 시간을 읽습니다.
 *
 * 사용 정보 접근 권한으로 읽는 **앱 단위** 기록이라, 유튜브 안에서 쇼츠를 본 시간과
 * 긴 영상을 본 시간을 나누지는 못합니다. 쇼츠만 따로 재려면 화면 내용을 읽는
 * 접근성 서비스가 필요한데, 이 앱은 화면 내용을 읽지 않기로 했습니다.
 */
class AppUsageReader(private val context: Context) {

    /** [packages] 각각을 오늘 쓴 시간(ms). 권한이 없거나 읽지 못하면 빈 값. */
    fun todayUsage(packages: Set<String>): Map<String, Long> {
        if (packages.isEmpty() || !AppPermissions.hasUsageAccess(context)) return emptyMap()
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val now = System.currentTimeMillis()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val events = runCatching { manager.queryEvents(startOfDay, now) }.getOrNull()
            ?: return emptyMap()
        val collected = mutableListOf<UsageEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val kind = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEvent.Kind.Resumed
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageEvent.Kind.Paused
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.DEVICE_SHUTDOWN,
                -> UsageEvent.Kind.AllStopped
                else -> null
            } ?: continue
            if (kind != UsageEvent.Kind.AllStopped && event.packageName !in packages) continue
            collected += UsageEvent(event.packageName, event.className, event.timeStamp, kind)
        }
        return foregroundDurations(collected, startOfDay, now)
    }
}
