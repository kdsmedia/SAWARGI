package com.altomedia.sawargi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.altomedia.sawargi.data.Post
import com.altomedia.sawargi.ui.components.PostItem
import com.altomedia.sawargi.ui.theme.BrandGreen
import com.altomedia.sawargi.ui.theme.BrandGreenDark

@Composable
fun ProfileScreen(
    vm: ProfileViewModel = viewModel(),
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenPost: (Long) -> Unit = {},
    onOpenUser: (String) -> Unit = {},
    onOpenSaved: (() -> Unit)? = null,
) {
    val profile by vm.profile.collectAsState()
    val myPosts by vm.myPosts.collectAsState()
    val followers by vm.followers.collectAsState()
    val following by vm.following.collectAsState()
    val loading by vm.isLoading.collectAsState()

    vm.load()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading && profile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "header") {
                        ProfileHeader(profile = profile)
                    }
                    item(key = "buttons") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onEditProfile,
                                modifier = Modifier.weight(1f)
                            ) { Text("Edit Profil") }
                            OutlinedButton(
                                onClick = onLogout,
                                modifier = Modifier.weight(1f)
                            ) { Text("Keluar") }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (onOpenSaved != null) {
                            Text(
                                text = "Lihat Postingan Tersimpan",
                                style = MaterialTheme.typography.labelLarge,
                                color = BrandGreen,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(onClick = onOpenSaved)
                                    .padding(4.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(myPosts.size, "Postingan")
                            StatItem(followers.size, "Pengikut")
                            StatItem(following.size, "Mengikuti")
                        }
                    }
                    item(key = "section") {
                        Text(
                            text = "Postingan Saya",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    if (myPosts.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = "Belum ada postingan.",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(myPosts, key = { it.id ?: it.userId }) { post ->
                            PostItem(
                                post = post,
                                showAllActions = true,
                                onPostClick = onOpenPost,
                                onAvatarClick = onOpenUser,
                                onReact = vm::toggleReaction,
                                onSave = vm::toggleSave
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: com.altomedia.sawargi.data.Profile?) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Brush.horizontalGradient(listOf(BrandGreen, BrandGreenDark)))
        ) {
            if (!profile?.cover.isNullOrBlank()) {
                AsyncImage(
                    model = profile?.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (!profile?.avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.avatar,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = (profile?.displayName ?: "?").firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile?.displayName ?: "Saya",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (profile?.verified == true) {
                    Spacer(Modifier.width(6.dp))
                    Text("✓", color = BrandGreen, fontWeight = FontWeight.Bold)
                }
            }
            if (!profile?.username.isNullOrBlank()) {
                Text(
                    text = "@${profile?.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!profile?.bio.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(text = profile?.bio ?: "", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}