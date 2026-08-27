package com.altomedia.sawargi.data

import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put

/** Json used for Postgrest bodies. */
internal val supabaseJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Authentication & profile repository backed by Supabase Auth + the public
 * `profiles` table.
 *
 * The app uses phone-only login. A phone number is mapped to a synthetic email
 * (`<phone>@sawargi.app`) so the standard GoTrue **Email** provider can be used
 * for [loginWithPhone] / [registerWithPhone] — the Phone provider would require
 * SMS/OTP flows. Users never see or type an email.
 */
class AuthRepository {

    companion object {
        const val EMAIL_DOMAIN = "sawargi.app"
        fun syntheticEmail(phone: String): String = phone.trim() + "@" + EMAIL_DOMAIN
    }

    /**
     * Emits the app-level auth state based on the current Supabase session status.
     * Collect this from a ViewModel/LaunchedEffect.
     */
    val authState = flow {
        while (true) {
            val uid = Supabase.auth.currentSessionOrNull()?.user?.id
            emit(if (uid != null) AuthState.LoggedIn(uid) else AuthState.LoggedOut)
            kotlinx.coroutines.delay(2000)
        }
    }

    /** Sign in with phone + password, mapped to a synthetic email under [EMAIL_DOMAIN]. */
    suspend fun loginWithPhone(phone: String, password: String): Result<AuthState> = runCatching {
        Supabase.auth.signInWith(Email) {
            this.email = syntheticEmail(phone)
            this.password = password
        }
        val userId = Supabase.auth.currentSessionOrNull()?.user?.id ?: error("No session")
        AuthState.LoggedIn(userId)
    }

    /**
     * Register with phone + password + display name. The phone number is used
     * as the account identity (synthetic email), so existing accounts under the
     * same phone cannot be re-registered.
     */
    suspend fun registerWithPhone(
        phone: String,
        password: String,
        fullName: String,
    ): Result<AuthState> = runCatching {
        val email = syntheticEmail(phone)
        Supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = buildJsonObject { put("full_name", fullName); put("phone", phone) }
        }
        val userId = Supabase.auth.currentSessionOrNull()?.user?.id
            ?: throw IllegalStateException("Pendaftaran memerlukan verifikasi oleh admin.")
        // Seed the profile row
        val profile = Profile(
            id = userId,
            fullName = fullName,
            phone = phone,
            email = email,
        )
        Supabase.postgrest.from("profiles").upsert(
            supabaseJson.encodeToJsonElement(profile).jsonArray
        )
        AuthState.LoggedIn(userId)
    }

    suspend fun signOut() = runCatching {
        Supabase.auth.signOut()
        AuthState.LoggedOut
    }

    suspend fun fetchProfile(userId: String): Result<Profile?> = runCatching {
        Supabase.postgrest
            .from("profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingleOrNull<Profile>()
    }

    suspend fun updateProfile(profile: Profile): Result<Unit> = runCatching {
        Supabase.postgrest.from("profiles").upsert(
            supabaseJson.encodeToJsonElement(profile).jsonArray
        )
    }
}