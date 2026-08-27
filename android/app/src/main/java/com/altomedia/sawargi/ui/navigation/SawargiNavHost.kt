package com.altomedia.sawargi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.altomedia.sawargi.data.AuthState
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.ui.screens.AuthScreen
import com.altomedia.sawargi.ui.screens.ConversationScreen
import com.altomedia.sawargi.ui.screens.CreatePostScreen
import com.altomedia.sawargi.ui.screens.EditProfileScreen
import com.altomedia.sawargi.ui.screens.MainShellScreen
import com.altomedia.sawargi.ui.screens.OtherProfileScreen
import com.altomedia.sawargi.ui.screens.PostDetailScreen
import com.altomedia.sawargi.ui.screens.ProfileScreen
import com.altomedia.sawargi.ui.screens.SavedPostsScreen
import com.altomedia.sawargi.ui.screens.WelcomeScreen
import kotlinx.coroutines.launch

/**
 * Root navigation graph for SAWARGI.
 *
 * Routes:
 *  - welcome / login / register  : pre-auth
 *  - main                        : bottom-nav shell (Home, Jelajahi, Buat, Chat, Profil)
 *  - post/{postId}               : post detail
 *  - profile/{userId}            : own ProfileScreen when userId == me, else OtherProfileScreen
 *  - edit-profile / saved        : profile utilities
 *  - chat/{other}                : direct-message conversation
 */
object SawargiRoutes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val POST = "post/{postId}"
    const val PROFILE = "profile/{userId}"
    const val EDIT_PROFILE = "edit-profile"
    const val SAVED = "saved"
    const val CHAT = "chat/{other}"

    fun post(postId: Long): String = "post/$postId"
    fun profile(userId: String): String = "profile/$userId"
    fun chat(other: String): String = "chat/$other"
}

@Composable
fun SawargiNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val scope = rememberCoroutineScope()

    // Auto-redirect to the shell whenever the user becomes authenticated.
    val authState by Deps.authRepository.authState.collectAsState(initial = AuthState.Loading)
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn) {
            navController.navigate(SawargiRoutes.MAIN) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun goToMain() {
        navController.navigate(SawargiRoutes.MAIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = SawargiRoutes.WELCOME) {

        composable(SawargiRoutes.WELCOME) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(SawargiRoutes.LOGIN) },
                onRegisterClick = { navController.navigate(SawargiRoutes.REGISTER) }
            )
        }

        composable(SawargiRoutes.LOGIN) {
            AuthScreen(
                isRegister = false,
                onSuccess = { goToMain() },
                onSwitchMode = { navController.navigate(SawargiRoutes.REGISTER) {
                    popUpTo(SawargiRoutes.LOGIN) { inclusive = true }
                } }
            )
        }

        composable(SawargiRoutes.REGISTER) {
            AuthScreen(
                isRegister = true,
                onSuccess = { goToMain() },
                onSwitchMode = { navController.navigate(SawargiRoutes.LOGIN) {
                    popUpTo(SawargiRoutes.REGISTER) { inclusive = true }
                } }
            )
        }

        composable(SawargiRoutes.MAIN) {
            MainShellScreen(
                onLogout = {
                    scope.launch {
                        Deps.authRepository.signOut()
                        navController.navigate(SawargiRoutes.WELCOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onOpenPost = { postId -> navController.navigate(SawargiRoutes.post(postId)) },
                onOpenUser = { userId -> navController.navigate(SawargiRoutes.profile(userId)) },
                onOpenConversation = { other -> navController.navigate(SawargiRoutes.chat(other)) },
                onOpenEditProfile = { navController.navigate(SawargiRoutes.EDIT_PROFILE) },
                onOpenSaved = { navController.navigate(SawargiRoutes.SAVED) }
            )
        }

        composable(
            route = SawargiRoutes.POST,
            arguments = listOf(navArgument("postId") { type = NavType.LongType })
        ) { entry ->
            val postId = entry.arguments?.getLong("postId") ?: 0L
            PostDetailScreen(
                postId = postId,
                onBack = { navController.popBackStack() },
                onAvatarClick = { userId ->
                    navController.navigate(SawargiRoutes.profile(userId))
                }
            )
        }

        composable(
            route = SawargiRoutes.PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { entry ->
            val userId = entry.arguments?.getString("userId").orEmpty()
            val me = Deps.socialRepository.currentUserId()
            if (userId.isNotBlank() && userId == me) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(SawargiRoutes.EDIT_PROFILE) },
                    onLogout = {
                        scope.launch {
                            Deps.authRepository.signOut()
                            navController.navigate(SawargiRoutes.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onOpenPost = { postId ->
                        navController.navigate(SawargiRoutes.post(postId))
                    },
                    onOpenUser = { uid ->
                        if (uid != me) navController.navigate(SawargiRoutes.profile(uid))
                    },
                    onOpenSaved = { navController.navigate(SawargiRoutes.SAVED) }
                )
            } else if (userId.isNotBlank()) {
                OtherProfileScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onOpenPost = { postId ->
                        navController.navigate(SawargiRoutes.post(postId))
                    },
                    onMessage = { other ->
                        navController.navigate(SawargiRoutes.chat(other))
                    }
                )
            } else {
                ProfileScreen(
                    onEditProfile = { navController.navigate(SawargiRoutes.EDIT_PROFILE) },
                    onLogout = {
                        scope.launch {
                            Deps.authRepository.signOut()
                            navController.navigate(SawargiRoutes.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onOpenPost = { postId ->
                        navController.navigate(SawargiRoutes.post(postId))
                    },
                    onOpenUser = { uid ->
                        if (uid != me) navController.navigate(SawargiRoutes.profile(uid))
                    },
                    onOpenSaved = { navController.navigate(SawargiRoutes.SAVED) }
                )
            }
        }

        composable(SawargiRoutes.EDIT_PROFILE) {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(SawargiRoutes.SAVED) {
            SavedPostsScreen(
                onBack = { navController.popBackStack() },
                onOpenPost = { postId ->
                    navController.navigate(SawargiRoutes.post(postId))
                }
            )
        }

        composable(
            route = SawargiRoutes.CHAT,
            arguments = listOf(navArgument("other") { type = NavType.StringType })
        ) { entry ->
            val other = entry.arguments?.getString("other").orEmpty()
            ConversationScreen(
                otherUserId = other,
                onBack = { navController.popBackStack() }
            )
        }
    }
}