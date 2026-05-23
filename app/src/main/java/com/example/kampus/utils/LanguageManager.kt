package com.example.kampus.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "kampus_prefs"
    private const val KEY_LANGUAGE = "app_language"

    const val ENGLISH = "en"
    const val KHMER = "km"

    private val _languageCode = MutableStateFlow(ENGLISH)
    val languageCode: StateFlow<String> = _languageCode

    fun initialize(context: Context) {
        _languageCode.value = readStoredLanguageCode(context)
    }

    fun getLanguageCode(context: Context): String = readStoredLanguageCode(context)

    fun setLanguage(context: Context, languageCode: String) {
        val normalized = when (languageCode) {
            KHMER -> KHMER
            else -> ENGLISH
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, normalized)
            .apply()
        _languageCode.value = normalized
    }

    private fun readStoredLanguageCode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, ENGLISH)
            .orEmpty()
            .ifBlank { ENGLISH }
    }

    fun wrapContext(context: Context): Context {
        val languageCode = getLanguageCode(context)
        val locale = when (languageCode) {
            KHMER -> Locale.forLanguageTag("km")
            else -> Locale.forLanguageTag("en")
        }

        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(android.os.LocaleList(locale))
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            ContextWrapper(context)
        }
    }
}