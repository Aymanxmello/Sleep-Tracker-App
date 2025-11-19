package com.example.sleeptrackerapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.work.WorkManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AccountFragment : Fragment() {

    interface LogoutListener {
        fun onLogoutClicked()
    }

    private var logoutListener: LogoutListener? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // --- VUES ---
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvDailyGoal: TextView
    private lateinit var tvTimezone: TextView

    // Préférences Heures
    private lateinit var tvReminderPref: TextView // Texte Heure Coucher
    private lateinit var tvWakeupPref: TextView   // Texte Heure Réveil
    private lateinit var layoutBedtime: LinearLayout
    private lateinit var layoutWakeup: LinearLayout

    // Contrôles
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var btnExportCsv: Button
    private lateinit var btnBackupEncrypted: Button
    private lateinit var btnLogout: Button

    // --- DONNÉES LOCALES ---
    // Heures par défaut
    private var bedTimeHour = 22
    private var bedTimeMinute = 30
    private var wakeUpHour = 7
    private var wakeUpMinute = 0

    // --- GESTION PERMISSION (Android 13+) ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            switchNotifications.isChecked = true
            activateAllReminders()
        } else {
            switchNotifications.isChecked = false
            Toast.makeText(context, "Permission refusée. Les rappels ne fonctionneront pas.", Toast.LENGTH_LONG).show()
        }
    }

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
        loadTimePreferences() // Charger les heures sauvegardées

        setupClickListeners()

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

        btnExportCsv = view.findViewById(R.id.btn_export_csv)
        btnBackupEncrypted = view.findViewById(R.id.btn_backup_encrypted)
        btnLogout = view.findViewById(R.id.btn_logout)
    }

    private fun setupClickListeners() {
        // 1. Déconnexion
        btnLogout.setOnClickListener {
            logoutListener?.onLogoutClicked()
        }

        // 2. Choix Heure Coucher
        layoutBedtime.setOnClickListener {
            showTimePicker("Heure de coucher", bedTimeHour, bedTimeMinute) { h, m ->
                bedTimeHour = h
                bedTimeMinute = m
                saveTimePreferences()
                updateTimeUI()
                if (switchNotifications.isChecked) scheduleSleepReminder(requireContext(), bedTimeHour, bedTimeMinute)
            }
        }

        // 3. Choix Heure Réveil
        layoutWakeup.setOnClickListener {
            showTimePicker("Heure de réveil", wakeUpHour, wakeUpMinute) { h, m ->
                wakeUpHour = h
                wakeUpMinute = m
                saveTimePreferences()
                updateTimeUI()
                if (switchNotifications.isChecked) scheduleWakeUpReminder(requireContext(), wakeUpHour, wakeUpMinute)
            }
        }

        // 4. Switch Notifications
        switchNotifications.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                // Vérifier permission Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        view.isChecked = false // On décoche en attendant la réponse
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@setOnCheckedChangeListener
                    }
                }
                activateAllReminders()
            } else {
                cancelAllReminders()
            }
        }

        // 5. Export CSV
        btnExportCsv.setOnClickListener { exportDataToCsv() }

        // 6. Sauvegarde Chiffrée
        btnBackupEncrypted.setOnClickListener { performEncryptedBackup() }
    }

    // --- GESTION DES RAPPELS (WORKMANAGER) ---

    private fun activateAllReminders() {
        val context = requireContext()
        scheduleSleepReminder(context, bedTimeHour, bedTimeMinute)
        scheduleWakeUpReminder(context, wakeUpHour, wakeUpMinute)
        scheduleInactivityCheck(context)
        Toast.makeText(context, "Rappels activés", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAllReminders() {
        val context = requireContext()
        cancelSleepReminder(context)
        WorkManager.getInstance(context).cancelUniqueWork("DailyWakeUpReminder")
        WorkManager.getInstance(context).cancelUniqueWork("InactivityCheck")
        Toast.makeText(context, "Rappels désactivés", Toast.LENGTH_SHORT).show()
    }

    // --- GESTION DU TEMPS (TIMEPICKER) ---

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

    private fun loadTimePreferences() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        bedTimeHour = sharedPref.getInt("bed_hour", 22)
        bedTimeMinute = sharedPref.getInt("bed_min", 30)
        wakeUpHour = sharedPref.getInt("wake_hour", 7)
        wakeUpMinute = sharedPref.getInt("wake_min", 0)
        updateTimeUI()
    }

    // --- EXPORT CSV ---

    private fun exportDataToCsv() {
        val user = auth.currentUser ?: return
        Toast.makeText(context, "Génération du CSV...", Toast.LENGTH_SHORT).show()

        firestore.collection("users").document(user.uid).collection("sleep_sessions")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val fileName = "sommeil_export.csv"
                    val file = File(requireContext().cacheDir, fileName)
                    val writer = FileWriter(file)

                    // En-tête CSV
                    writer.append("Date,Heure,Durée (h),Qualité,Mouvements,Source\n")

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    for (doc in documents) {
                        val dateMillis = doc.getLong("date") ?: 0L
                        val dateStr = dateFormat.format(Date(dateMillis))
                        val timeStr = timeFormat.format(Date(dateMillis))
                        val duration = doc.getDouble("durationHours") ?: 0.0
                        val quality = doc.getString("qualityText") ?: "N/A"
                        val movements = doc.getLong("movements") ?: 0
                        val source = doc.getString("source") ?: "Manuel"

                        writer.append("$dateStr,$timeStr,$duration,$quality,$movements,$source\n")
                    }
                    writer.flush()
                    writer.close()

                    shareFile(file, "text/csv", "Export Données Sommeil")

                } catch (e: Exception) {
                    Toast.makeText(context, "Erreur Export: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
    }

    // --- SAUVEGARDE CHIFFRÉE (AES-256) ---

    private fun performEncryptedBackup() {
        val user = auth.currentUser ?: return
        Toast.makeText(context, "Chiffrement en cours...", Toast.LENGTH_SHORT).show()

        firestore.collection("users").document(user.uid).collection("sleep_sessions")
            .get()
            .addOnSuccessListener { documents ->
                try {
                    // 1. Conversion en JSON via Gson
                    val dataList = documents.map { it.data }
                    val jsonString = Gson().toJson(dataList)

                    // 2. Fichier destination
                    val fileName = "backup_secure_${System.currentTimeMillis()}.enc"
                    val file = File(requireContext().filesDir, fileName) // Utilisation de filesDir pour sécurité accrue

                    if(file.exists()) file.delete()

                    // 3. Clé Maître Android Keystore
                    val mainKey = MasterKey.Builder(requireContext())
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    // 4. Chiffrement
                    val encryptedFile = EncryptedFile.Builder(
                        requireContext(),
                        file,
                        mainKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                    ).build()

                    val outputStream = encryptedFile.openFileOutput()
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                    outputStream.close()

                    // 5. Partage
                    shareFile(file, "application/octet-stream", "Sauvegarde Chiffrée")

                } catch (e: Exception) {
                    Toast.makeText(context, "Erreur Chiffrement: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
    }

    // --- FONCTION UTILITAIRE PARTAGE ---

    private fun shareFile(file: File, mimeType: String, title: String) {
        try {
            // ATTENTION : Nécessite la configuration correcte du FileProvider dans AndroidManifest.xml
            // Authority doit correspondre à "applicationId + .provider"
            val authority = "${requireContext().packageName}.provider"

            val uri = FileProvider.getUriForFile(requireContext(), authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, title))

        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, "Erreur FileProvider. Vérifiez le Manifest.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible de partager le fichier.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- CHARGEMENT DONNÉES FIREBASE ---

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

    override fun onDetach() {
        super.onDetach()
        logoutListener = null
    }

    companion object {
        fun newInstance() = AccountFragment()
    }
}