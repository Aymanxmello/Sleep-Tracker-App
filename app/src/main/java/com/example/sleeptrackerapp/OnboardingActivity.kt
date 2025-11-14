package com.example.sleeptrackerapp

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView // Import pour Lottie

// Data class mise à jour pour utiliser une ressource Lottie (raw)
data class OnboardingItem(
    val lottieRes: Int,
    val title: String,
    val description: String
)

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var btnSkip: TextView
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View

    // Utilisez R.raw.NOM_DU_FICHIER_JSON pour Lottie
    // NOTE : Assurez-vous d'avoir les fichiers sleep_benefits.json, tracking.json, goals.json dans res/raw
    private val onboardingItems = listOf(
        OnboardingItem(
            R.raw.sleep_benefits, // Remplacer par R.raw.votre_fichier_1
            "Pourquoi suivre son sommeil ?",
            "Améliorez votre santé et votre bien-être en comprenant vos habitudes de sommeil. Un bon sommeil booste votre énergie et votre concentration."
        ),
        OnboardingItem(
            R.raw.tracking, // Remplacer par R.raw.votre_fichier_2
            "Enregistrement automatique",
            "Suivez votre sommeil automatiquement ou manuellement. Notre application détecte vos cycles de sommeil et analyse leur qualité."
        ),
        OnboardingItem(
            R.raw.goals, // Remplacer par R.raw.votre_fichier_3
            "Rappels & Objectifs",
            "Définissez vos objectifs de sommeil personnalisés et recevez des rappels pour maintenir une routine saine et régulière."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btn_next)
        btnSkip = findViewById(R.id.btn_skip)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)

        // Setup ViewPager
        viewPager.adapter = OnboardingAdapter(onboardingItems)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateButtons(position)
            }
        })

        // Initialisation des dots pour la première page
        updateDots(0)
        updateButtons(0)

        // Button Next/Start
        btnNext.setOnClickListener {
            if (viewPager.currentItem < onboardingItems.size - 1) {
                viewPager.currentItem += 1
            } else {
                finishOnboarding()
            }
        }

        // Button Skip
        btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun updateDots(position: Int) {
        val dots = listOf(dot1, dot2, dot3)
        dots.forEachIndexed { index, dot ->
            animateDot(dot, index == position)
        }
    }

    // Animation de mise à l'échelle pour l'indicateur actif
    private fun animateDot(dot: View, isActive: Boolean) {
        val scaleValue = if (isActive) 1.5f else 1f

        ObjectAnimator.ofFloat(dot, "scaleX", scaleValue).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(dot, "scaleY", scaleValue).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        dot.setBackgroundResource(if (isActive) R.drawable.dot_active else R.drawable.dot_inactive)
    }

    private fun updateButtons(position: Int) {
        if (position == onboardingItems.size - 1) {
            btnNext.text = "Commencer"
            btnSkip.visibility = View.GONE
        } else {
            btnNext.text = "Suivant"
            btnSkip.visibility = View.VISIBLE
        }
    }

    private fun finishOnboarding() {
        // Sauvegarder que l'onboarding est terminé
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()

        // Aller vers MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

// Adapter pour ViewPager2
class OnboardingAdapter(private val items: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.onboarding_page, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val lottieAnimationView: LottieAnimationView = itemView.findViewById(R.id.animation_view)
        private val title: TextView = itemView.findViewById(R.id.page_title)
        private val description: TextView = itemView.findViewById(R.id.page_description)

        fun bind(item: OnboardingItem) {
            // Configuration Lottie
            lottieAnimationView.setAnimation(item.lottieRes)
            lottieAnimationView.repeatCount = LottieAnimationView.INFINITE
            lottieAnimationView.playAnimation()

            title.text = item.title
            description.text = item.description

            // Animations d'entrée du texte (réinitialisation et animation)
            title.alpha = 0f
            description.alpha = 0f
            title.translationY = 50f
            description.translationY = 50f

            title.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            description.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }
}