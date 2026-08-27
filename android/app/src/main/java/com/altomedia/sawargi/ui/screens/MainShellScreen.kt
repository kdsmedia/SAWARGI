package com.altomedia.sawargi.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Root shell hosting the 5 main tabs with a bottom navigation bar.
 * Navigation callbacks forward up to the outer NavHost.
 */
@Composable
fun MainShellScreen(
    onLogout: () -> Unit,
    onOpenPost: (Long) -> Unit = {},
    onOpenUser: (String) -> Unit = {},
    onOpenConversation: (String) -> Unit = {},
    onOpenEditProfile: () -> Unit = {},
    onOpenSaved: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val homeVm: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val profileVm: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = "Jelajahi") },
                    label = { Text("Jelajahi") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Buat") },
                    label = { Text("Buat") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Send, contentDescription = "Chat") },
                    label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profil") },
                    label = { Text("Profil") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    vm = homeVm,
                    onPostClick = onOpenPost,
                    onCommentClick = onOpenPost,
                    onAvatarClick = onOpenUser
                )

                1 -> SearchScreen(onOpenUser = onOpenUser)

                2 -> CreatePostScreen(onPosted = { selectedTab = 0 })

                3 -> ChatListScreen(onOpenConversation = onOpenConversation)

                4 -> ProfileScreen(
                    vm = profileVm,
                    onEditProfile = onOpenEditProfile,
                    onLogout = onLogout,
                    onOpenPost = onOpenPost,
                    onOpenUser = onOpenUser,
                    onOpenSaved = onOpenSaved
                )
            }
        }
    }
}