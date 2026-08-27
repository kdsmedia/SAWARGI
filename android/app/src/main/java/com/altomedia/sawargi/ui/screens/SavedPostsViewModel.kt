package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Post
import com.altomedia.sawargi.data.ReactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SavedPostsViewModel : ViewModel() {

    private val repo = Deps.socialRepository

    private val _saved = MutableStateFlow<List<Post>>(emptyList())
    val saved: StateFlow<List<Post>> = _saved.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _savedSet = MutableStateFlow<Set<Long>>(emptySet())
    val savedSet: StateFlow<Set<Long>> = _savedSet.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val uid = repo.currentUserId() ?: run {
                _isLoading.value = false
                return@launch
            }
            repo.fetchSavedPosts(uid).onSuccess { posts ->
                _saved.value = posts
                _savedSet.value = posts.mapNotNull { it.id }.toSet()
            }
            _isLoading.value = false
        }
    }

    fun toggleSave(post: Post) {
        val uid = repo.currentUserId() ?: return
        val pid = post.id ?: return
        viewModelScope.launch {
            repo.toggleSave(uid, pid).onSuccess { isSavedNow ->
                _savedSet.update { s ->
                    if (isSavedNow) s + pid else s - pid
                }
            }
        }
    }

    fun toggleReaction(post: Post, reaction: ReactionType) {
        val uid = repo.currentUserId() ?: return
        val pid = post.id ?: return
        viewModelScope.launch {
            repo.toggleReaction(uid, pid, reaction).onSuccess { res ->
                val target = res?.type
                _saved.update { list ->
                    list.map { p -> if (p.id == pid) p.copy(myReaction = target) else p }
                }
            }
        }
    }
}