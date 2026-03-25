// ─── AuthViewModel.kt ─────────────────────────────────────────────────────
package com.example.campussocial.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // TODO: Connect to Firebase Auth
            // firebaseAuth.signInWithEmailAndPassword(email, password)
            _authState.value = AuthState.Success("Login successful!")
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // TODO: Connect to Firebase Auth
            // firebaseAuth.createUserWithEmailAndPassword(email, password)
            _authState.value = AuthState.Success("Registration successful!")
        }
    }

    fun sendOtp(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // TODO: Send OTP via Firebase
            _authState.value = AuthState.Success("OTP sent to $email")
        }
    }

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // TODO: Verify OTP
            _authState.value = AuthState.Success("OTP verified!")
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
