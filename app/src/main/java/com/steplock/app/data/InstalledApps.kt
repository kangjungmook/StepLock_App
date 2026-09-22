package com.steplock.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap

/** 기기에 깔려 있고 실행 아이콘이 있는 앱 하나. */
data class InstalledApp(
    val packageName: String,
    val label: String,
)

/**
 * 잠글 앱을 고르기 위해 기기에 깔린 앱을 읽습니다.
 *
 * 전에는 쇼츠·릴스·틱톡·엑스 **네 개를 코드에 박아 두고** 패키지 이름으로 맞췄습니다.
 * 그래서 틱톡 라이트(`com.ss.android.ugc.tiktok.lite`)처럼 패키지가 다른 앱은 아무리
 * 골라도 걸리지 않았습니다. 이제 목록을 기기에서 읽어 사용자가 직접 고릅니다.
 *
 * Android 11 부터는 다른 앱이 보이지 않는 게 기본이라, 매니페스트 `<queries>` 에
 * MAIN/LAUNCHER 인텐트를 선언해 **실행 아이콘이 있는 앱**만 보이게 했습니다.
 * `QUERY_ALL_PACKAGES` 는 쓰지 않습니다 — 플레이가 용도를 따로 심사하는 권한이고,
 * 잠글 앱을 고르는 데는 실행 가능한 앱만 있으면 충분합니다.
 */
class InstalledAppsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val selfPackage: String = appContext.packageName

    /**
     * 실행 아이콘이 있는 앱을 이름 순으로. 스텝락 자신은 뺍니다 —
     * 스스로를 잠그면 설정을 바꿀 방법이 없어집니다.
     */
    fun launchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != selfPackage }
            .map { InstalledApp(it.packageName, packageManager.getApplicationLabel(it).toString()) }
            // 같은 패키지가 런처 액티비티를 여러 개 가질 수 있어 한 번만 남깁니다.
            .distinctBy { it.packageName }
            .sortedBy { it.label }
            .toList()
            // 여기서 읽은 이름을 캐시에 넣어 둡니다 — 목록을 한 번 본 뒤에는
            // 홈·잠금 화면이 이름을 다시 조회하지 않습니다.
            .onEach { labels[it.packageName] = it.label }
    }

    /**
     * 앱 이름. 지워졌거나 보이지 않는 패키지는 null 입니다.
     *
     * 한 번 읽은 이름은 [labels] 에 남겨 둡니다. [resolve] 가 홈 화면의 상태를
     * 만들 때마다 불리는데(걸음 수가 올라갈 때마다 포함) 그게 메인 스레드에서
     * 도는 프로세스 간 호출이라, 캐시가 없으면 걸을 때마다 화면이 끊깁니다.
     */
    fun label(packageName: String): String? =
        labels.getOrPut(packageName) {
            runCatching {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, 0),
                ).toString()
            }.getOrDefault(NOT_FOUND)
        }.takeIf { it != NOT_FOUND }

    fun icon(packageName: String): Drawable? = runCatching {
        packageManager.getApplicationIcon(packageName)
    }.getOrNull()

    /**
     * 저장된 패키지 이름을 화면에 쓸 형태로 바꿉니다.
     *
     * 지워진 앱도 목록에 남겨 둡니다 — 조용히 사라지면 사용자가 왜 목록이 줄었는지
     * 알 수 없고, 다시 설치하면 그대로 다시 막혀야 합니다. 이름을 못 읽으면
     * 패키지 이름을 그대로 보여 줍니다.
     */
    fun resolve(packageNames: Collection<String>): List<InstalledApp> =
        packageNames
            .map { InstalledApp(it, label(it) ?: it) }
            .sortedBy { it.label }

    private companion object {
        /**
         * 패키지 이름 → 앱 이름. 프로세스가 사는 동안 유지합니다 — 앱 이름은
         * 거의 바뀌지 않고, 바뀌어도 앱을 다시 열면 맞아집니다.
         *
         * 여러 곳에서 각자 이 저장소를 만들어 쓰므로(목록·아이콘·잠금 화면)
         * 캐시는 인스턴스가 아니라 클래스에 둡니다. 배경 스레드에서도 읽으니
         * 동시 접근이 안전한 맵이어야 합니다.
         */
        val labels = ConcurrentHashMap<String, String>()

        /** 캐시는 null 을 담을 수 없어서 "못 찾음"을 값으로 표시합니다. */
        const val NOT_FOUND = "\u0000"
    }
}

/**
 * 0.1.0 까지는 잠글 앱을 `"shorts"` 같은 자체 id 로 저장했습니다.
 * 이제 패키지 이름으로 저장하므로, 예전 값을 읽을 때 한 번 옮겨 줍니다.
 */
object LegacyBlockedApps {
    private val packages = mapOf(
        "shorts" to "com.google.android.youtube",
        "reels" to "com.instagram.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "x" to "com.twitter.android",
    )

    /**
     * 저장된 값을 패키지 이름 집합으로 정리합니다.
     * 패키지 이름에는 점이 들어가므로, 점이 없고 옛 id 도 아닌 값은 버립니다.
     */
    fun migrate(stored: Set<String>): Set<String> =
        stored.mapNotNullTo(mutableSetOf()) { value ->
            packages[value] ?: value.takeIf { '.' in it }
        }
}
