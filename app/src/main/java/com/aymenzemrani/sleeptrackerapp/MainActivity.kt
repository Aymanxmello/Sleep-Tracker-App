package com.aymenzemrani.sleeptrackerapp

import android.animation.ValueAnimator
import android.content.Context
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
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), AccountFragment.LogoutListener, DataClient.OnDataChangedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var navBar: LinearLayout
    private lateinit var selectedBackground: LinearLayout
    private lateinit var viewPager: ViewPager2

    private lateinit var moonIcon: ImageView
    private lateinit var bellIcon: ImageView
    private lateinit var musicIcon: ImageView
    private lateinit var statsIcon: ImageView

    private var selectedPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.loadLocale(this)
        setContentView(R.layout.activity_main)


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        // Bind Views
        viewPager = findViewById(R.id.viewPager)
        navBar = findViewById(R.id.navBar)
        selectedBackground = findViewById(R.id.selectedBackground)

        moonIcon = findViewById(R.id.moonIcon)
        bellIcon = findViewById(R.id.bellIcon)
        musicIcon = findViewById(R.id.musicIcon)
        statsIcon = findViewById(R.id.statsIcon)

        setupViewPager()
        setupClickListeners()

        moonIcon.post {
            moveBackgroundTo(moonIcon, animate = false)
            updateIconColors()
        }
    }

    // --- WEAR OS LISTENER ---
    override fun onResume() {
        super.onResume()
        // Start listening for watch data
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        // Stop listening to save battery
        Wearable.getDataClient(this).removeListener(this)
    }

    // This function runs when the watch sends data
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/sleep_data") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                // 1. Get raw data from Watch
                val durationSeconds = dataMap.getLong("duration")
                val movements = dataMap.getInt("movements")
                val timestamp = dataMap.getLong("timestamp")

                // 2. Process data (Same logic as your Manual Method)
                processAndSaveWatchData(durationSeconds, movements, timestamp)
            }
        }
    }

    private fun processAndSaveWatchData(durationSec: Long, movements: Int, timestamp: Long) {
        val user = auth.currentUser ?: return

        // Calculate Hours and Minutes
        val hours = durationSec / 3600
        val minutes = (durationSec % 3600) / 60
        val durationFloat = hours + (minutes / 60f)
        val durationString = String.format("%02d:%02d", hours, minutes)

        // Calculate Quality (Based on movements per hour)
        val movementsPerHour = if (hours > 0) movements / hours else movements.toLong()
        val (qualityText, qualityScore) = when {
            movementsPerHour < 5 -> "Excellente" to 100
            movementsPerHour < 15 -> "Bonne" to 80
            movementsPerHour < 30 -> "Moyenne" to 60
            movementsPerHour < 50 -> "Mauvaise" to 40
            else -> "Très mauvaise" to 20
        }

        // 3. SAVE LOCAL (For "Dernière nuit" / Last Night display)
        val sharedPref = getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("last_sleep_date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            .putString("last_sleep_duration", durationString)
            .putString("last_sleep_quality", qualityText)
            .apply()

        // 4. SAVE TO FIREBASE (For Charts)
        val sleepSession = hashMapOf(
            "date" to timestamp,
            "durationString" to durationString,
            "durationHours" to durationFloat,
            "qualityText" to qualityText,
            "qualityScore" to qualityScore,
            "source" to "Smart Watch", // Mark source as Watch
            "movements" to movements
        )

        firestore.collection("users").document(user.uid)
            .collection("sleep_sessions")
            .add(sleepSession)
            .addOnSuccessListener {
                runOnUiThread {
                    Toast.makeText(this, "Données de la montre reçues et sauvegardées !", Toast.LENGTH_LONG).show()
                    // Refresh UI if on dashboard
                    viewPager.adapter?.notifyDataSetChanged()
                }
            }
    }
    // --- END WEAR OS LOGIC ---

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position != selectedPosition) {
                    val icon = getIconForPosition(position)
                    selectedPosition = position
                    moveBackgroundTo(icon, animate = true)
                    updateIconColors()
                    animateIconBounce(icon)
                }
            }
        })
    }

    private fun setupClickListeners() {
        moonIcon.setOnClickListener { viewPager.currentItem = 0 }
        bellIcon.setOnClickListener { viewPager.currentItem = 1 }
        musicIcon.setOnClickListener { viewPager.currentItem = 2 }
        statsIcon.setOnClickListener { viewPager.currentItem = 3 }
    }

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
        icon.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).withEndAction {
            icon.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }.start()
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