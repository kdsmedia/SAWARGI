package com.altomedia.sawargi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altomedia.sawargi.ui.theme.BrandGreen

/** Login / Register screen with its own ViewModel. */
@Composable
fun AuthScreen(
    isRegister: Boolean,
    onSuccess: () -> Unit,
    onSwitchMode: () -> Unit,
    vm: AuthViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val loading by vm.isLoading.collectAsState()

    LaunchedEffect(uiState.loggedIn) {
        if (uiState.loggedIn) onSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (isRegister) "Buat Akun SAWARGI" else "Masuk SAWARGI",
            style = MaterialTheme.typography.headlineMedium,
            color = BrandGreen
        )
        Spacer(Modifier.height(24.dp))

        if (isRegister) {
            OutlinedTextField(
                value = uiState.fullName,
                onValueChange = vm::onFullNameChange,
                label = { Text("Nama Lengkap") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = vm::onPhoneChange,
                label = { Text("Nomor HP") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        if (isRegister) {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = vm::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = vm::onPhoneChange,
                label = { Text("Nomor HP") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = vm::onPasswordChange,
            label = { Text("Sandi") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { if (isRegister) vm.onRegister() else vm.onLogin() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
            } else {
                Text(if (isRegister) "Daftar" else "Masuk")
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSwitchMode) {
            Text(if (isRegister) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar")
        }
    }
}