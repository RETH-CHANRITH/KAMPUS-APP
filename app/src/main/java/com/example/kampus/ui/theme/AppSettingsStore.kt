package com.example.kampus.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ui_settings")

data class AppSettings(
    val isDark: Boolean = true,
    val fontScale: Float = 1f,
    val accentKey: String = AppAccent.Blue.key,
)

object AppSettingsStore {
    private object Keys {
        val IsDark = booleanPreferencesKey("is_dark")
        val FontScale = floatPreferencesKey("font_scale")
        val AccentKey = stringPreferencesKey("accent_key")
    }

    fun settingsFlow(context: Context): Flow<AppSettings> =
        context.dataStore.data.map { prefs ->
            AppSettings(
                isDark = prefs[Keys.IsDark] ?: true,
                fontScale = (prefs[Keys.FontScale] ?: 1f).coerceIn(0.3f, 1.5f),
                accentKey = prefs[Keys.AccentKey] ?: AppAccent.Blue.key,
            )
        }

    suspend fun saveTheme(context: Context, isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IsDark] = isDark
        }
    }

    suspend fun saveFontScale(context: Context, fontScale: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FontScale] = fontScale.coerceIn(0.3f, 1.5f)
        }
    }

    suspend fun saveAccent(context: Context, accentKey: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AccentKey] = accentKey
        }
    }
}
