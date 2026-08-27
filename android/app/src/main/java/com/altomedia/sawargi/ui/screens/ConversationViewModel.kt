package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Message
import com.altomedia.sawargi.data.Profile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationViewModel : ViewModel() {

    private val chatRepo = Deps.chatRepository

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _other = MutableStateFlow<Profile?>(null)
    val other: StateFlow<Profile?> = _other.asStateFlow()

    private val _conversationId = MutableStateFlow<Long?>(null)
    val conversationId: StateFlow<Long?> = _conversationId.asStateFlow()

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var pollJob: Job? = null

    fun start(me: String, otherUserId: String) {
        viewModelScope.launch {
            chatRepo.fetchProfile(otherUserId).let { prof -> _other.value = prof }
            chatRepo.getOrCreateConversation(me, otherUserId).onSuccess { id ->
                _conversationId.value = id
                loadMessages(id)
                startPolling(id)
            }
        }
    }

    private fun loadMessages(convId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepo.fetchMessages(convId).onSuccess { _messages.value = it }
            _isLoading.value = false
        }
    }

    private fun startPolling(convId: Long) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                chatRepo.fetchMessages(convId).onSuccess { msgs ->
                    val existing = _messages.value.map { it.id to it.text }.toSet()
                    val incoming = msgs.filter { m -> (m.id to m.text) !in existing }
                    if (incoming.isNotEmpty()) _messages.value = msgs
                }
            }
        }
    }

    fun send() {
        val t = _text.value.trim()
        val sender = Deps.socialRepository.currentUserId() ?: return
        val convId = _conversationId.value ?: return
        if (t.isBlank()) return
        viewModelScope.launch {
            chatRepo.sendMessage(sender, convId, t).onSuccess { msg ->
                _messages.value = _messages.value + msg
                _text.value = ""
            }
        }
    }

    fun onTextChange(value: String) {
        _text.value = value
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}