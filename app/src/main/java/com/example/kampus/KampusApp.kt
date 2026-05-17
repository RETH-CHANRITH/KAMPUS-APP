package com.example.kampus

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.kampus.di.SupabaseModule
import com.example.kampus.utils.FcmTokenManager
import com.example.kampus.utils.E2EEManager
import com.example.kampus.utils.PresenceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KampusApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: AuthStateListener

    override fun onCreate() {
        super.onCreate()

        // Ensure Firebase SDK is initialized before using any Firebase APIs
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        authStateListener = AuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid ?: return@AuthStateListener
            appScope.launch {
                runCatching { FcmTokenManager.syncCurrentDeviceToken(this@KampusApp, userId) }
                    .onFailure { error -> Log.w("KampusApp", "FCM token sync failed after auth change: ${error.message}") }
            }
        }

        // Initialize Supabase for image uploads
        SupabaseModule.initSupabase(this)

        // Initialize E2EE manager used by chat send flow
        initializeE2EEManager()
        auth.addAuthStateListener(authStateListener)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val userId = auth.currentUser?.uid ?: return
                appScope.launch {
                    runCatching { PresenceManager.markOnline(userId) }
                    runCatching { E2EEManager.ensureUserKeys(userId) }
                    runCatching { FcmTokenManager.flushPendingToken(this@KampusApp, userId) }
                    runCatching { FcmTokenManager.syncCurrentDeviceToken(this@KampusApp, userId) }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                val userId = auth.currentUser?.uid ?: return
                appScope.launch {
                    runCatching { PresenceManager.markOffline(userId) }
                }
            }
        })
    }

    private fun initializeE2EEManager() {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedSharedPrefs = EncryptedSharedPreferences.create(
                this,
                "kampus_e2ee_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            E2EEManager.initialize(encryptedSharedPrefs)
            android.util.Log.d("KampusApp", "E2EEManager initialized successfully")
        } catch (e: Exception) {
            android.util.Log.w("KampusApp", "EncryptedSharedPreferences unavailable, using fallback prefs: ${e.message}")
            try {
                val fallbackPrefs = getSharedPreferences("kampus_e2ee_prefs", MODE_PRIVATE)
                E2EEManager.initialize(fallbackPrefs)
                android.util.Log.w("KampusApp", "E2EEManager initialized with fallback SharedPreferences")
            } catch (ex: Exception) {
                android.util.Log.e("KampusApp", "Failed to initialize E2EEManager fallback: ${ex.message}", ex)
            }
        }
    }
}
