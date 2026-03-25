package com.example.campussocial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.campussocial.navigation.NavGraph
import com.example.campussocial.ui.theme.KampusTheme

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