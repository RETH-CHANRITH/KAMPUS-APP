package com.example.kampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.kampus.navigation.NavGraph
import com.example.kampus.ui.theme.KampusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KampusTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}