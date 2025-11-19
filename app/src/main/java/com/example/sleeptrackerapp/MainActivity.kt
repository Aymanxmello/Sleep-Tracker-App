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
    private lateinit var viewPager: ViewPager2 // Ajout du ViewPager

    private lateinit var moonIcon: ImageView
    private lateinit var bellIcon: ImageView
    private lateinit var musicIcon: ImageView
    private lateinit var statsIcon: ImageView

    // On garde la liste pour référence
    private val fragments = listOf(
        DashboardFragment.newInstance(),
        GoalsFragment.newInstance(),
        TipsFragment.newInstance(),
        AccountFragment.newInstance()
    )

    private var selectedPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.loadLocale(this)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        // Liaison des vues
        viewPager = findViewById(R.id.viewPager) // Correspond à l'ID dans le XML
        navBar = findViewById(R.id.navBar)
        selectedBackground = findViewById(R.id.selectedBackground)

        moonIcon = findViewById(R.id.moonIcon)
        bellIcon = findViewById(R.id.bellIcon)
        musicIcon = findViewById(R.id.musicIcon)
        statsIcon = findViewById(R.id.statsIcon)

        // Configuration du ViewPager pour le Swipe
        setupViewPager()
        setupClickListeners()

        // Initialisation UI (placement du fond blanc sur la 1ère icône)
        moonIcon.post {
            moveBackgroundTo(moonIcon, animate = false)
            updateIconColors()
        }
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter

        // C'est ici que la magie du swipe opère :
        // Quand on change de page (par swipe), on met à jour le menu du bas
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position != selectedPosition) {
                    val icon = getIconForPosition(position)

                    // Mettre à jour la position
                    selectedPosition = position

                    // Déclencher les animations (fond + couleur + rebond)
                    moveBackgroundTo(icon, animate = true)
                    updateIconColors()
                    animateIconBounce(icon)
                }
            }
        })
    }

    private fun setupClickListeners() {
        // Au clic, on demande au ViewPager de changer de page
        // (le callback onPageSelected ci-dessus s'occupera de l'animation)
        moonIcon.setOnClickListener { viewPager.currentItem = 0 }
        bellIcon.setOnClickListener { viewPager.currentItem = 1 }
        musicIcon.setOnClickListener { viewPager.currentItem = 2 }
        statsIcon.setOnClickListener { viewPager.currentItem = 3 }
    }

    // Helper pour récupérer l'icône selon la position
    private fun getIconForPosition(position: Int): ImageView {
        return when (position) {
            0 -> moonIcon
            1 -> bellIcon
            2 -> musicIcon
            3 -> statsIcon
            else -> moonIcon
        }
    }

    private fun animateIconBounce(icon: ImageView) {
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

    override fun onLogoutClicked() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show()
    }

    // Adaptateur obligatoire pour le ViewPager2
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