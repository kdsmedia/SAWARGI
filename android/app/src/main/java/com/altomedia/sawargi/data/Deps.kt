package com.altomedia.sawargi.data

/**
 * Simple manual DI container (avoids adding a DI framework).
 */
object Deps {
    val authRepository by lazy { AuthRepository() }
    val socialRepository by lazy { SocialRepository() }
    val chatRepository by lazy { ChatRepository(socialRepository) }
}