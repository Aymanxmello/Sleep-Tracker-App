package com.example.sleeptrackerapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity


class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 3000 // 3 secondes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)

            val intent = if (onboardingCompleted) {
                Intent(this, LoginActivity::class.java) // Navigates to LoginActivity if onboarding is complete
            } else {
                Intent(this, OnboardingActivity::class.java)
            }

            startActivity(intent)
            finish()
        }, SPLASH_DELAY)
    }
}