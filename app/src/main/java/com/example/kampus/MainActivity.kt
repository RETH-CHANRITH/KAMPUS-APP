package com.example.kampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.kampus.navigation.NavGraph
import com.example.kampus.ui.theme.KampusTheme
import com.example.kampus.utils.FirestoreSeedData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private const val PREFS_NAME = "kampus_prefs"
        private const val KEY_SOCIAL_SEED_CLEANED = "social_seed_cleaned_v1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Seed Firestore with sample data on first launch
        // seedFirestoreData()  // ← Disabled to use real accounts only

        clearSeededSocialDataOnce()
        
        setContent {
            KampusTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
    
    private fun seedFirestoreData() {
        lifecycleScope.launch {
            val firestore = FirebaseFirestore.getInstance()
            FirestoreSeedData.seedAllData(firestore)
        }
    }

    private fun clearSeededSocialDataOnce() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SOCIAL_SEED_CLEANED, false)) return

        lifecycleScope.launch {
            val firestore = FirebaseFirestore.getInstance()
            val cleaned = FirestoreSeedData.clearSeededSocialDataForCurrentUser(firestore)
            if (cleaned) {
                prefs.edit().putBoolean(KEY_SOCIAL_SEED_CLEANED, true).apply()
            }
        }
    }
}