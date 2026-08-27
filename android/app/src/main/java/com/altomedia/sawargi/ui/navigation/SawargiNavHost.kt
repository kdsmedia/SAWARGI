package com.altomedia.sawargi.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            Scaffold(
                modifier = Modifier.padding(),
                content = { padding ->
                    // Login will be implemented in Supabase phase
                    androidx.compose.material3.Text(
                        "Login (Supabase wiring)",
                        modifier = Modifier.padding(padding)
                    )
                }
            )
        }
        composable(Destinations.REGISTER) {
            Scaffold(
                content = { padding ->
                    androidx.compose.material3.Text(
                        "Register (Supabase wiring)",
                        modifier = Modifier.padding(padding)
                    )
                }
            )
        }
        composable(Destinations.HOME) {
            Scaffold(
                content = { padding ->
                    androidx.compose.material3.Text(
                        "Home (feed)",
                        modifier = Modifier.padding(padding)
                    )
                }
            )
        }
    }
}