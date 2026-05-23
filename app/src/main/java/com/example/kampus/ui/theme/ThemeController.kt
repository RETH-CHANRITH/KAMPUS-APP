package com.example.kampus.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AppAccent(val key: String, val color: Color) {
    Blue("blue", Color(0xFF0D7FFF)),
    Purple("purple", Color(0xFF9C27B0)),
    Pink("pink", Color(0xFFE91E63)),
    Red("red", Color(0xFFF44336)),
    Orange("orange", Color(0xFFFF9800)),
    Green("green", Color(0xFF4CAF50)),
    Teal("teal", Color(0xFF009688));

    companion object {
        fun fromKey(key: String): AppAccent = entries.firstOrNull { it.key == key } ?: Blue
    }
}

object ThemeController {
    // Global observable theme flag — true = dark, false = light
    var isDark by mutableStateOf(true)
    // Global font scale for realtime typography updates across the app
    var fontScale by mutableStateOf(1f)
    // Global accent used by MaterialTheme
    var accent by mutableStateOf(AppAccent.Blue)
    fun toggle() { isDark = !isDark }
}
