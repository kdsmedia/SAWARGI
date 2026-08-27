package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Post
import com.altomedia.sawargi.data.Profile
import com.altomedia.sawargi.data.ReactionType
import com.altomedia.sawargi.data.Story
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface FeedState {
    data object Loading : FeedState
    data class Loaded(val posts: List<Post>) : FeedState
    data class Error(val message: String) : FeedState
}

class HomeViewModel : ViewModel() {

    private val repo = Deps.socialRepository

    private val _feed = MutableStateFlow<FeedState>(FeedState.Loading)
    val feed: StateFlow<FeedState> = _feed.asStateFlow()

    private val _myReactions = MutableStateFlow<Map<Long, String>>(emptyMap())
    val myReactions: StateFlow<Map<Long, String>> = _myReactions.asStateFlow()

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _savedCount = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val savedCount: StateFlow<Map<Long, Boolean>> = _savedCount.asStateFlow()

    private val _myProfile = MutableStateFlow<Profile?>(null)
    val myProfile: StateFlow<Profile?> = _myProfile.asStateFlow()

    init {
        load()
    }

    fun load() {
        loadFeed()
        loadProfile()
    }

    fun refresh() {
        viewModelScope.launch {
            _feed.update { FeedState.Loading }
            repo.fetchFeed().onSuccess { posts ->
                _feed.update { FeedState.Loaded(posts) }
            }.onFailure { err ->
                _feed.update { FeedState.Error(err.message ?: "Gagal memuat feed.") }
            }
            val uid = repo.currentUserId()
            if (uid != null) applyReactions(uid)
        }
    }

    private fun loadFeed() {
        viewModelScope.launch {
            repo.fetchFeed()
                .onSuccess { posts ->
                    _feed.update { FeedState.Loaded(posts) }
                    repo.currentUserId()?.let { applyReactions(it) }
                }
                .onFailure { err ->
                    _feed.update { FeedState.Error(err.message ?: "Gagal memuat feed.") }
                }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val uid = repo.currentUserId() ?: return@launch
            repo.fetchProfile(uid).onSuccess { p -> _myProfile.value = p }
        }
    }

    private fun applyReactions(uid: String) {
        viewModelScope.launch {
            repo.fetchMyReactions(uid).onSuccess { reactions ->
                _myReactions.value = reactions.mapNotNull { r ->
                    val pid = r.postId
                    if (pid != null) pid to r.type else null
                }.toMap()
                _feed.update { state ->
                    val loaded = state as? FeedState.Loaded ?: return@update state
                    val byId = _myReactions.value
                    FeedState.Loaded(loaded.posts.map { p ->
                        p.copy(myReaction = p.id?.let { byId[it] } ?: p.myReaction)
                    })
                }
            }
        }
    }

    /** Toggle a reaction then reconcile with the server. */
    fun toggleReaction(post: Post, reaction: ReactionType) {
        val uid = repo.currentUserId() ?: return
        val postId = post.id ?: return
        viewModelScope.launch {
            repo.toggleReaction(uid, postId, reaction)
                .onSuccess { result ->
                    val target = result?.type
                    val updated = _myReactions.value.toMutableMap()
                    if (target == null) updated.remove(postId) else updated[postId] = target
                    _myReactions.value = updated
                    updatePostReaction(postId, target)
                }
        }
    }

    fun toggleSave(post: Post) {
        val uid = repo.currentUserId() ?: return
        val pid = post.id ?: return
        viewModelScope.launch {
            repo.toggleSave(uid, pid).onSuccess { saved ->
                val m = _savedCount.value.toMutableMap()
                m[pid] = saved
                _savedCount.value = m
            }
        }
    }

    private fun updatePostReaction(postId: Long, target: String?) {
        _feed.update { state ->
            val loaded = state as? FeedState.Loaded ?: return@update state
            FeedState.Loaded(loaded.posts.map { p ->
                if (p.id == postId) {
                    val prev = ReactionType.from(p.myReaction)
                    val next = ReactionType.from(target)
                    val delta = when {
                        prev == null && next != null -> 1
                        prev != null && next == null -> -1
                        else -> 0
                    }
                    p.copy(
                        myReaction = target,
                        reactionsCount = (p.reactionsCount + delta).coerceAtLeast(0)
                    )
                } else p
            })
        }
    }
}