package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.AuthState
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Post
import com.altomedia.sawargi.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val socialRepo = Deps.socialRepository

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _myPosts = MutableStateFlow<List<Post>>(emptyList())
    val myPosts: StateFlow<List<Post>> = _myPosts.asStateFlow()

    private val _followers = MutableStateFlow<List<Profile>>(emptyList())
    val followers: StateFlow<List<Profile>> = _followers.asStateFlow()

    private val _following = MutableStateFlow<List<Profile>>(emptyList())
    val following: StateFlow<List<Profile>> = _following.asStateFlow()

    private val _myReactions = MutableStateFlow<Map<Long, String>>(emptyMap())
    val myReactions: StateFlow<Map<Long, String>> = _myReactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val uid = socialRepo.currentUserId() ?: run {
                _isLoading.value = false
                return@launch
            }
            socialRepo.fetchProfile(uid).onSuccess { _profile.value = it }
            loadPosts(uid)
            loadFollows(uid)
            loadReactions(uid)
            _isLoading.value = false
        }
    }

    private fun loadPosts(uid: String) {
        viewModelScope.launch {
            socialRepo.fetchProfilePosts(uid).onSuccess { _myPosts.value = it }
        }
    }

    private fun loadFollows(uid: String) {
        viewModelScope.launch {
            socialRepo.fetchFollowers(uid).onSuccess { _followers.value = it }
            socialRepo.fetchFollowing(uid).onSuccess { _following.value = it }
        }
    }

    private fun loadReactions(uid: String) {
        viewModelScope.launch {
            socialRepo.fetchMyReactions(uid).onSuccess { reactions ->
                _myReactions.value = reactions.mapNotNull {
                    val pid = it.postId
                    if (pid != null) pid to it.type else null
                }.toMap()
            }
        }
    }

    fun refreshPosts() {
        socialRepo.currentUserId()?.let { loadPosts(it) }
    }

    fun toggleReaction(post: Post, reaction: com.altomedia.sawargi.data.ReactionType) {
        val uid = socialRepo.currentUserId() ?: return
        val pid = post.id ?: return
        viewModelScope.launch {
            socialRepo.toggleReaction(uid, pid, reaction).onSuccess { res ->
                val target = res?.type
                val m = _myReactions.value.toMutableMap()
                if (target == null) m.remove(pid) else m[pid] = target
                _myReactions.value = m
                _myPosts.update { posts ->
                    posts.map { p ->
                        if (p.id == pid) p.copy(myReaction = target) else p
                    }
                }
            }
        }
    }

    fun toggleSave(post: Post) {
        val uid = socialRepo.currentUserId() ?: return
        val pid = post.id ?: return
        viewModelScope.launch {
            socialRepo.toggleSave(uid, pid)
        }
    }

    fun deletePost(post: Post) {
        val pid = post.id ?: return
        viewModelScope.launch {
            socialRepo.deletePost(pid).onSuccess {
                _myPosts.update { posts -> posts.filterNot { it.id == pid } }
            }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            Deps.authRepository.signOut().onSuccess {
                onDone()
            }
        }
    }
}