package com.example.tryanderror

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class NewsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val prefs = getSharedPreferences("news_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        
        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
