package com.example.kampus

import android.app.Application
import com.example.kampus.di.SupabaseModule

class KampusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Supabase for image uploads
        SupabaseModule.initSupabase(this)
    }
}
