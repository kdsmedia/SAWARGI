package com.altomedia.sawargi.data

import io.github.jan.supabase.postgrest.query.Order

/**
 * Feed / posts / comments repository backed by Supabase Postgrest.
 */
class PostRepository {

    suspend fun fetchPosts(): Result<List<Post>> = runCatching {
        Supabase.postgrest
            .from("posts")
            .select {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Post>()
    }

    suspend fun createPost(userId: String, text: String, image: String? = null): Result<Post> = runCatching {
        val post = Post(userId = userId, text = text, image = image)
        // Single-value insert (auto-serialized). Return the local object.
        Supabase.postgrest
            .from("posts")
            .insert(post)
        post
    }

    suspend fun fetchComments(postId: String): Result<List<Comment>> = runCatching {
        Supabase.postgrest
            .from("comments")
            .select {
                filter { eq("post_id", postId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Comment>()
    }

    suspend fun addComment(postId: String, userId: String, text: String): Result<Comment> = runCatching {
        val comment = Comment(postId = postId, userId = userId, text = text)
        Supabase.postgrest
            .from("comments")
            .insert(comment)
        comment
    }
}