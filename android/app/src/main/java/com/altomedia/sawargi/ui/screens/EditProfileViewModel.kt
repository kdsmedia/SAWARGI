package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.Deps
import com.altomedia.sawargi.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditProfileViewModel : ViewModel() {

    private val repo = Deps.socialRepository

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val uid = repo.currentUserId() ?: return@launch
            repo.fetchProfile(uid).onSuccess { p ->
                if (p != null) {
                    _name.value = p.fullName ?: p.displayName
                    _bio.value = p.bio ?: ""
                    _username.value = p.username ?: ""
                }
            }
        }
    }

    fun onNameChange(v: String) {
        _name.value = v
        _error.value = null
    }

    fun onBioChange(v: String) {
        _bio.value = v
        _error.value = null
    }

    fun onUsernameChange(v: String) {
        _username.value = v
        _error.value = null
    }

    fun save() {
        val uid = repo.currentUserId() ?: return
        if (_name.value.isBlank()) {
            _error.value = "Nama tidak boleh kosong."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val updated = Profile(
                id = uid,
                username = _username.value.ifBlank { null },
                fullName = _name.value,
                bio = _bio.value.ifBlank { null }
            )
            repo.updateProfile(updated)
                .onSuccess { _saved.value = true }
                .onFailure { err -> _error.value = err.message ?: "Gagal menyimpan profil." }
            _isSaving.value = false
        }
    }

    fun reset() {
        _saved.value = false
        _error.value = null
    }
}