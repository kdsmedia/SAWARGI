package com.altomedia.sawargi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.sawargi.data.AuthRepository
import com.altomedia.sawargi.data.AuthState
import com.altomedia.sawargi.data.Deps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles login / register UI state for SAWARGI.
 */
class AuthViewModel(private val repo: AuthRepository = Deps.authRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun onLogin() {
        val s = _uiState.value
        if (s.phone.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(error = "Nomor HP dan sandi wajib diisi.") }
            return
        }
        _isLoading.value = true
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch {
            repo.loginWithPhone(s.phone.trim(), s.password)
                .onSuccess { state ->
                    if (state is AuthState.LoggedIn) {
                        _uiState.update { it.copy(loggedIn = true, error = null) }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Login gagal.") }
                }
            _isLoading.value = false
        }
    }

    fun onRegister() {
        val s = _uiState.value
        if (s.phone.isBlank() || s.password.isBlank() || s.fullName.isBlank()) {
            _uiState.update { it.copy(error = "Nama, nomor HP, dan sandi wajib diisi.") }
            return
        }
        _isLoading.value = true
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch {
            repo.registerWithPhone(
                phone = s.phone.trim(),
                password = s.password,
                fullName = s.fullName.trim(),
            ).onSuccess {
                _uiState.update { it.copy(loggedIn = true, error = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Registrasi gagal.") }
            }
            _isLoading.value = false
        }
    }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }
    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}

data class AuthUiState(
    val password: String = "",
    val fullName: String = "",
    val phone: String = "",
    val error: String? = null,
    val loggedIn: Boolean = false,
)