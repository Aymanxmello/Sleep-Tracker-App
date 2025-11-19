package com.example.sleeptrackerapp

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity(), AccountFragment.LogoutListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var navBar: LinearLayout
    private lateinit var selectedBackground: LinearLayout

    private lateinit var moonIcon: ImageView
    private lateinit var bellIcon: ImageView
    private lateinit var musicIcon: ImageView
    private lateinit var statsIcon: ImageView

    private val fragments = listOf(
        DashboardFragment.newInstance(),
        GoalsFragment.newInstance(),
        TipsFragment.newInstance(),
        AccountFragment.newInstance()
    )

    // CHANGED: Set initial selected position to 0 (Dashboard Fragment / Moon Icon)
    private var selectedPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.loadLocale(this)
        setContentView(R.layout.activity_main)

        // Initialisation de Firebase
        auth = Firebase.auth

        // Connect views
        navBar = findViewById(R.id.navBar)
        selectedBackground = findViewById(R.id.selectedBackground)

        moonIcon = findViewById(R.id.moonIcon)
        bellIcon = findViewById(R.id.bellIcon)
        musicIcon = findViewById(R.id.musicIcon)
        statsIcon = findViewById(R.id.statsIcon)

        setupClickListeners()

        // 3. Set the initial fragment and move background
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragments[selectedPosition])
            .commit()

        // Move glowing background to default item (needs to be posted to get correct measurements)
        moonIcon.post { // CHANGED: Use moonIcon for initial placement
            moveBackgroundTo(moonIcon, animate = false)
            updateIconColors()
        }
    }

    private fun setupClickListeners() {
        moonIcon.setOnClickListener { onIconClick(0, moonIcon) }
        bellIcon.setOnClickListener { onIconClick(1, bellIcon) }
        musicIcon.setOnClickListener { onIconClick(2, musicIcon) }
        statsIcon.setOnClickListener { onIconClick(3, statsIcon) }
    }

    private fun onIconClick(position: Int, icon: ImageView) {
        if (position != selectedPosition) {

            // 4. Switch Fragment Content
            switchFragment(position)

            // Bounce animation on tap
            icon.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150)
                .withEndAction {
                    icon.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()
                }
                .start()

            selectedPosition = position
            moveBackgroundTo(icon, animate = true)
            updateIconColors()
        }
    }

    private fun switchFragment(position: Int) {
        val nextFragment = fragments[position]
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, nextFragment)
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .commit()
    }

    private fun updateIconColors() {
        val icons = listOf(moonIcon, bellIcon, musicIcon, statsIcon)

        icons.forEachIndexed { index, icon ->
            if (index == selectedPosition) {
                // Set active color to white
                icon.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
            } else {
                // Set inactive color (R.color.icon_unselected)
                icon.setColorFilter(ContextCompat.getColor(this, R.color.icon_unselected))
            }
        }
    }

    private fun moveBackgroundTo(targetView: ImageView, animate: Boolean) {

        val targetX = targetView.x + (targetView.width / 2) - (selectedBackground.width / 2)

        if (animate) {
            val animator = ValueAnimator.ofFloat(selectedBackground.x, targetX)
            animator.duration = 400
            animator.interpolator = OvershootInterpolator(1.4f)
            animator.addUpdateListener { animation ->
                selectedBackground.x = animation.animatedValue as Float
            }
            animator.start()
        } else {
            selectedBackground.x = targetX
        }
    }

    // 2. Implementation of the LogoutListener method
    override fun onLogoutClicked() {
        auth.signOut() // Sign out the user from Firebase

        // Navigate back to LoginActivity and clear the activity stack
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show()
    }
}