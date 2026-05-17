package com.example.kampus

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.kampus.di.SupabaseModule
import com.example.kampus.utils.PresenceManager
import com.example.kampus.utils.E2EEManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KampusApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize E2EE Configuration (sets plaintext mode by default to work around KeyStore2 issues)
        com.example.kampus.utils.E2EEConfig.initialize(this)
        
        // Initialize Supabase for image uploads
        SupabaseModule.initSupabase(this)

        // Initialize E2EE Manager with encrypted shared preferences
        initializeE2EEManager()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
                appScope.launch {
                    runCatching { PresenceManager.markOnline(userId) }
                    runCatching { E2EEManager.ensureUserKeys(userId) }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
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
            android.util.Log.d("KampusApplication", "E2EEManager initialized successfully")
        } catch (e: Exception) {
            // If EncryptedSharedPreferences fails (emulator/device issues), fall back to regular SharedPreferences
            android.util.Log.w("KampusApplication", "EncryptedSharedPreferences unavailable, falling back to regular SharedPreferences: ${e.message}")
            e.printStackTrace()
            try {
                val fallbackPrefs = getSharedPreferences("kampus_e2ee_prefs", MODE_PRIVATE)
                E2EEManager.initialize(fallbackPrefs)
                android.util.Log.w("KampusApplication", "E2EEManager initialized with fallback SharedPreferences (NOT encrypted)")
            } catch (ex: Exception) {
                android.util.Log.e("KampusApplication", "Failed to initialize E2EEManager with fallback prefs: ${ex.message}", ex)
                ex.printStackTrace()
            }
        }
    }
}
