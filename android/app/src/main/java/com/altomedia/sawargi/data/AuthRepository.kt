package com.altomedia.sawargi.data

import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.Phone
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray

/** Json used for Postgrest bodies. */
internal val supabaseJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Authentication & profile repository backed by Supabase Auth + the public
 * `profiles` table.
 */
class AuthRepository {

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

    /** Sign in with email + password (GoTrue built-in provider). */
    suspend fun loginWithEmail(email: String, password: String): Result<AuthState> = runCatching {
        Supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = Supabase.auth.currentSessionOrNull()?.user?.id ?: error("No session")
        AuthState.LoggedIn(userId)
    }

    /** Sign in with phone + password (GoTrue phone provider). */
    suspend fun loginWithPhone(phone: String, password: String): Result<AuthState> = runCatching {
        Supabase.auth.signInWith(Phone) {
            this.phone = phone
            this.password = password
        }
        val userId = Supabase.auth.currentSessionOrNull()?.user?.id ?: error("No session")
        AuthState.LoggedIn(userId)
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        fullName: String,
        phone: String,
    ): Result<AuthState> = runCatching {
        Supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = Supabase.auth.currentSessionOrNull()?.user?.id
            ?: throw IllegalStateException("Pendaftaran memerlukan verifikasi email oleh admin.")
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