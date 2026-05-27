package com.example.kampus.ui.auth

import androidx.compose.ui.graphics.Color

/** Matches login / register / forgot-password mockups (blue accent, dark inputs). */
object AuthColors {
    private val BackgroundDark = Color(0xFF121417)
    private val InputFillDark = Color(0xFF2A2D34)
    private val TextSecondaryDark = Color(0xFF9CA3AF)
    private val SocialButtonDark = Color(0xFF1E2229)
    val Background get() = BackgroundDark
    val InputFill get() = InputFillDark
    val Primary get() = Color(0xFF0D7FFF)
    val TextSecondary get() = TextSecondaryDark
    val SocialButtonBg get() = SocialButtonDark
    val SocialTintSurface get() = Primary.copy(alpha = 0.15f)
    val CardSurface get() = Color(0xFF26262C)
    val BorderSubtle get() = Color(0xFF3F3F46)
    val BackIconTint get() = Color(0xFF111827)
}
