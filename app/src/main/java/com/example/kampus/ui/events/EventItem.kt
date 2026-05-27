package com.example.kampus.ui.events

import androidx.compose.ui.graphics.Color
import com.example.kampus.ui.theme.AccentYellow
import com.example.kampus.ui.theme.BgDark
import com.example.kampus.ui.theme.BorderSubtle
import com.example.kampus.ui.theme.CardSurface
import com.example.kampus.ui.theme.Primary
import com.example.kampus.ui.theme.SurfaceDark
import com.example.kampus.ui.theme.TextPrimary
import com.example.kampus.ui.theme.TextSecondary
import com.example.kampus.ui.theme.TextTertiary

// ─────────────────────────────────────────────────────────────────────────────
//  Shared colour palette — mirrors KAMPUS dark theme
// ─────────────────────────────────────────────────────────────────────────────
object EventColors {
    val Bg         = BgDark
    val Card       = CardSurface
    val Surface    = SurfaceDark
    val Border     = BorderSubtle
    val Blue       = Primary
    val BlueGlow   = Primary
    val BlueSoft   = Primary.copy(alpha = 0.16f)
    val White      = TextPrimary
    val Gray1      = TextPrimary
    val Gray2      = TextSecondary
    val Gray3      = TextTertiary
    val Gray4      = TextTertiary
    val Gray5      = TextTertiary.copy(alpha = 0.55f)
    val Red        = Color(0xFFEF4444)
    val Green      = Color(0xFF22C55E)
    val Amber      = AccentYellow
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
    val id                   : Int,
    val title                : String,
    val category             : EventCategory,
    val date                 : String,           // e.g. "March 28, 2026"
    val time                 : String,           // e.g. "6:00 PM – 11:00 PM"
    val location             : String,
    val interested           : Int,
    val likes                : Int,
    val comments             : Int,
    val shares               : Int,
    val coverEmoji           : String,           // large emoji used as cover art
    val coverColor1          : Color,
    val coverColor2          : Color,
    val description          : String,
    val organizer            : String,
    val organizerEmoji       : String,
    val organizerTime        : String,
    val isFeatured           : Boolean = false,
    val isInterested         : Boolean = false,
    // Persistence / backend identifiers
    val remoteId             : String = "",      // Supabase UUID string id
    val ownerId              : String = "",
    val createdAt            : Long? = null,
    // Cover media
    val coverImageUrl        : String = "",      // actual image URL from Supabase storage
    val imageUrl             : String? = null,   // legacy alias kept for Firestore compat
    val isPinned             : Boolean = false,
    // Extended event detail fields from Supabase
    val eventType            : String = "",
    val capacity             : Int = 0,
    val registrationDeadline : String = "",
    val website              : String = "",
    val onlineEvent          : Boolean = false,
    val certificateAvailable : Boolean = false,
    val paidEvent            : Boolean = false,
    val allowGuest           : Boolean = false,
    val tags                 : List<String> = emptyList(),
    val speaker              : String = "",
)

// ─────────────────────────────────────────────────────────────────────────────
//  Filter chip model
// ─────────────────────────────────────────────────────────────────────────────
data class EventFilter(
    val label    : String,
    val isActive : Boolean = false,
)