package com.example.tryanderror

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoContainer = findViewById<LinearLayout>(R.id.logoContainer)
        
        // Premium Fade-in Animation
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1500
        logoContainer.startAnimation(fadeIn)

        // Delay for 2.5 seconds to show brand splash
        Handler(Looper.getMainLooper()).postDelayed({
            checkDestination()
        }, 2500)
    }

    private fun checkDestination() {
        val prefs = getSharedPreferences("news_prefs", Context.MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("isFirstTime", true)
        val user = FirebaseAuth.getInstance().currentUser

        val intent = when {
            isFirstTime -> Intent(this, OnboardingActivity::class.java)
            user != null -> Intent(this, NewsActivity::class.java)
            else -> Intent(this, SignInActivity::class.java)
        }

        startActivity(intent)
        finish()
        
        // Sleek cross-fade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
