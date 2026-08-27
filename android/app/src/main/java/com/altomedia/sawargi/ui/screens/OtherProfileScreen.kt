package com.altomedia.sawargi.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.altomedia.sawargi.ui.components.PostItem
import com.altomedia.sawargi.ui.theme.BrandGreen
import com.altomedia.sawargi.ui.theme.BrandGreenDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreen(
    userId: String,
    vm: OtherProfileViewModel = viewModel(),
    onBack: () -> Unit = {},
    onOpenPost: (Long) -> Unit = {},
    onMessage: (String) -> Unit = {},
) {
    val profile by vm.profile.collectAsState()
    val posts by vm.posts.collectAsState()
    val amFollowing by vm.amFollowing.collectAsState()
    val loading by vm.isLoading.collectAsState()

    LaunchedEffect(userId) {
        vm.load(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.displayName ?: "Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    }
                }
            )
        }
    ) { padding ->
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
                    item(key = "header") { ProfileHeader(profile = profile) }
                    item(key = "actions") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (amFollowing) {
                                OutlinedButton(
                                    onClick = { vm.toggleFollow() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Mengikuti") }
                            } else {
                                Button(
                                    onClick = { vm.toggleFollow() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Ikuti") }
                            }
                            OutlinedButton(
                                onClick = { onMessage(profile?.id ?: userId) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Chat") }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    item(key = "stats") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(posts.size, "Postingan")
                        }
                    }
                    item(key = "section") {
                        Text(
                            text = "Postingan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    if (posts.isEmpty()) {
                        item(key = "empty") {
                            Text("Belum ada postingan.", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        items(posts, key = { it.id ?: it.userId }) { post ->
                            PostItem(
                                post = post,
                                onPostClick = onOpenPost,
                                onAvatarClick = { },
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
                    text = profile?.displayName ?: "Pengguna",
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