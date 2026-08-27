package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Conversation
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConversationRow(
    val conversation: Conversation,
    val otherId: String,
    val otherName: String,
    val otherAvatar: String?,
)

class ChatListViewModel : ViewModel() {

    private val chatRepo = Deps.chatRepository

    private val _conversations = MutableStateFlow<List<ConversationRow>>(emptyList())
    val conversations: StateFlow<List<ConversationRow>> = _conversations.asStateFlow()

    private val _me = MutableStateFlow<String?>(null)
    val me: StateFlow<String?> = _me.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val me = Deps.socialRepository.currentUserId() ?: run {
                _isLoading.value = false
                return@launch
            }
            _me.value = me
            chatRepo.fetchConversations(me)
                .onSuccess { convos ->
                    _conversations.value = convos.mapNotNull { c ->
                        val other = if (c.userA == me) c.userB else c.userA
                        val prof = chatRepo.fetchProfile(other)
                        ConversationRow(
                            conversation = c,
                            otherId = other,
                            otherName = prof?.displayName ?: "Pengguna",
                            otherAvatar = prof?.avatar
                        )
                    }
                }
            _isLoading.value = false
        }
    }

    /** Return the other user's id (the person to open a chat with). */
    fun openConversation(otherUserId: String): String = otherUserId
}