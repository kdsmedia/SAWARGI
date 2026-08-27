package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val repo = Deps.socialRepository

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
            repo.fetchNotifications(uid).onSuccess { _notifications.value = it }
            _isLoading.value = false
        }
    }

    fun markSeen() {
        val uid = repo.currentUserId() ?: return
        viewModelScope.launch {
            repo.markNotificationsSeen(uid).onSuccess {
                _notifications.update { list -> list.map { it.copy(seen = true) } }
            }
        }
    }
}