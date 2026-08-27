package com.altomedia.sawargi.data

import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/**
 * Repository for the social features of SAWARGI: profiles, feed, posts,
 * reactions, comments, follows, bookmarks, search and notifications —
 * backed by Supabase Postgrest.
 */
class SocialRepository {

    fun currentUserId(): String? = Supabase.currentSessionUserId()

    // ------------------------------------------------------------------
    // Profiles
    // ------------------------------------------------------------------
    suspend fun fetchProfile(id: String): Result<Profile?> = runCatching {
        Supabase.postgrest.from("profiles")
            .select {
                filter { eq("id", id) }
            }
            .decodeSingleOrNull<Profile>()
    }

    suspend fun updateProfile(profile: Profile): Result<Unit> = runCatching {
        Supabase.postgrest.from("profiles").update(profile) {
            filter { eq("id", profile.id) }
        }
    }

    // ------------------------------------------------------------------
    // Feed & posts
    // ------------------------------------------------------------------
    private suspend fun attachAuthors(posts: List<Post>): List<Post> {
        if (posts.isEmpty()) return posts
        val ids = posts.map { it.userId }.distinct()
        val profiles = runCatching {
            val joined = ids.joinToString(",") { "'$it'" }
            Supabase.postgrest.from("profiles")
                .select(Columns.raw("id, full_name, username, avatar")) { }
                .decodeList<Profile>()
                .filter { it.id in ids }
        }.getOrDefault(emptyList())
        val byId = profiles.associateBy { it.id }
        return posts.map { it.copy(author = byId[it.userId]) }
    }

    suspend fun fetchFeed(limit: Long = 50): Result<List<Post>> = runCatching {
        val posts = Supabase.postgrest.from("posts")
            .select(Columns.raw("*, author:profiles!user_id(id, full_name, username, avatar)")) {
                filter { eq("post_privacy", 0) } // public posts only in the feed
                order("created_at", Order.DESCENDING)
                limit(limit)
            }
            .decodeList<Post>()
        posts
    }

    suspend fun fetchProfilePosts(userId: String, limit: Long = 200): Result<List<Post>> = runCatching {
        val posts = Supabase.postgrest.from("posts")
            .select(Columns.raw("*, author:profiles!user_id(id, full_name, username, avatar)")) {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(limit)
            }
            .decodeList<Post>()
        posts
    }

    suspend fun createPost(
        userId: String,
        text: String?,
        media: String? = null,
        mediaType: String = "image",
        type: String = "status",
    ): Result<Post> = runCatching {
        val post = Post(
            userId = userId,
            text = text?.takeIf { it.isNotBlank() },
            media = media,
            mediaType = mediaType,
            type = type,
        )
        val inserted = Supabase.postgrest.from("posts")
            .insert(post)
            .decodeSingle<Post>()
        inserted
    }

    suspend fun deletePost(postId: Long): Result<Unit> = runCatching {
        Supabase.postgrest.from("posts").delete {
            filter { eq("id", postId) }
        }
    }

    // ------------------------------------------------------------------
    // Reactions
    // ------------------------------------------------------------------
    /** Returns the current user's reactions on the feed (bounded) for client-side checks. */
    suspend fun fetchMyReactions(userId: String, limit: Long = 200): Result<List<PostReaction>> = runCatching {
        Supabase.postgrest.from("post_reactions")
            .select(Columns.raw("id, user_id, post_id, type")) {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(limit)
            }
            .decodeList<PostReaction>()
    }

    /**
     * Toggles a reaction on a post. If the post already has the same reaction
     * by the current user it is removed; otherwise it is replaced/added.
     * Returns the new reaction or null when removed.
     */
    suspend fun toggleReaction(userId: String, postId: Long, reaction: ReactionType): Result<PostReaction?> = runCatching {
        val existing = Supabase.postgrest.from("post_reactions")
            .select(Columns.raw("id, user_id, post_id, type")) {
                filter { eq("user_id", userId) }
                filter { eq("post_id", postId) }
            }
            .decodeList<PostReaction>()

        val mine = existing.firstOrNull()
        if (mine != null && mine.type == reaction.value) {
            // Remove the reaction
            Supabase.postgrest.from("post_reactions").delete {
                filter { eq("id", mine.id!!) }
            }
            null
        } else {
            // Remove any previous reaction, then add the new one
            if (mine != null) {
                Supabase.postgrest.from("post_reactions").delete {
                    filter { eq("id", mine.id!!) }
                }
            }
            val newReaction = PostReaction(userId = userId, postId = postId, type = reaction.value)
            Supabase.postgrest.from("post_reactions").insert(newReaction)
            newReaction
        }
    }

    // ------------------------------------------------------------------
    // Comments + replies
    // ------------------------------------------------------------------
    suspend fun fetchComments(postId: Long): Result<List<Comment>> = runCatching {
        val comments = Supabase.postgrest.from("comments")
            .select(Columns.raw("*, author:profiles!user_id(id, full_name, username, avatar)")) {
                filter { eq("post_id", postId) }
                order("created_at", Order.ASCENDING)
                limit(500)
            }
            .decodeList<Comment>()
        comments
    }

    suspend fun addComment(userId: String, postId: Long, text: String): Result<Comment> = runCatching {
        val comment = Comment(postId = postId, userId = userId, text = text)
        val inserted = Supabase.postgrest.from("comments")
            .insert(comment)
            .decodeSingle<Comment>()
        inserted
    }

