package com.example.sleeptrackerapp

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var navBar: LinearLayout
    private lateinit var selectedBackground: LinearLayout

    private lateinit var moonIcon: ImageView
    private lateinit var bellIcon: ImageView
    private lateinit var musicIcon: ImageView
    private lateinit var statsIcon: ImageView

    private var selectedPosition = 2  // Default: Music icon selected

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Connect views
        navBar = findViewById(R.id.navBar)
        selectedBackground = findViewById(R.id.selectedBackground)

        moonIcon = findViewById(R.id.moonIcon)
        bellIcon = findViewById(R.id.bellIcon)
        musicIcon = findViewById(R.id.musicIcon)
        statsIcon = findViewById(R.id.statsIcon)

        setupClickListeners()

        // Move glowing background to default item
        musicIcon.post {
            moveBackgroundTo(musicIcon, animate = false)
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

    private fun updateIconColors() {
        val icons = listOf(moonIcon, bellIcon, musicIcon, statsIcon)

        icons.forEachIndexed { index, icon ->
            if (index == selectedPosition) {
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

}
