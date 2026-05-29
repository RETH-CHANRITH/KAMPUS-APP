// ─── AuthViewModel.kt ─────────────────────────────────────────────────────
package com.example.kampus.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.kampus.utils.PresenceManager
import com.example.kampus.utils.E2EEManager
import kotlinx.coroutines.Dispatchers

sealed class SessionState {
    object Unknown : SessionState()
    object LoggedOut : SessionState()
    data class LoggedIn(val uid: String) : SessionState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

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
            
            // First check if this email is banned (optional, but good UX)
            // But we can only do this after signing in to get the UID safely.
            
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        _authState.value = AuthState.Error("Login failed: missing user session")
                        return@addOnSuccessListener
                    }

                    // Check if banned
                    firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
                        if (doc.getBoolean("isBanned") == true) {
                            auth.signOut()
                            _authState.value = AuthState.Error("Your account has been banned for violating community guidelines.")
                            return@addOnSuccessListener
                        }

                        refreshSession()
                        ensureUserProfile(
                            userId = uid,
                            email = auth.currentUser?.email ?: email,
                            displayName = auth.currentUser?.displayName,
                        ) { ok, err ->
                            if (ok) {
                                // Ensure E2E encryption keys are set up
                                viewModelScope.launch(Dispatchers.IO) {
                                    E2EEManager.ensureUserKeys(uid)
                                        .onSuccess {
                                            _authState.value = AuthState.Success("Login successful!")
                                        }
                                        .onFailure { e ->
                                            _authState.value = AuthState.Error("Key setup failed: ${e.message}")
                                        }
                                }
                            } else {
                                _authState.value = AuthState.Error(err ?: "Login failed while syncing profile")
                            }
                        }
                    }
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
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        _authState.value = AuthState.Error("Registration failed: missing user session")
                        return@addOnSuccessListener
                    }

                    ensureUserProfile(
                        userId = uid,
                        email = email,
                        displayName = name,
                    ) { ok, err ->
                        if (ok) {
                            // Seed E2E encryption keys for new user
                            viewModelScope.launch(Dispatchers.IO) {
                                E2EEManager.seedKeysForNewUser(uid)
                                    .onSuccess {
                                        _authState.value = AuthState.Success("Registration successful!")
                                    }
                                    .onFailure { e ->
                                        _authState.value = AuthState.Error("Key generation failed: ${e.message}")
                                    }
                            }
                        } else {
                            _authState.value = AuthState.Error(err ?: "Registration failed while creating profile")
                        }
                    }
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
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        _authState.value = AuthState.Error("Google login failed: missing user session")
                        return@addOnSuccessListener
                    }

                    // Check if banned
                    firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
                        if (doc.getBoolean("isBanned") == true) {
                            auth.signOut()
                            _authState.value = AuthState.Error("Your account has been banned.")
                            return@addOnSuccessListener
                        }

                        refreshSession()
                        ensureUserProfile(
                            userId = uid,
                            email = auth.currentUser?.email,
                            displayName = auth.currentUser?.displayName,
                        ) { ok, err ->
                            if (ok) {
                                // Ensure E2E encryption keys are set up
                                viewModelScope.launch(Dispatchers.IO) {
                                    E2EEManager.ensureUserKeys(uid)
                                        .onSuccess {
                                            _authState.value = AuthState.Success("Google login successful!")
                                        }
                                        .onFailure { e ->
                                            _authState.value = AuthState.Error("Key setup failed: ${e.message}")
                                        }
                                }
                            } else {
                                _authState.value = AuthState.Error(err ?: "Google login failed while syncing profile")
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    _authState.value = AuthState.Error(e.message ?: "Google login failed")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                runCatching { PresenceManager.markOffline(userId) }
                // Clear E2E encryption keys
                E2EEManager.clearUserKeys(userId)
            }
            auth.signOut()
            refreshSession()
            _authState.value = AuthState.Idle
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

    private fun ensureUserProfile(
        userId: String,
        email: String?,
        displayName: String?,
        onComplete: (Boolean, String?) -> Unit,
    ) {
        val safeEmail = email.orEmpty()
        val computedName = when {
            !displayName.isNullOrBlank() -> displayName
            safeEmail.contains("@") -> safeEmail.substringBefore("@").replace(".", " ")
            else -> "User"
        }.trim().ifBlank { "User" }

        val computedHandle = "@" + computedName
            .lowercase()
            .replace(Regex("\\s+"), "")
            .replace(Regex("[^a-z0-9_@]"), "")
            .ifBlank { "user" }

        val userRef = firestore.collection("users").document(userId)
        userRef.get()
            .addOnSuccessListener { existing ->
                if (existing.exists()) {
                    val updates = mutableMapOf<String, Any>()
                    if ((existing.getString("displayName") ?: "").isBlank()) updates["displayName"] = computedName
                    if ((existing.getString("handle") ?: "").isBlank()) updates["handle"] = computedHandle
                    if ((existing.getString("email") ?: "").isBlank() && safeEmail.isNotBlank()) updates["email"] = safeEmail
                    updates["updatedAt"] = System.currentTimeMillis()

                    if (updates.size == 1 && updates.containsKey("updatedAt")) {
                        onComplete(true, null)
                    } else {
                        userRef.update(updates)
                            .addOnSuccessListener { onComplete(true, null) }
                            .addOnFailureListener { e -> onComplete(false, e.message) }
                    }
                } else {
                    val profile = mapOf(
                        "id" to userId,
                        "displayName" to computedName,
                        "handle" to computedHandle,
                        "bio" to "",
                        "email" to safeEmail,
                        "phone" to "",
                        "faculty" to "",
                        "year" to "",
                        "location" to "",
                        "avatarEmoji" to "🎓",
                        "profileImageUrl" to "",
                        "coverImageUrl" to "",
                        "isOnline" to true,
                        "isVerified" to false,
                        "stats" to mapOf(
                            "posts" to 0L,
                            "followers" to 0L,
                            "following" to 0L,
                            "friendRequests" to 0L,
                        ),
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis(),
                    )

                    userRef.set(profile)
                        .addOnSuccessListener {
                            viewModelScope.launch {
                                runCatching { PresenceManager.markOnline(userId) }
                                    .onSuccess { onComplete(true, null) }
                                    .onFailure { e -> onComplete(false, e.message) }
                            }
                        }
                        .addOnFailureListener { e -> onComplete(false, e.message) }
                }
            }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }
}