    suspend fun deleteComment(commentId: Long): Result<Unit> = runCatching {
        Supabase.postgrest.from("comments").delete {
            filter { eq("id", commentId) }
        }
    }

    suspend fun addReply(userId: String, commentId: Long, text: String): Result<CommentReply> = runCatching {
        val reply = CommentReply(commentId = commentId, userId = userId, text = text)
        Supabase.postgrest.from("comment_replies")
            .insert(reply)
            .decodeSingle<CommentReply>()
    }

    // ------------------------------------------------------------------
    // Follows
    // ------------------------------------------------------------------
    suspend fun followToggle(follower: String, following: String): Result<Boolean> = runCatching {
        val existing = Supabase.postgrest.from("followers")
            .select(Columns.raw("id, follower, following, status")) {
                filter { eq("follower", follower) }
                filter { eq("following", following) }
            }
            .decodeList<Following>()
        if (existing.isNotEmpty()) {
            Supabase.postgrest.from("followers").delete {
                filter { eq("id", existing.first().id!!) }
            }
            false // now not following
        } else {
            Supabase.postgrest.from("followers")
                .insert(Following(follower = follower, following = following))
            true // now following
        }
    }

    suspend fun isFollowing(follower: String, following: String): Result<Boolean> = runCatching {
        Supabase.postgrest.from("followers")
            .select(Columns.raw("id")) {
                filter { eq("follower", follower) }
                filter { eq("following", following) }
                limit(1)
            }
            .decodeList<Following>()
            .isNotEmpty()
    }

    suspend fun fetchFollowers(userId: String): Result<List<Profile>> = runCatching {
        Supabase.postgrest.from("followers")
            .select(Columns.raw("following:profiles!following(id, full_name, username, avatar)")) {
                filter { eq("follower", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Profile>()
    }

    suspend fun fetchFollowing(userId: String): Result<List<Profile>> = runCatching {
        Supabase.postgrest.from("followers")
            .select(Columns.raw("follower:profiles!follower(id, full_name, username, avatar)")) {
                filter { eq("following", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Profile>()
    }

    // ------------------------------------------------------------------
    // Bookmarks
    // ------------------------------------------------------------------
    suspend fun toggleSave(userId: String, postId: Long): Result<Boolean> = runCatching {
        val existing = Supabase.postgrest.from("saved_posts")
            .select(Columns.raw("id")) {
                filter { eq("user_id", userId) }
                filter { eq("post_id", postId) }
            }
            .decodeList<SavedPost>()
        if (existing.isNotEmpty()) {
            Supabase.postgrest.from("saved_posts").delete {
                filter { eq("id", existing.first().id!!) }
            }
            false
        } else {
            Supabase.postgrest.from("saved_posts")
                .insert(SavedPost(userId = userId, postId = postId))
            true
        }
    }

    suspend fun fetchSavedPosts(userId: String): Result<List<Post>> = runCatching {
        val saved = Supabase.postgrest.from("saved_posts")
            .select(Columns.raw("id, post:posts!post_id(*)")) {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<SavedPostTmp>()
        saved.mapNotNull { it.post }
    }

    // ------------------------------------------------------------------
    // Search / suggestions
    // ------------------------------------------------------------------
    suspend fun searchUsers(query: String, me: String): Result<List<Profile>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        Supabase.postgrest.from("profiles")
            .select(Columns.raw("id, full_name, username, phone, avatar, verified")) {
                filter { or { ilike("full_name", "%$query%") } }
                limit(20)
            }
            .decodeList<Profile>()
            .filter { it.id != me }
    }

    suspend fun fetchSuggestions(me: String, limit: Long = 15): Result<List<Profile>> = runCatching {
        Supabase.postgrest.from("profiles")
            .select(Columns.raw("id, full_name, username, phone, avatar, verified")) {
                filter { neq("id", me) }
                order("created_at", Order.DESCENDING)
                limit(limit)
            }
            .decodeList<Profile>()
    }

    // ------------------------------------------------------------------
    // Notifications
    // ------------------------------------------------------------------
    suspend fun fetchNotifications(userId: String): Result<List<Notification>> = runCatching {
        Supabase.postgrest.from("notifications")
            .select(Columns.raw("*, actorProfile:profiles!actor(id, full_name, username, avatar)")) {
                filter { eq("recipient", userId) }
                order("created_at", Order.DESCENDING)
                limit(100)
            }
            .decodeList<Notification>()
    }

    suspend fun markNotificationsSeen(userId: String): Result<Unit> = runCatching {
        val body = buildJsonObject { put("seen", true) }
        Supabase.postgrest.from("notifications")
            .update(body) {
                filter { eq("recipient", userId) }
            }
    }

    suspend fun createNotification(
        recipient: String,
        actor: String,
        type: String,
        postId: Long? = null,
        commentId: Long? = null,
    ): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("recipient", recipient)
            put("actor", actor)
            put("type", type)
            if (postId != null) put("post_id", postId)
            if (commentId != null) put("comment_id", commentId)
        }
        Supabase.postgrest.from("notifications").insert(body)
    }
}

/** Small intermediary DTO to read embedded saved posts. */
@kotlinx.serialization.Serializable
internal data class SavedPostTmp(@kotlinx.serialization.SerialName("post") val post: Post? = null)