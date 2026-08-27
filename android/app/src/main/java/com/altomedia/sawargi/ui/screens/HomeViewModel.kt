package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Post
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

    private val repo = Deps.postRepository

    private val _feed = MutableStateFlow<FeedState>(FeedState.Loading)
    val feed: StateFlow<FeedState> = _feed.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _feed.update { FeedState.Loading }
            repo.fetchPosts()
                .onSuccess { posts -> _feed.update { FeedState.Loaded(posts) } }
                .onFailure { err -> _feed.update { FeedState.Error(err.message ?: "Gagal memuat feed.") } }
        }
    }

    fun refresh() = loadFeed()
}