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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.altomedia.sawargi.ads.AdBanner
import com.altomedia.sawargi.data.Post
import com.altomedia.sawargi.data.ReactionType
import com.altomedia.sawargi.data.Story
import com.altomedia.sawargi.ui.components.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel = viewModel(),
    onPostClick: (Long) -> Unit = {},
    onCommentClick: (Long) -> Unit = {},
    onAvatarClick: (String) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
) {
    val feed by vm.feed.collectAsState()
    val stories by vm.stories.collectAsState()
    val saved by vm.savedCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SAWARGI", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AdBanner(modifier = Modifier.fillMaxWidth())
            when (val state = feed) {
                is FeedState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is FeedState.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("Gagal memuat feed: ${state.message}") }

                is FeedState.Loaded -> {
                    if (state.posts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { Text("Belum ada postingan. Mulai buat status pertama kamu!") }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (stories.isNotEmpty()) {
                                item(key = "stories") {
                                    StoryRow(
                                        stories = stories,
                                        onAvatarClick = onAvatarClick
                                    )
                                }
                            }
                            items(state.posts, key = { it.id ?: it.userId }) { post ->
                                PostItem(
                                    post = post,
                                    onPostClick = onPostClick,
                                    onCommentClick = onCommentClick,
                                    onReact = vm::toggleReaction,
                                    onSave = vm::toggleSave,
                                    onAvatarClick = onAvatarClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryRow(stories: List<Story>, onAvatarClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stories, key = { it.id ?: it.userId }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { story.author?.id?.let(onAvatarClick) }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    if (!story.author?.avatar.isNullOrBlank()) {
                        AsyncImage(
                            model = story.author?.avatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        androidx.compose.material3.Text(
                            text = (story.author?.displayName ?: "?").firstOrNull()?.uppercase() ?: "?",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = story.author?.displayName?.take(8) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}