package com.example.kampus.ui.events

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  Shared colour palette — mirrors KAMPUS dark theme
// ─────────────────────────────────────────────────────────────────────────────
object EventColors {
    val Bg         = Color(0xFF080B11)
    val Card       = Color(0xFF0F1520)
    val Surface    = Color(0xFF111827)
    val Border     = Color(0xFF1A2333)
    val Blue       = Color(0xFF3B82F6)
    val BlueGlow   = Color(0xFF2563EB)
    val BlueSoft   = Color(0xFF1D3461)
    val White      = Color(0xFFFFFFFF)
    val Gray1      = Color(0xFFE5E7EB)
    val Gray2      = Color(0xFFD1D5DB)
    val Gray3      = Color(0xFF9CA3AF)
    val Gray4      = Color(0xFF6B7280)
    val Gray5      = Color(0xFF374151)
    val Red        = Color(0xFFEF4444)
    val Green      = Color(0xFF22C55E)
    val Amber      = Color(0xFFF59E0B)
    val NavBg      = Color(0xFF0C1018)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Category tag with accent colour
// ─────────────────────────────────────────────────────────────────────────────
enum class EventCategory(val label: String, val color: Color, val emoji: String) {
    MUSIC       ("Music",       Color(0xFF8B5CF6), "🎵"),
    TECH        ("Tech",        Color(0xFF3B82F6), "💻"),
    ART         ("Art",         Color(0xFFF59E0B), "🎨"),
    SPORTS      ("Sports",      Color(0xFF22C55E), "⚽"),
    BUSINESS    ("Business",    Color(0xFF06B6D4), "💼"),
    FOOD        ("Food",        Color(0xFFEF4444), "🍕"),
    CAMPUS      ("Campus",      Color(0xFF3B82F6), "🎓"),
    ENTERTAINMENT("Entertainment", Color(0xFFEC4899), "🎭"),
}

// ─────────────────────────────────────────────────────────────────────────────
//  Domain model
// ─────────────────────────────────────────────────────────────────────────────
data class EventItem(
    val id           : Int,
    val title        : String,
    val category     : EventCategory,
    val date         : String,           // e.g. "March 28, 2026"
    val time         : String,           // e.g. "6:00 PM – 11:00 PM"
    val location     : String,
    val interested   : Int,
    val likes        : Int,
    val comments     : Int,
    val shares       : Int,
    val coverEmoji   : String,           // large emoji used as cover art
    val coverColor1  : Color,
    val coverColor2  : Color,
    val description  : String,
    val organizer    : String,
    val organizerEmoji: String,
    val organizerTime : String,
    val isFeatured   : Boolean = false,
    val isInterested : Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
//  Filter chip model
// ─────────────────────────────────────────────────────────────────────────────
data class EventFilter(
    val label    : String,
    val isActive : Boolean = false,
)