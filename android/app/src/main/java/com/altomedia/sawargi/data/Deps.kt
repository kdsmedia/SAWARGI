package com.altomedia.sawargi.data

/**
 * Simple manual DI container (avoids adding a DI framework).
 */
object Deps {
    val authRepository by lazy { AuthRepository() }
    val postRepository by lazy { PostRepository() }
}