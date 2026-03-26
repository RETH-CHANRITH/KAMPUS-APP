package com.example.kampus.ui.groups

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  Shared colour palette
// ─────────────────────────────────────────────────────────────────────────────
object GroupColors {
    val Bg        = Color(0xFF0A0E1A)
    val Surface   = Color(0xFF111827)
    val Card      = Color(0xFF111827)
    val Border    = Color(0xFF1C2A3F)
    val Blue      = Color(0xFF2563EB)
    val BlueGlow  = Color(0xFF1D4ED8)
    val White     = Color(0xFFF9FAFB)
    val Gray1     = Color(0xFFD1D5DB)
    val Gray3     = Color(0xFF6B7280)
    val Gray4     = Color(0xFF4B5563)
    val Gray5     = Color(0xFF374151)
    val Red       = Color(0xFFEF4444)
    val NavBg     = Color(0xFF0D111C)
    val NavBorder = Color(0xFF141D2E)
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
    val description : String  = "",
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