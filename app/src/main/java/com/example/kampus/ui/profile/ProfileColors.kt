package com.example.kampus.ui.profile

import androidx.compose.ui.graphics.Color
import com.example.kampus.ui.theme.ThemeController

object ProfileColors {
    private val isDark get() = ThemeController.isDark
    
    val Bg get() = if (isDark) Color(0xFF1A1D2E) else Color(0xFFF3F4F8)
    val Card get() = if (isDark) Color(0xFF252A41) else Color(0xFFFFFFFF)
    val Border get() = if (isDark) Color(0xFF364153) else Color(0xFFD1D5DB)
    val Blue get() = Color(0xFF0D7FFF)
    val Purple get() = Color(0xFF7C3AED)
    val White get() = if (isDark) Color(0xFFFFFFFF) else Color(0xFF111827)
    val Subtle get() = if (isDark) Color(0xFF99A1AF) else Color(0xFF6B7280)
    val Red get() = Color(0xFFEF4444)
    val NavBg get() = if (isDark) Color(0xFF0C1018) else Color(0xFFFFFFFF)
    val NavBorder get() = if (isDark) Color(0xFF141D2E) else Color(0xFFD1D5DB)
    val ErrorBg get() = if (isDark) Color(0xFF4B1F1F) else Color(0xFFFEE2E2)
    val SuccessBg get() = if (isDark) Color(0xFF1F4B2B) else Color(0xFFE0F7E4)
}
