package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Post
import com.altomedia.sawargi.data.Profile
import com.altomedia.sawargi.data.ReactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtherProfileViewModel : ViewModel() {

    private val repo = Deps.socialRepository

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _amFollowing = MutableStateFlow(false)
    val amFollowing: StateFlow<Boolean> = _amFollowing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun load(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchProfile(userId).onSuccess { _profile.value = it }
            loadPosts(userId)
            loadFollowState(userId)
            _isLoading.value = false
        }
    }

    private fun loadPosts(userId: String) {
        viewModelScope.launch {
            repo.fetchProfilePosts(userId).onSuccess { _posts.value = it }
        }
    }

    private fun loadFollowState(userId: String) {
        val me = repo.currentUserId() ?: return
        viewModelScope.launch {
            repo.isFollowing(me, userId).onSuccess { _amFollowing.value = it }
        }
    }

    fun toggleFollow() {
        val me = repo.currentUserId() ?: return
        val userId = _profile.value?.id ?: return
        viewModelScope.launch {
            repo.followToggle(me, userId).onSuccess { nowFollowing ->
                _amFollowing.value = nowFollowing
                if (nowFollowing) {
                    repo.createNotification(
                        recipient = userId,
                        actor = me,
                        type = "follow"
                    )
                }
            }
        }
    }

    fun toggleReaction(post: Post, reaction: ReactionType) {
        val me = repo.currentUserId() ?: return
        val pid = post.id ?: return
        viewModelScope.launch {
            repo.toggleReaction(me, pid, reaction).onSuccess { res ->
                val target = res?.type
                _posts.update { list ->
                    list.map { p -> if (p.id == pid) p.copy(myReaction = target) else p }
                }
            }
        }
    }

    fun toggleSave(post: Post) {
        val me = repo.currentUserId() ?: return
        val pid = post.id ?: return
        viewModelScope.launch {
            repo.toggleSave(me, pid)
        }
    }
}