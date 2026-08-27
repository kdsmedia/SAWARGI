package com.altomedia.sawargi.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.altomedia.sawargi.data.Post
import com.altomedia.sawargi.data.ReactionType
import com.altomedia.sawargi.ui.theme.BrandGreen
import com.altomedia.sawargi.ui.util.relativeTime

/**
 * Reusable post card used by the home feed, post detail and profile screens.
 */
@Composable
fun PostItem(
    post: Post,
    modifier: Modifier = Modifier,
    showAllActions: Boolean = true,
    onPostClick: (Long) -> Unit = {},
    onCommentClick: (Long) -> Unit = {},
    onReact: (Post, ReactionType) -> Unit = { _, _ -> },
    onSave: (Post) -> Unit = {},
    onAvatarClick: (String) -> Unit = {},
) {
    val postId = post.id
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (postId != null) Modifier.clickable { onPostClick(postId) } else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    avatar = post.author?.avatar,
                    name = post.author?.displayName,
                    size = 40,
                    onClick = { post.author?.id?.let(onAvatarClick) }
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.author?.displayName ?: "Pengguna",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.author?.verified == true) {
                            Spacer(Modifier.width(4.dp))
                            Text("✓", color = BrandGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = relativeTime(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!post.feeling.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "merasa ${post.feeling}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!post.text.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(text = post.text, style = MaterialTheme.typography.bodyMedium)
            }

            if (!post.media.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = post.media,
                    contentDescription = "Media",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }

            if (!post.link.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                LinkCard(post = post)
            }

            Spacer(Modifier.height(8.dp))
            Row {
                val mine = ReactionType.from(post.myReaction)
                Text(
                    text = "${post.reactionsCount} " + (mine?.emoji ?: "\uD83D\uDC4D"),
                    style = MaterialTheme.typography.bodySmall
                )
                if (post.commentsCount > 0) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${post.commentsCount} Komentar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showAllActions) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val mine = ReactionType.from(post.myReaction)
                    ActionItem(
                        label = mine?.emoji ?: "\uD83D\uDC4D",
                        text = mine?.label ?: "Suka",
                        activated = mine != null,
                        onClick = { onReact(post, reactionCycle(post.myReaction)) }
                    )
                    ActionItem(
                        label = "\uD83D\uDCAC",
                        text = "Komentar",
                        onClick = { postId?.let(onCommentClick) }
                    )
                    ActionItem(
                        label = "\uD83D\uDD16",
                        text = "Simpan",
                        onClick = { onSave(post) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkCard(post: Post) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                post.link?.let {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                }
            }
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!post.linkImage.isNullOrBlank()) {
                AsyncImage(
                    model = post.linkImage,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small)
                )
                Spacer(Modifier.width(8.dp))
            }
            Column {
                if (!post.linkTitle.isNullOrBlank()) {
                    Text(
                        text = post.linkTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = post.link ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Avatar(
    avatar: String?,
    name: String?,
    size: Int,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!avatar.isNullOrBlank()) {
            AsyncImage(
                model = avatar,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp)
            )
        } else {
            Text(
                text = (name ?: "?").firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ActionItem(
    label: String,
    text: String,
    onClick: () -> Unit,
    activated: Boolean = false,
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (activated) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Cycles through reactions starting from my current one. */
private fun reactionCycle(current: String?): ReactionType {
    val all = ReactionType.entries
    val idx = ReactionType.from(current)?.ordinal
    return if (idx != null) all[(idx + 1) % all.size] else all[0]
}