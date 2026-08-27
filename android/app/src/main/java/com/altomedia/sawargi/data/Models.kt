package com.altomedia.sawargi.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Reaction types used in WoWonder -> SAWARGI. */
enum class ReactionType(val value: String, val emoji: String, val label: String) {
    LIKE("like", "\uD83D\uDC4D", "Suka"),
    LOVE("love", "\uD83D\uDC96", "Cinta"),
    HAHA("haha", "\uD83D\uDE02", "Haha"),
    WOW("wow", "\uD83D\uDE32", "Wow"),
    SAD("sad", "\uD83D\uDE22", "Sedih"),
    ANGRY("angry", "\uD83D\uDE21", "Marah");

    companion object {
        fun from(value: String?): ReactionType? = entries.firstOrNull { it.value == value }
    }
}

/** User profile stored in the public `profiles` table. */
@Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val cover: String? = null,
    val bio: String? = null,
    val gender: String? = null,
    val birthday: String? = null,
    val address: String? = null,
    val country: String? = null,
    val verified: Boolean = false,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_pro") val isPro: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
) {
    val displayName: String
        get() = fullName?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: "Pengguna " + id.take(4)
}

/** A timeline post. */
@Serializable
data class Post(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    val text: String? = null,
    val media: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    val link: String? = null,
    @SerialName("link_title") val linkTitle: String? = null,
    @SerialName("link_image") val linkImage: String? = null,
    val feeling: String? = null,
    val type: String? = null,
    @SerialName("shared_post_id") val sharedPostId: Long? = null,
    @SerialName("post_privacy") val postPrivacy: Int = 0,
    val boosted: Boolean = false,
    @SerialName("reactions_count") val reactionsCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("shares_count") val sharesCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    /** Filled client-side from the profiles table (not part of `posts`). */
    val author: Profile? = null,
    /** True when the current user reacted to this post (client-side). */
    @SerialName("my_reaction") val myReaction: String? = null,
)

/** A comment on a post. */
@Serializable
data class Comment(
    val id: Long? = null,
    @SerialName("post_id") val postId: Long,
    @SerialName("user_id") val userId: String,
    val text: String,
    @SerialName("created_at") val createdAt: String? = null,
    val author: Profile? = null,
)

/** A reply to a comment. */
@Serializable
data class CommentReply(
    val id: Long? = null,
    @SerialName("comment_id") val commentId: Long,
    @SerialName("user_id") val userId: String,
    val text: String,
    @SerialName("created_at") val createdAt: String? = null,
    val author: Profile? = null,
)

/** A reaction row (post or comment). */
@Serializable
data class PostReaction(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("post_id") val postId: Long? = null,
    @SerialName("comment_id") val commentId: Long? = null,
    val type: String,
    @SerialName("created_at") val createdAt: String? = null,
)

/** Follow / friend-request row (Wo_Followers equivalent). */
@Serializable
data class Following(
    val id: Long? = null,
    val follower: String,
    val following: String,
    val status: String = "accepted",
    @SerialName("created_at") val createdAt: String? = null,
)

/** Bookmark / saved post. */
@Serializable
data class SavedPost(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("post_id") val postId: Long,
    @SerialName("created_at") val createdAt: String? = null,
)

/** A direct-message conversation (unique ordered pair of users). */
@Serializable
data class Conversation(
    val id: Long? = null,
    @SerialName("user_a") val userA: String,
    @SerialName("user_b") val userB: String,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** A direct message. */
@Serializable
data class Message(
    val id: Long? = null,
    @SerialName("conversation_id") val conversationId: Long,
    @SerialName("sender_id") val senderId: String,
    val text: String? = null,
    val media: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    val type: String = "text",
    val seen: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

/** A notification. */
@Serializable
data class Notification(
    val id: Long? = null,
    val recipient: String,
    val actor: String? = null,
    val type: String,
    @SerialName("post_id") val postId: Long? = null,
    @SerialName("comment_id") val commentId: Long? = null,
    val text: String? = null,
    val url: String? = null,
    val seen: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    val actorProfile: Profile? = null,
)

/** A story (ephemeral media post). */
@Serializable
data class Story(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    val media: String,
    @SerialName("media_type") val mediaType: String = "image",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val author: Profile? = null,
)

/** Search result / suggestion (a profile row plus follow relation flags). */
@Serializable
data class UserListItem(
    val profile: Profile,
    val mutual: Int = 0,
    val following: Boolean? = null,
)