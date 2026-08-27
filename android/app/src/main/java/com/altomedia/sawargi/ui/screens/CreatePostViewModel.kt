package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreatePostViewModel : ViewModel() {

    private val repo = Deps.socialRepository

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _myProfile = MutableStateFlow<Profile?>(null)
    val myProfile: StateFlow<Profile?> = _myProfile.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val uid = repo.currentUserId() ?: return@launch
            repo.fetchProfile(uid).onSuccess { _myProfile.value = it }
        }
    }

    fun onTextChange(value: String) {
        _text.value = value
        _error.value = null
    }

    /** Submit a status post (no media for now). */
    fun submit() {
        val t = _text.value.trim()
        val uid = repo.currentUserId() ?: return
        if (t.isBlank()) {
            _error.value = "Status tidak boleh kosong."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            repo.createPost(userId = uid, text = t)
                .onSuccess { _submitted.value = true }
                .onFailure { err -> _error.value = err.message ?: "Gagal membuat postingan." }
            _isSubmitting.value = false
        }
    }

    fun reset() {
        _text.value = ""
        _submitted.value = false
        _error.value = null
    }
}