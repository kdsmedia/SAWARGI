package com.altomedia.sawargi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.altomedia.sawargi.ui.screens.AuthScreen
import com.altomedia.sawargi.ui.screens.HomeScreen
import com.altomedia.sawargi.ui.screens.WelcomeScreen

/** Navigation destinations for SAWARGI. */
object Destinations {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val CHAT = "chat"
}

@Composable
fun SawargiNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Destinations.WELCOME
    ) {
        composable(Destinations.WELCOME) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(Destinations.LOGIN) },
                onRegisterClick = { navController.navigate(Destinations.REGISTER) }
            )
        }
        composable(Destinations.LOGIN) {
            AuthScreen(
                isRegister = false,
                onSuccess = { navController.navigate(Destinations.HOME) { popUpTo(Destinations.WELCOME) { inclusive = true } } },
                onSwitchMode = { navController.navigate(Destinations.REGISTER) { popUpTo(Destinations.WELCOME) { inclusive = true } } }
            )
        }
        composable(Destinations.REGISTER) {
            AuthScreen(
                isRegister = true,
                onSuccess = { navController.navigate(Destinations.HOME) { popUpTo(Destinations.WELCOME) { inclusive = true } } },
                onSwitchMode = { navController.navigate(Destinations.LOGIN) { popUpTo(Destinations.WELCOME) { inclusive = true } } }
            )
        }
        composable(Destinations.HOME) {
            HomeScreen()
        }
    }
}