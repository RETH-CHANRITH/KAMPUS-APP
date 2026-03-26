// ─── AuthViewModel.kt ─────────────────────────────────────────────────────
package com.example.kampus.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SessionState {
    object Unknown : SessionState()
    object LoggedOut : SessionState()
    data class LoggedIn(val uid: String) : SessionState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _session = MutableStateFlow<SessionState>(SessionState.Unknown)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        _session.value = auth.currentUser?.uid?.let { SessionState.LoggedIn(it) } ?: SessionState.LoggedOut
    }

    fun refreshSession() {
        _session.value = auth.currentUser?.uid?.let { SessionState.LoggedIn(it) } ?: SessionState.LoggedOut
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    refreshSession()
                    _authState.value = AuthState.Success("Login successful!")
                }
                .addOnFailureListener { e ->
                    _authState.value = AuthState.Error(e.message ?: "Login failed")
                }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    refreshSession()
                    _authState.value = AuthState.Success("Registration successful!")
                }
                .addOnFailureListener { e ->
                    _authState.value = AuthState.Error(e.message ?: "Registration failed")
                }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener {
                    refreshSession()
                    _authState.value = AuthState.Success("Google login successful!")
                }
                .addOnFailureListener { e ->
                    _authState.value = AuthState.Error(e.message ?: "Google login failed")
                }
        }
    }

    fun logout() {
        auth.signOut()
        refreshSession()
        _authState.value = AuthState.Idle
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
