package com.example.kampus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val KampusDarkColorScheme = darkColorScheme(
    primary = NeonPink,
    onPrimary = Color.White,
    primaryContainer = NeonPink.copy(alpha = 0.18f),
    onPrimaryContainer = NeonPink,

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
    inversePrimary = NeonPink,
)

@Composable
fun KampusTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> KampusDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
