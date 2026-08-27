package com.altomedia.sawargi.data

import android.content.Context
import com.altomedia.sawargi.R
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Central Supabase client for SAWARGI.
 * Uses the publishable/anon key from strings.xml (intended to be client-side).
 */
object Supabase {

    lateinit var client: io.github.jan.supabase.SupabaseClient
        private set

    fun init(context: Context) {
        if (::client.isInitialized) return
        val url = context.getString(R.string.supabase_url)
        val key = context.getString(R.string.supabase_anon_key)
        client = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    val auth: Auth get() = client.auth
    val postgrest: Postgrest get() = client.postgrest
    val realtime: Realtime get() = client.realtime
    val storage: Storage get() = client.storage

    fun currentSessionUserId(): String? = auth.currentSessionOrNull()?.user?.id
}

/**
 * Simple auth UI state exposed by repositories.
 */
sealed interface AuthState {
    data object Loading : AuthState
    data class LoggedIn(val userId: String) : AuthState
    data object LoggedOut : AuthState
    data class Error(val message: String) : AuthState
}