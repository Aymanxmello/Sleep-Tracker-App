package com.example.sleeptrackerapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.work.WorkManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class AccountFragment : Fragment() {

    // Gestionnaire de résultat pour la permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Si accepté, on active tout
            switchNotifications.isChecked = true
        } else {
            // Si refusé, on décoche le switch
            switchNotifications.isChecked = false
            Toast.makeText(context, "Permission nécessaire pour les rappels", Toast.LENGTH_SHORT).show()
        }
    }

    interface LogoutListener {
        fun onLogoutClicked()
    }

    private var logoutListener: LogoutListener? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Vues
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvDailyGoal: TextView
    private lateinit var tvTimezone: TextView
    private lateinit var tvReminderPref: TextView // Heure coucher
    private lateinit var tvWakeupPref: TextView   // Heure réveil
    private lateinit var layoutBedtime: LinearLayout
    private lateinit var layoutWakeup: LinearLayout
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var btnLogout: Button
    private lateinit var btnExportCsv: Button

    // Variables pour stocker les heures choisies (valeurs par défaut)
    private var bedTimeHour = 22
    private var bedTimeMinute = 30
    private var wakeUpHour = 7
    private var wakeUpMinute = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LogoutListener) {
            logoutListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews(view)
        loadUserData()

        // Charger les heures sauvegardées
        loadTimePreferences()

        btnLogout.setOnClickListener {
            logoutListener?.onLogoutClicked()
        }

        // Click Listener pour l'heure de coucher
        layoutBedtime.setOnClickListener {
            showTimePicker("Heure de coucher", bedTimeHour, bedTimeMinute) { hour, minute ->
                bedTimeHour = hour
                bedTimeMinute = minute
                updateTimeUI()
                saveTimePreferences()
                // Reprogrammer si les notifs sont actives
                if (switchNotifications.isChecked) {
                    scheduleSleepReminder(requireContext(), bedTimeHour, bedTimeMinute)
                    Toast.makeText(context, "Rappel coucher mis à jour", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Click Listener pour l'heure de réveil
        layoutWakeup.setOnClickListener {
            showTimePicker("Heure de réveil", wakeUpHour, wakeUpMinute) { hour, minute ->
                wakeUpHour = hour
                wakeUpMinute = minute
                updateTimeUI()
                saveTimePreferences()
                // Reprogrammer si les notifs sont actives
                if (switchNotifications.isChecked) {
                    scheduleWakeUpReminder(requireContext(), wakeUpHour, wakeUpMinute)
                    Toast.makeText(context, "Rappel réveil mis à jour", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Gestion du switch Notifications
        switchNotifications.setOnCheckedChangeListener { buttonView, isChecked ->
            val context = requireContext()

            if (isChecked) {
                // VÉRIFICATION DE LA PERMISSION (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        // On décoche temporairement pour éviter une boucle, le temps de demander
                        buttonView.isChecked = false
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@setOnCheckedChangeListener
                    }
                }

                // Si on a la permission, on programme
                scheduleSleepReminder(context, bedTimeHour, bedTimeMinute)
                scheduleWakeUpReminder(context, wakeUpHour, wakeUpMinute)
                scheduleInactivityCheck(context)
                Toast.makeText(context, "Rappels activés", Toast.LENGTH_SHORT).show()

            } else {
                cancelSleepReminder(context)
                WorkManager.getInstance(context).cancelUniqueWork("DailyWakeUpReminder")
                WorkManager.getInstance(context).cancelUniqueWork("InactivityCheck")
                Toast.makeText(context, "Rappels désactivés", Toast.LENGTH_SHORT).show()            }
        }


        btnExportCsv.setOnClickListener { exportDataToCsv() }

        return view
    }

    private fun initializeViews(view: View) {
        tvUsername = view.findViewById(R.id.tv_username)
        tvEmail = view.findViewById(R.id.tv_email)
        tvDailyGoal = view.findViewById(R.id.tv_daily_goal)
        tvTimezone = view.findViewById(R.id.tv_timezone)

        tvReminderPref = view.findViewById(R.id.tv_reminder_pref)
        tvWakeupPref = view.findViewById(R.id.tv_wakeup_pref)
        layoutBedtime = view.findViewById(R.id.layout_bedtime)
        layoutWakeup = view.findViewById(R.id.layout_wakeup)

        switchNotifications = view.findViewById(R.id.switch_notifications)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnExportCsv = view.findViewById(R.id.btn_export_csv)
    }

    // Afficher le Material Time Picker
    private fun showTimePicker(title: String, hour: Int, minute: Int, onTimeSelected: (Int, Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText(title)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()

        picker.addOnPositiveButtonClickListener {
            onTimeSelected(picker.hour, picker.minute)
        }

        picker.show(parentFragmentManager, "TimePicker")
    }

    private fun updateTimeUI() {
        tvReminderPref.text = String.format("%02d:%02d", bedTimeHour, bedTimeMinute)
        tvWakeupPref.text = String.format("%02d:%02d", wakeUpHour, wakeUpMinute)
    }

    // Sauvegarder dans SharedPreferences
    private fun saveTimePreferences() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("bed_hour", bedTimeHour)
            putInt("bed_min", bedTimeMinute)
            putInt("wake_hour", wakeUpHour)
            putInt("wake_min", wakeUpMinute)
            apply()
        }
    }

    // Charger depuis SharedPreferences
    private fun loadTimePreferences() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        bedTimeHour = sharedPref.getInt("bed_hour", 22)
        bedTimeMinute = sharedPref.getInt("bed_min", 30)
        wakeUpHour = sharedPref.getInt("wake_hour", 7)
        wakeUpMinute = sharedPref.getInt("wake_min", 0)
        updateTimeUI()
    }

    private fun loadUserData() {
        val user = auth.currentUser
        tvEmail.text = user?.email ?: "utilisateur@example.com"
        tvTimezone.text = TimeZone.getDefault().id

        if (user != null) {
            firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    tvUsername.text = document.getString("username") ?: "Utilisateur"
                }
            firestore.collection("users").document(user.uid).collection("goals")
                .orderBy("dateCreated", Query.Direction.DESCENDING).limit(1).get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        tvDailyGoal.text = "${docs.documents[0].getDouble("duration")}h"
                    }
                }
        }
    }

    fun exportDataToCsv() {
        // (Code d'exportation CSV identique à avant...)
        val user = auth.currentUser ?: return
        Toast.makeText(context, "Génération du CSV...", Toast.LENGTH_SHORT).show()

        firestore.collection("users").document(user.uid).collection("sleep_sessions")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val fileName = "sleep_data_export.csv"
                    val file = File(requireContext().cacheDir, fileName)
                    val writer = FileWriter(file)
                    writer.append("Date,Durée (h),Qualité\n")
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    for (doc in documents) {
                        val dateMillis = doc.getLong("date") ?: 0L
                        val dateStr = dateFormat.format(Date(dateMillis))
                        val duration = doc.getDouble("durationHours") ?: 0.0
                        val quality = doc.getString("qualityText") ?: "N/A"
                        writer.append("$dateStr,$duration,$quality\n")
                    }
                    writer.flush()
                    writer.close()
                    shareCsvFile(file)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    private fun shareCsvFile(file: File) {
        val content = file.readText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mon historique de sommeil")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, "Exporter via"))
    }

    override fun onDetach() {
        super.onDetach()
        logoutListener = null
    }

    companion object {
        fun newInstance() = AccountFragment()
    }
}