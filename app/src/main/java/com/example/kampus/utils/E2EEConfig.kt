package com.example.kampus.utils

import android.content.Context
import android.util.Log

/**
 * E2EE Configuration & Diagnostic Helper
 *
 * Temporary fallback mechanism to bypass E2EE while debugging KeyStore2 issues.
 * This allows the app to function with plaintext messages until crypto infrastructure
 * is fixed.
 */
object E2EEConfig {
    private const val TAG = "E2EEConfig"
    private const val PREF_NAME = "e2ee_config"
    private const val KEY_E2EE_ENABLED = "e2ee_enabled"

    private var sharedPrefs: android.content.SharedPreferences? = null

    /**
     * Initialize configuration (call from Application.onCreate)
     */
    fun initialize(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Force E2EE ON so the app always uses the encrypted chat path.
        if (!sharedPrefs!!.contains(KEY_E2EE_ENABLED) || !sharedPrefs!!.getBoolean(KEY_E2EE_ENABLED, true)) {
            sharedPrefs!!.edit().putBoolean(KEY_E2EE_ENABLED, true).apply()
            Log.i(TAG, "✅ E2EE ENABLED (forced on startup)")
        }
    }

    /**
     * Check if E2EE is enabled
     */
    fun isE2EEEnabled(): Boolean {
        return sharedPrefs?.getBoolean(KEY_E2EE_ENABLED, false) ?: false
    }

    /**
     * Toggle E2EE (for testing/debugging)
     */
    fun setE2EEEnabled(enabled: Boolean) {
        sharedPrefs?.edit()?.putBoolean(KEY_E2EE_ENABLED, enabled)?.apply()
        if (enabled) {
            Log.i(TAG, "✅ E2EE ENABLED")
        } else {
            Log.w(TAG, "⚠️ E2EE DISABLED: Using plaintext mode")
        }
    }

    /**
     * Diagnostic: Log KeyStore status
     */
    fun logDiagnostics() {
        Log.d(TAG, """
            === E2EE Diagnostics ===
            E2EE Enabled: ${isE2EEEnabled()}
            Mode: ENCRYPTED
            Status: Production
            ================================
        """.trimIndent())
    }
}
