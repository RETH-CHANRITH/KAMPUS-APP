package com.example.kampus

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.kampus.navigation.NavGraph
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import com.example.kampus.ui.theme.KampusTheme
import com.example.kampus.ui.theme.AppSettings
import com.example.kampus.ui.theme.AppSettingsStore
import com.example.kampus.ui.theme.AppAccent
import com.example.kampus.ui.theme.ThemeController
import com.example.kampus.utils.LanguageManager
import com.example.kampus.utils.FirestoreSeedData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ⚠️ Must be called FIRST — activates the SplashScreen theme so Android
        // draws the dark branded splash instead of a plain white window.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        LanguageManager.initialize(this)
        
        // Seed Firestore with sample data on first launch
        seedFirestoreData()
        requestNotificationPermissionIfNeeded()
        
        setContent {
            val context = LocalContext.current
            val savedSettings by AppSettingsStore
                .settingsFlow(context)
                .collectAsState(initial = AppSettings())

            LaunchedEffect(savedSettings) {
                ThemeController.isDark = savedSettings.isDark
                ThemeController.fontScale = savedSettings.fontScale
                ThemeController.accent = AppAccent.fromKey(savedSettings.accentKey)
            }

            val navController = rememberNavController()
            val baseDensity = LocalDensity.current
            val animatedFontScale by animateFloatAsState(
                targetValue = ThemeController.fontScale,
                label = "appFontScale",
            )
            val scaledDensity = Density(
                density = baseDensity.density,
                fontScale = animatedFontScale,
            )

            // Crossfade the whole content when theme changes to make switching smooth
            Crossfade(targetState = ThemeController.isDark, label = "themeMode") { isDark ->
                KampusTheme(
                    darkTheme = isDark,
                    accent = ThemeController.accent.color,
                ) {
                    CompositionLocalProvider(LocalDensity provides scaledDensity) {
                        NavGraph(navController = navController)
                    }
                }
            }
        }
    }
    
    private fun seedFirestoreData() {
        lifecycleScope.launch {
            val firestore = FirebaseFirestore.getInstance()
            FirestoreSeedData.seedAllData(firestore)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}