package com.example.kampus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.example.kampus.ui.theme.ThemeController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object KampusColors {
    private val isDark get() = ThemeController.isDark

    val Background: Color get() = if (isDark) Color(0xFF1A1D2E) else Color(0xFFFFFFFF)
    val Surface: Color get() = if (isDark) Color(0xFF252A41) else Color(0xFFF7F7FA)
    val SurfaceElevated: Color get() = if (isDark) Color(0xFF2E3450) else Color(0xFFF0F0F5)
    val Primary: Color get() = ThemeController.accent.color
    val PrimaryContainer: Color get() = Primary.copy(alpha = 0.12f)
    val TextPrimary: Color get() = if (isDark) Color.White else Color(0xFF111827)
    val TextSecondary: Color get() = if (isDark) Color(0xFFD1D5DC) else Color(0xFF6B7280)
    val TextMuted: Color get() = if (isDark) Color(0xFF99A1AF) else Color(0xFF9CA3AF)
    val TextHint: Color get() = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF6B7280).copy(alpha = 0.5f)
    val Success: Color get() = Color(0xFF22C55E)
    val Warning: Color get() = Color(0xFFF59E0B)
    val Error: Color get() = Color(0xFFEF4444)
    val Border: Color get() = if (isDark) Color(0xFF4A5565) else Color(0xFFD1D5DB)
    val DividerColor: Color get() = if (isDark) Color(0xFF1F2437) else Color(0xFFF0F0F5)

    val BadgePublicBackground: Color get() = if (isDark) Color(0x1A22C55E) else Color(0x0F22C55E)
    val BadgePublicText: Color get() = Color(0xFF22C55E)
    val BadgePrivateBackground: Color get() = if (isDark) Color(0x1AF59E0B) else Color(0x0FF59E0B)
    val BadgePrivateText: Color get() = Color(0xFFF59E0B)
}

object KampusType {
    private val family = FontFamily.SansSerif

    val HeadingLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )
    val HeadingMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )
    val HeadingSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
    val BodyMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val BodySmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    )
    val LabelMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val Caption = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )
}

val KampusColorScheme: ColorScheme = darkColorScheme(
    primary = KampusColors.Primary,
    onPrimary = KampusColors.TextPrimary,
    primaryContainer = KampusColors.PrimaryContainer,
    onPrimaryContainer = KampusColors.Primary,
    secondary = KampusColors.Warning,
    onSecondary = Color.Black,
    background = KampusColors.Background,
    onBackground = KampusColors.TextPrimary,
    surface = KampusColors.Surface,
    onSurface = KampusColors.TextPrimary,
    surfaceVariant = KampusColors.SurfaceElevated,
    onSurfaceVariant = KampusColors.TextSecondary,
    outline = KampusColors.Border,
    outlineVariant = KampusColors.DividerColor,
    error = KampusColors.Error,
    onError = Color.White,
)

@Composable
fun KampusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KampusColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}

@Composable
fun KampusAppTheme(content: @Composable () -> Unit) {
    KampusTheme(content = content)
}