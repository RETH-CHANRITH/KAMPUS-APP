package com.example.kampus.ui.groups

import androidx.compose.ui.graphics.Color
import com.example.kampus.ui.theme.ThemeController

// ─────────────────────────────────────────────────────────────────────────────
//  Shared colour palette
// ─────────────────────────────────────────────────────────────────────────────
object GroupColors {
    private val isDark get() = ThemeController.isDark
    val Bg        get() = if (isDark) Color(0xFF0A0E1A) else Color(0xFFF3F4F8)
    val Surface   get() = if (isDark) Color(0xFF111827) else Color(0xFFFFFFFF)
    val Card      get() = if (isDark) Color(0xFF111827) else Color(0xFFFFFFFF)
    val Border    get() = if (isDark) Color(0xFF1C2A3F) else Color(0xFFD1D5DB)
    val Blue      get() = ThemeController.accent.color
    val BlueGlow  get() = Blue.copy(alpha = if (isDark) 0.75f else 0.55f)
    val White     get() = if (isDark) Color(0xFFF9FAFB) else Color(0xFF111827)
    val Gray1     get() = if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151)
    val Gray3     get() = if (isDark) Color(0xFF6B7280) else Color(0xFF6B7280)
    val Gray4     get() = if (isDark) Color(0xFF4B5563) else Color(0xFF9CA3AF)
    val Gray5     get() = if (isDark) Color(0xFF374151) else Color(0xFF9CA3AF)
    val Red       get() = Color(0xFFEF4444)
    val NavBg     get() = if (isDark) Color(0xFF0D111C) else Color(0xFFFFFFFF)
    val NavBorder get() = if (isDark) Color(0xFF141D2E) else Color(0xFFD1D5DB)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Domain models
// ─────────────────────────────────────────────────────────────────────────────
data class GroupData(
    val id          : Int,
    val name        : String,
    val category    : String,
    val coverColor1 : Color,
    val coverColor2 : Color,
    val coverEmoji  : String,
    val members     : String,
    val posts       : String,
    val isJoined    : Boolean = false,
    val privacy     : String  = "public",
    val ownerId     : String  = "",
    val description : String  = "",
)

data class GroupJoinRequest(
    val requesterId   : String,
    val requesterName : String,
    val requestedAt   : Long,
    val status        : String = "pending",
)

data class GroupPost(
    val id            : Int,
    val author        : String,
    val initials      : String,
    val initialsColor : Color,
    val time          : String,
    val content       : String,
    val hasImage      : Boolean = false,
    val likes         : Int,
    val comments      : Int,
)