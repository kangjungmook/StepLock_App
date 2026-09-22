import java.util.Properties

// 릴리스 서명 정보. 저장소에 두지 않고 keystore.properties 나 환경변수로 받습니다.
// 둘 다 없으면 서명 설정 없이도 빌드는 되게 해서(디버그·CI) 개발을 막지 않습니다.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStorePath = signingValue("storeFile", "STEPLOCK_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "STEPLOCK_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "STEPLOCK_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "STEPLOCK_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

/**
 * AdMob 식별자.
 *
 * **디버그 빌드는 언제나 테스트 ID를 씁니다.** 실 ID는 릴리스 빌드에만 들어갑니다.
 * 자기 앱의 실 광고를 직접 누르면 AdMob 계정이 정지될 수 있는데, 폰에 깔아서
 * 눌러 보는 건 늘 디버그 APK라서 이렇게 갈라 두면 그 사고가 구조적으로 막힙니다.
 *
 * 실 ID는 비밀값이 아닙니다(APK를 열면 그대로 보입니다). 저장소에 두지 않는 이유는
 * 계정마다 다르고, 남이 받아 빌드했을 때 내 계정으로 노출이 집계되면 안 되기
 * 때문입니다. admob.properties(로컬) 또는 환경변수(CI)로 받습니다.
 *
 * https://developers.google.com/admob/android/test-ads
 */
val admobProperties = Properties().apply {
    val file = rootProject.file("admob.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// 구글이 공개해 둔 테스트 ID. AdMob 계정이 없어도 동작하고, 항상 테스트 광고만 나옵니다.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobRewarded = "ca-app-pub-3940256099942544/5224354917"
val testAdmobBanner = "ca-app-pub-3940256099942544/6300978111"

fun admobId(key: String, env: String): String? =
    admobProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(env)?.takeIf { it.isNotBlank() }

val realAdmobAppId = admobId("appId", "STEPLOCK_ADMOB_APP_ID")
val realAdmobRewarded = admobId("rewardedUnitId", "STEPLOCK_ADMOB_REWARDED")
val realAdmobBanner = admobId("bannerUnitId", "STEPLOCK_ADMOB_BANNER")

// 셋 중 하나라도 비어 있으면 릴리스도 테스트 ID로 빌드합니다 — 앱 ID만 실물이고
// 광고 단위는 테스트인 뒤섞인 빌드가 나오면 원인을 찾기 어렵습니다.
val hasRealAdmobIds = listOf(realAdmobAppId, realAdmobRewarded, realAdmobBanner)
    .all { !it.isNullOrBlank() }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.steplock.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.steplock.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Supabase 공개 키입니다. RLS로 보호되는 값이라 저장소에 함께 둡니다.
        buildConfigField("String", "SUPABASE_URL", "\"https://thcchvmwkgfhqqzponnx.supabase.co\"")
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRoY2Nodm13a2dmaHFxenBvbm54Iiwicm9sZSI6ImFub24i" +
                "LCJpYXQiOjE3ODk5NjE3MzksImV4cCI6MjEwNTUzNzczOX0." +
                "qceCUQgCg8uTKXaF1YtZ8DJtPIdQ-S2a5t4chQJrFPY\"",
        )

        // 기본값은 테스트 ID입니다. 릴리스에서만 아래 buildTypes 가 덮어씁니다.
        // 앱 ID는 매니페스트 meta-data 로만 읽히므로 플레이스홀더로 넣습니다 —
        // 이 값이 비면 앱이 실행 즉시 죽습니다(AdMob SDK가 던집니다).
        manifestPlaceholders["admobAppId"] = testAdmobAppId
        buildConfigField("String", "ADMOB_REWARDED_UNIT", "\"$testAdmobRewarded\"")
        buildConfigField("String", "ADMOB_BANNER_UNIT", "\"$testAdmobBanner\"")
        // 테스트 ID로 빌드됐는지 앱이 알 수 있게 해 둡니다 —
        // 설정 화면에서 "테스트 광고" 라고 밝히는 데 씁니다.
        buildConfigField("boolean", "ADMOB_TEST_IDS", "true")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // 서명 정보가 없으면 서명하지 않은 릴리스로 빌드합니다 —
            // R8 규칙이 맞는지 CI에서 확인하는 데는 서명이 필요 없습니다.
            signingConfig = signingConfigs.findByName("release")

            // 실 광고는 여기에만 들어갑니다. ID가 없으면 defaultConfig 의
            // 테스트 ID가 그대로 남아 릴리스도 안전하게 빌드됩니다.
            if (hasRealAdmobIds) {
                manifestPlaceholders["admobAppId"] = realAdmobAppId!!
                buildConfigField("String", "ADMOB_REWARDED_UNIT", "\"$realAdmobRewarded\"")
                buildConfigField("String", "ADMOB_BANNER_UNIT", "\"$realAdmobBanner\"")
                buildConfigField("boolean", "ADMOB_TEST_IDS", "false")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.health.connect.client)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.androidx.ui.tooling)
}
