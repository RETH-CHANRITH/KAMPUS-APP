package com.example.kampus.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val BgDark = Color(0xFF0F0F17)
val SurfaceDark = Color(0xFF1C1B22)
val SurfaceDarkElev = Color(0xFF24222C)

val NeonPink = Color(0xFFFF6B6B)
val NeonPinkLight = Color(0xFFFF8A80)
val NeonPinkSoft = Color(0xFFFF8787)

val AccentYellow = Color(0xFFFFCA28)
val AccentGold = Color(0xFFFFD54F)

val BlueFocus = Color(0xFF5A8CFF)

val TextPrimary: Color get() = if (ThemeController.isDark) Color(0xFFFFFFFF) else Color(0xFF111827)
val TextSecondary: Color get() = if (ThemeController.isDark) Color.White.copy(alpha = 0.52f) else Color(0xFF111827).copy(alpha = 0.60f)
val TextTertiary: Color get() = if (ThemeController.isDark) Color.White.copy(alpha = 0.32f) else Color(0xFF111827).copy(alpha = 0.45f)

val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFFF5252)
val Background: Color get() = if (ThemeController.isDark) Color(0xFF121417) else Color(0xFFF3F4F8)
val InputFill: Color get() = if (ThemeController.isDark) Color(0xFF2A2D34) else Color(0xFFE5E7EB)
val Primary = Color(0xFF3B82F6)

val SocialButtonBg = Color(0xFF1E2229)
val SocialTintSurface = Primary.copy(alpha = 0.15f)
val CardSurface = Color(0xFF26262C)
val BorderSubtle = Color(0xFF3F3F46)
val BackIconTint = Color(0xFF111827)
