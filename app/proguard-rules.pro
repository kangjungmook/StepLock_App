# 릴리스 빌드는 R8 축소·난독화를 켜고 씁니다.
# 아래는 리플렉션이나 생성된 코드를 쓰는 라이브러리 때문에 필요한 규칙입니다.

# ── kotlinx.serialization ───────────────────────────────────────────────
# 컴파일러 플러그인이 각 @Serializable 클래스마다 Companion.serializer() 를
# 만들어 두고 리플렉션으로 찾습니다. 이름이 바뀌면 런타임에 실패합니다.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 앱에서 직접 직렬화하는 모델 (SyncRepository 의 행 타입들)
-keep,includedescriptorclasses class com.steplock.app.**$$serializer { *; }
-keepclassmembers class com.steplock.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.steplock.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── ktor / okhttp ──────────────────────────────────────────────────────
# okhttp·okio 는 자체 consumer 규칙을 갖고 있어 추가 설정이 거의 없지만,
# 플랫폼별 선택 코드가 없는 클래스를 참조해서 경고가 납니다.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn kotlinx.coroutines.debug.**

# ktor 는 엔진을 ServiceLoader 로 찾습니다.
-keep class io.ktor.client.engine.okhttp.** { *; }
-dontwarn io.ktor.**

# ── Health Connect ─────────────────────────────────────────────────────
# 레코드 타입을 리플렉션으로 매핑합니다.
-keep class androidx.health.connect.client.records.** { *; }

# ── 디버깅용 ────────────────────────────────────────────────────────────
# 크래시 스택에서 줄 번호를 읽을 수 있게 남기고, 원본 파일명은 숨깁니다.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
