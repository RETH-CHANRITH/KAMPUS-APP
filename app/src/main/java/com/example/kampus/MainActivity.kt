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
import kotlinx.coroutines.flow.MutableSharedFlow
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    val intentFlow = MutableSharedFlow<android.content.Intent>(extraBufferCapacity = 1)
    private var notificationsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentFlow.tryEmit(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationsListener?.remove()
        notificationsListener = null
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

        // Start listening to real-time user notifications to show system alerts
        val auth = FirebaseAuth.getInstance()
        val listenerStartTime = System.currentTimeMillis()
        
        auth.addAuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            notificationsListener?.remove()
            notificationsListener = null
            
            if (currentUser != null) {
                val db = FirebaseFirestore.getInstance()
                notificationsListener = db.collection("users").document(currentUser.uid)
                    .collection("notifications")
                    .whereGreaterThan("createdAt", listenerStartTime)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null || snapshots == null) return@addSnapshotListener
                        for (change in snapshots.documentChanges) {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val doc = change.document
                                val title = doc.getString("title") ?: "New Notification"
                                val body = doc.getString("body") ?: ""
                                val type = doc.getString("type") ?: ""
                                val targetId = doc.getString("targetId") ?: ""
                                
                                // Show local notification on device status bar
                                com.example.kampus.utils.NotificationLogger.showSystemNotification(
                                    context = this,
                                    title = title,
                                    body = body,
                                    type = type,
                                    targetId = targetId
                                )
                            }
                        }
                    }
            }
        }
        
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