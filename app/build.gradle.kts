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
 * AdMob 식별자. 비밀값은 아니지만(APK 안에 그대로 들어갑니다) 계정마다 달라서
 * admob.properties 나 환경변수로 받고, 없으면 **구글 공식 테스트 ID** 로 떨어집니다.
 *
 * 테스트 ID로 빌드된 앱은 항상 테스트 광고만 띄웁니다. 실수로 실 광고를 직접 눌러
 * 계정이 정지되는 사고가 이 기본값 덕에 일어나지 않습니다.
 * https://developers.google.com/admob/android/test-ads
 */
val admobProperties = Properties().apply {
    val file = rootProject.file("admob.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun admobId(key: String, env: String, testId: String): String =
    admobProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: testId

val admobAppId = admobId("appId", "STEPLOCK_ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
val admobRewardedUnit =
    admobId("rewardedUnitId", "STEPLOCK_ADMOB_REWARDED", "ca-app-pub-3940256099942544/5224354917")
val admobBannerUnit =
    admobId("bannerUnitId", "STEPLOCK_ADMOB_BANNER", "ca-app-pub-3940256099942544/6300978111")

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

        // 앱 ID는 매니페스트 meta-data 로만 읽히므로 플레이스홀더로 넣습니다.
        // 이 값이 비면 앱이 실행 즉시 죽습니다(AdMob SDK가 던집니다).
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_REWARDED_UNIT", "\"$admobRewardedUnit\"")
        buildConfigField("String", "ADMOB_BANNER_UNIT", "\"$admobBannerUnit\"")
        // 테스트 ID로 빌드됐는지 앱이 알 수 있게 해 둡니다 —
        // 설정 화면에서 "테스트 광고" 라고 밝히는 데 씁니다.
        buildConfigField(
            "boolean",
            "ADMOB_TEST_IDS",
            (admobAppId.startsWith("ca-app-pub-3940256099942544")).toString(),
        )
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
