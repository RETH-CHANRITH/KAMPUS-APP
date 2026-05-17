package com.example.kampus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Deep dark canvas — must match the XML windowBackground exactly
private val AppBgColor = Color(0xFF080B11)

private fun kampusDarkColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.18f),
    onPrimaryContainer = accent,

    secondary = AccentYellow,
    onSecondary = Color.Black,
    secondaryContainer = AccentYellow.copy(alpha = 0.16f),
    onSecondaryContainer = AccentYellow,

    tertiary = AccentGold,
    onTertiary = Color.Black,
    tertiaryContainer = AccentYellow.copy(alpha = 0.14f),
    onTertiaryContainer = AccentYellow,

    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkElev,
    onSurfaceVariant = TextSecondary,

    outline = Color.White.copy(alpha = 0.18f),
    outlineVariant = Color.White.copy(alpha = 0.10f),

    inverseSurface = Color.White.copy(alpha = 0.08f),
    inverseOnSurface = Color.Black,
    inversePrimary = accent,
)

private fun kampusLightColorScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.18f),
    onPrimaryContainer = accent,

    secondary = AccentYellow,
    onSecondary = Color.Black,
    secondaryContainer = AccentYellow.copy(alpha = 0.16f),
    onSecondaryContainer = AccentYellow,

    tertiary = AccentGold,
    onTertiary = Color.Black,
    tertiaryContainer = AccentYellow.copy(alpha = 0.14f),
    onTertiaryContainer = AccentYellow,

    background = Color(0xFFFFFFFF),
    onBackground = Color.Black,
    surface = Color(0xFFF7F7FA),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F5),
    onSurfaceVariant = Color.Black.copy(alpha = 0.7f),

    outline = Color.Black.copy(alpha = 0.12f),
    outlineVariant = Color.Black.copy(alpha = 0.06f),

    inverseSurface = Color.Black.copy(alpha = 0.08f),
    inverseOnSurface = Color.White,
    inversePrimary = accent,
)

@Composable
fun KampusTheme(
    darkTheme: Boolean = true,
    accent: Color = NeonPink,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> if (darkTheme) kampusDarkColorScheme(accent) else kampusLightColorScheme(accent)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        SideEffect {
            window?.let { w ->
                WindowCompat.setDecorFitsSystemWindows(w, false)

                // Choose appropriate bar background depending on theme
                val bg = if (darkTheme) AppBgColor else colorScheme.background
                @Suppress("DEPRECATION")
                w.statusBarColor = bg.toArgb()
                @Suppress("DEPRECATION")
                w.navigationBarColor = bg.toArgb()

                // Icon tint: light icons for dark theme, dark icons for light theme
                WindowCompat.getInsetsController(w, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
