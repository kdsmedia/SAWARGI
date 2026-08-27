package com.altomedia.sawargi.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** User profile stored in the public [users] table. */
@Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
)

/** A timeline post. */
@Serializable
data class Post(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val text: String? = null,
    val image: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val type: String? = null,
)

/** A comment on a post. */
@Serializable
data class Comment(
    val id: String? = null,
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    val text: String,
    @SerialName("created_at") val createdAt: String? = null,
)