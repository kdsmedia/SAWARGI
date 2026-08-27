package com.altomedia.sawargi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.altomedia.sawargi.data.Comment
import com.altomedia.sawargi.ui.components.PostItem
import com.altomedia.sawargi.ui.util.relativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Long,
    initialPost: com.altomedia.sawargi.data.Post? = null,
    vm: PostDetailViewModel = viewModel(),
    onBack: () -> Unit = {},
    onAvatarClick: (String) -> Unit = {},
) {
    val post by vm.post.collectAsState()
    val comments by vm.comments.collectAsState()
    val newComment by vm.newComment.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val me = com.altomedia.sawargi.data.Deps.socialRepository.currentUserId()

    vm.setPost(initialPost ?: post)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Postingan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    }
                }
            )
        },
        bottomBar = {
            CommentInput(
                value = newComment,
                onValueChange = vm::onCommentTextChange,
                onSend = { vm.submitComment() }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (post == null && loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    post?.let { p ->
                        item(key = "post") {
                            Column {
                                PostItem(
                                    post = p,
                                    onCommentClick = {},
                                    onReact = { _, _ -> },
                                    onAvatarClick = onAvatarClick
                                )
                                Spacer(Modifier.height(12.dp))
                                AdBanner(modifier = Modifier.fillMaxWidth())
                            }
                        }
                        item(key = "comments_header") {
                            Text(
                                text = "Komentar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(comments, key = { it.id ?: it.userId }) { comment ->
                        CommentRow(comment = comment, isMine = comment.userId == me) {
                            vm.deleteComment(comment)
                        }
                    }
                    item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment, isMine: Boolean, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (!comment.author?.avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = comment.author?.avatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = (comment.author?.displayName ?: "?").firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.author?.displayName ?: "Pengguna",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = relativeTime(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
            }
            if (isMine) {
                Text(
                    text = "Hapus",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onDelete)
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun CommentInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Tulis komentar...") },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Kirim",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onSend)
                .padding(10.dp)
        )
    }
}