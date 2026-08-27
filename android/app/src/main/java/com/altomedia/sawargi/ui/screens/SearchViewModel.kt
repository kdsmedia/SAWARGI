package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val repo = Deps.socialRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Profile>>(emptyList())
    val results: StateFlow<List<Profile>> = _results.asStateFlow()

    private val _followState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followState: StateFlow<Map<String, Boolean>> = _followState.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
        _hasSearched.value = false
    }

    fun search() {
        val q = _query.value.trim()
        val me = repo.currentUserId() ?: return
        if (q.isEmpty()) {
            _results.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            repo.searchUsers(q, me).onSuccess { users ->
                _results.value = users
                _hasSearched.value = true
                users.forEach { loadFollowState(it.id) }
            }.onFailure { _hasSearched.value = true }
            _isSearching.value = false
        }
    }

    private fun loadFollowState(userId: String) {
        val me = repo.currentUserId() ?: return
        viewModelScope.launch {
            repo.isFollowing(me, userId).onSuccess { following ->
                _followState.update { it + (userId to following) }
            }
        }
    }

    fun toggleFollow(profile: Profile) {
        val me = repo.currentUserId() ?: return
        viewModelScope.launch {
            repo.followToggle(me, profile.id).onSuccess { nowFollowing ->
                _followState.update { it + (profile.id to nowFollowing) }
            }
        }
    }
}