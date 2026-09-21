package com.steplock.app.data

import com.steplock.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow

/** OAuth 콜백으로 돌아올 딥링크. AndroidManifest의 intent-filter와 같아야 합니다. */
const val AUTH_CALLBACK_SCHEME = "steplock"
const val AUTH_CALLBACK_HOST = "login-callback"

object SupabaseProvider {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth) {
                scheme = AUTH_CALLBACK_SCHEME
                host = AUTH_CALLBACK_HOST
            }
            install(Postgrest)
        }
    }
}

/**
 * 세션은 Supabase가 들고 있고(앱 재시작 시 자동 복구), 우리 쪽은 accountId만 받아
 * 기기 UUID 레코드를 계정에 귀속시킵니다.
 * 소셜 로그인은 브라우저를 열고 딥링크로 돌아오므로 결과는 sessionStatus로 들어옵니다.
 */
class AuthRepository {

    private val auth = SupabaseProvider.client.auth

    val sessionStatus: Flow<SessionStatus> = auth.sessionStatus

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    suspend fun signInWithGoogle(): Result<Unit> = runCatching { auth.signInWith(Google) }

    suspend fun signInWithKakao(): Result<Unit> = runCatching { auth.signInWith(Kakao) }

    suspend fun signInWithApple(): Result<Unit> = runCatching { auth.signInWith(Apple) }

    suspend fun signOut(): Result<Unit> = runCatching { auth.signOut() }

    /** 재설정 링크는 딥링크로 앱으로 돌아옵니다. */
    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.resetPasswordForEmail(
            email = email,
            redirectUrl = "$AUTH_CALLBACK_SCHEME://$AUTH_CALLBACK_HOST",
        )
    }
}
