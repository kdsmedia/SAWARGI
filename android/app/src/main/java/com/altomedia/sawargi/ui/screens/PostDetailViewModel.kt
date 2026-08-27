package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Comment
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Post
import com.altomedia.sawargi.data.ReactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostDetailViewModel : ViewModel() {

    private val repo = Deps.socialRepository

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _newComment = MutableStateFlow("")
    val newComment: StateFlow<String> = _newComment.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Full post for this screen is not provided by the API, so build from the feed. */
    fun setPost(p: Post?) {
        _post.value = p
    }

    fun load(postId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchComments(postId).onSuccess { _comments.value = it }
            _isLoading.value = false
        }
    }

    fun onCommentTextChange(text: String) {
        _newComment.value = text
    }

    fun submitComment() {
        val text = _newComment.value.trim()
        val uid = repo.currentUserId() ?: return
        val postId = _post.value?.id ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.addComment(uid, postId, text)
                .onSuccess { comment ->
                    _comments.update { it + comment }
                    _newComment.value = ""
                    repo.createNotification(
                        recipient = _post.value?.userId ?: return@onSuccess,
                        actor = uid,
                        type = "comment",
                        postId = postId
                    )
                }
        }
    }

    fun toggleCommentReaction() {
        // Comments do not support reactions in the current API; kept for symmetry.
    }

    fun deleteComment(comment: Comment) {
        val id = comment.id ?: return
        viewModelScope.launch {
            repo.deleteComment(id).onSuccess {
                _comments.update { comments -> comments.filterNot { it.id == id } }
            }
        }
    }
}