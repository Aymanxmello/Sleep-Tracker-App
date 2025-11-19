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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity(), AccountFragment.LogoutListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var navBar: LinearLayout
    private lateinit var selectedBackground: LinearLayout
    private lateinit var viewPager: ViewPager2

    private lateinit var moonIcon: ImageView
    private lateinit var bellIcon: ImageView
    private lateinit var musicIcon: ImageView
    private lateinit var statsIcon: ImageView

    // List to easily access icons by index
    private lateinit var navIcons: List<ImageView>

    private var selectedPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.loadLocale(this)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        auth = Firebase.auth

        // Initialize Views
        viewPager = findViewById(R.id.viewPager)
        navBar = findViewById(R.id.navBar)
        selectedBackground = findViewById(R.id.selectedBackground)

        moonIcon = findViewById(R.id.moonIcon)
        bellIcon = findViewById(R.id.bellIcon)
        musicIcon = findViewById(R.id.musicIcon)
        statsIcon = findViewById(R.id.statsIcon)

        navIcons = listOf(moonIcon, bellIcon, musicIcon, statsIcon)

        setupViewPager()
        setupClickListeners()

        // Move glowing background to initial position
        moonIcon.post {
            moveBackgroundTo(moonIcon, animate = false)
            updateIconColors(0)
        }
    }

    private fun setupViewPager() {
        val pagerAdapter = MainPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 3 // Keep all fragments alive to prevent reloading
        viewPager.isUserInputEnabled = true // Enable Swipe

        // Listener for Swipe events
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Sync Navigation Bar when swiping
                if (selectedPosition != position) {
                    selectedPosition = position
                    moveBackgroundTo(navIcons[position], animate = true)
                    updateIconColors(position)
                }
            }
        })
    }

    private fun setupClickListeners() {
        moonIcon.setOnClickListener { onIconClick(0) }
        bellIcon.setOnClickListener { onIconClick(1) }
        musicIcon.setOnClickListener { onIconClick(2) }
        statsIcon.setOnClickListener { onIconClick(3) }
    }

    private fun onIconClick(position: Int) {
        if (position != selectedPosition) {
            // Move ViewPager to the clicked item
            viewPager.currentItem = position
            // Note: The OnPageChangeCallback will handle the UI updates (background move & colors)

            // Optional: Add the bounce animation on click
            val icon = navIcons[position]
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
        }
    }

    private fun updateIconColors(activePosition: Int) {
        navIcons.forEachIndexed { index, icon ->
            if (index == activePosition) {
                icon.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
            } else {
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

    override fun onLogoutClicked() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show()
    }

    // Adapter inner class
    private inner class MainPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DashboardFragment.newInstance()
                1 -> GoalsFragment.newInstance()
                2 -> TipsFragment.newInstance()
                3 -> AccountFragment.newInstance()
                else -> DashboardFragment.newInstance()
            }
        }
    }
}