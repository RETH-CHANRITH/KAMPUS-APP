package com.example.kampus.di

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import com.example.kampus.data.remote.SupabaseStorageManager

object SupabaseModule {

    private const val SUPABASE_URL = "https://wcygigxevxohizwstkfg.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndjeWdpZ3hldnhvaGl6d3N0a2ZnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU3NDM4MTMsImV4cCI6MjA5MTMxOTgxM30.bvRqxnT7ST3dk8vg71lNec6G_QiqcVMiBtGyE6pliFQ"

    private var supabaseClient: io.github.jan.supabase.SupabaseClient? = null
    private var storageManager: SupabaseStorageManager? = null

    fun initSupabase(context: Context) {
        if (supabaseClient == null) {
            supabaseClient = createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_ANON_KEY
            ) {
                install(Storage)
            }
        }

        if (storageManager == null) {
            storageManager = SupabaseStorageManager(supabaseClient!!, context)
        }
    }

    fun getSupabaseClient(): io.github.jan.supabase.SupabaseClient {
        return supabaseClient ?: throw IllegalStateException("Supabase not initialized. Call initSupabase() first.")
    }

    fun getStorageManager(): SupabaseStorageManager {
        return storageManager ?: throw IllegalStateException("Supabase Storage Manager not initialized.")
    }
}
