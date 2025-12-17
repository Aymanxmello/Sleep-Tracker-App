package com.aymenzemrani.sleeptrackerapp

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
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
import java.util.Random
import java.util.TimeZone

class AccountFragment : Fragment() {

    interface LogoutListener {
        fun onLogoutClicked()
    }

    private var logoutListener: LogoutListener? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Views
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvDailyGoal: TextView
    private lateinit var tvTimezone: TextView

    private lateinit var tvReminderPref: TextView
    private lateinit var tvWakeupPref: TextView
    private lateinit var layoutBedtime: LinearLayout
    private lateinit var layoutWakeup: LinearLayout

    private lateinit var layoutLanguage: LinearLayout
    private lateinit var tvCurrentLanguage: TextView

    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var btnExportCsv: Button
    private lateinit var btnBackupEncrypted: Button
    private lateinit var btnLogout: Button

    // Default Data
    private var bedTimeHour = 22
    private var bedTimeMinute = 30
    private var wakeUpHour = 7
    private var wakeUpMinute = 0

    // Permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            switchNotifications.isChecked = true
            activateAllReminders()
        } else {
            switchNotifications.isChecked = false
            Toast.makeText(context, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
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
        loadTimePreferences()
        updateLanguageUI()

        setupClickListeners()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize the starry background animation here
        setupStarryBackground(view)
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

        layoutLanguage = view.findViewById(R.id.layout_language)
        tvCurrentLanguage = view.findViewById(R.id.tv_current_language)

        switchNotifications = view.findViewById(R.id.switch_notifications)

        btnExportCsv = view.findViewById(R.id.btn_export_csv)
        btnBackupEncrypted = view.findViewById(R.id.btn_backup_encrypted)
        btnLogout = view.findViewById(R.id.btn_logout)
    }

    // --- NEW FUNCTION FOR STARS ---
    private fun setupStarryBackground(view: View) {
        val container = view.findViewById<FrameLayout>(R.id.star_container)
        if (container == null) return

        // Use post to wait for layout measurement
        container.post {
            val width = container.width
            val height = container.height
            val random = Random()

            // Create 50 stars
            for (i in 0 until 50) {
                val star = View(context)

                // 1. White Circle Shape
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(Color.WHITE)
                star.background = drawable

                // 2. Random Size (2dp to 5dp)
                val density = resources.displayMetrics.density
                val size = ((2..5).random() * density).toInt()
                val params = FrameLayout.LayoutParams(size, size)

                // 3. Random Position
                params.leftMargin = random.nextInt(width)
                params.topMargin = random.nextInt(height)
                star.layoutParams = params

                // 4. Add to layout
                container.addView(star)

                // 5. Animation (Fade In/Out)
                star.alpha = random.nextFloat()
                val animator = ObjectAnimator.ofFloat(star, "alpha", 0.2f, 1f, 0.2f)
                animator.duration = (1500..4000).random().toLong()
                animator.startDelay = (0..2000).random().toLong()
                animator.repeatCount = ValueAnimator.INFINITE
                animator.repeatMode = ValueAnimator.REVERSE
                animator.start()
            }
        }
    }
    // -----------------------------

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            logoutListener?.onLogoutClicked()
        }

        layoutBedtime.setOnClickListener {
            showTimePicker(getString(R.string.bedtime_reminder), bedTimeHour, bedTimeMinute) { h, m ->
                bedTimeHour = h
                bedTimeMinute = m
                saveTimePreferences()
                updateTimeUI()
                if (switchNotifications.isChecked) scheduleSleepReminder(requireContext(), bedTimeHour, bedTimeMinute)
            }
        }

        layoutWakeup.setOnClickListener {
            showTimePicker(getString(R.string.wakeup_reminder), wakeUpHour, wakeUpMinute) { h, m ->
                wakeUpHour = h
                wakeUpMinute = m
                saveTimePreferences()
                updateTimeUI()
                if (switchNotifications.isChecked) scheduleWakeUpReminder(requireContext(), wakeUpHour, wakeUpMinute)
            }
        }

        layoutLanguage.setOnClickListener {
            showLanguageDialog()
        }

        switchNotifications.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        view.isChecked = false
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@setOnCheckedChangeListener
                    }
                }
                activateAllReminders()
            } else {
                cancelAllReminders()
            }
        }

        btnExportCsv.setOnClickListener { exportDataToCsv() }
        btnBackupEncrypted.setOnClickListener { performEncryptedBackup() }
    }

    private fun activateAllReminders() {
        val context = requireContext()
        scheduleSleepReminder(context, bedTimeHour, bedTimeMinute)
        scheduleWakeUpReminder(context, wakeUpHour, wakeUpMinute)
        // scheduleInactivityCheck(context) // Uncomment if you have this worker
        Toast.makeText(context, getString(R.string.reminders_enabled), Toast.LENGTH_SHORT).show()
    }

    private fun cancelAllReminders() {
        val context = requireContext()
        cancelSleepReminder(context)
        WorkManager.getInstance(context).cancelUniqueWork("DailyWakeUpReminder")
        WorkManager.getInstance(context).cancelUniqueWork("InactivityCheck")
        Toast.makeText(context, getString(R.string.reminders_disabled), Toast.LENGTH_SHORT).show()
    }

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
        tvReminderPref.text = String.format(Locale.US, "%02d:%02d", bedTimeHour, bedTimeMinute)
        tvWakeupPref.text = String.format(Locale.US, "%02d:%02d", wakeUpHour, wakeUpMinute)
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

    private fun updateLanguageUI() {
        tvCurrentLanguage.text = getString(R.string.current_language_name)
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Français", "العربية")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.language_dialog_title))
        builder.setItems(languages) { _, which ->
            when (which) {
                0 -> changeLanguage("en")
                1 -> changeLanguage("fr")
                2 -> changeLanguage("ar")
            }
        }
        builder.show()
    }

    private fun changeLanguage(langCode: String) {
        LocaleHelper.setLocale(requireContext(), langCode)
        LocaleHelper.restartApp(requireActivity())
    }

    private fun exportDataToCsv() {
        val user = auth.currentUser ?: return
        Toast.makeText(context, getString(R.string.csv_generating), Toast.LENGTH_SHORT).show()

        firestore.collection("users").document(user.uid).collection("sleep_sessions")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val fileName = "sleep_export.csv"
                    val file = File(requireContext().cacheDir, fileName)
                    val writer = FileWriter(file)

                    writer.append("Date,Time,Duration (h),Quality,Movements,Source\n")
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

                    for (doc in documents) {
                        val dateMillis = doc.getLong("date") ?: 0L
                        val dateStr = dateFormat.format(Date(dateMillis))
                        val timeStr = timeFormat.format(Date(dateMillis))
                        val duration = doc.getDouble("durationHours") ?: 0.0
                        val quality = doc.getString("qualityText") ?: "N/A"
                        val movements = doc.getLong("movements") ?: 0
                        val source = doc.getString("source") ?: "Manual"
                        writer.append("$dateStr,$timeStr,$duration,$quality,$movements,$source\n")
                    }
                    writer.flush()
                    writer.close()
                    shareFile(file, "text/csv", getString(R.string.export_csv))
                } catch (e: Exception) {
                    Toast.makeText(context, getString(R.string.error_export), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun performEncryptedBackup() {
        val user = auth.currentUser ?: return
        Toast.makeText(context, getString(R.string.backup_encrypting), Toast.LENGTH_SHORT).show()

        firestore.collection("users").document(user.uid).collection("sleep_sessions")
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val dataList = documents.map { it.data }
                    val jsonString = Gson().toJson(dataList)
                    val fileName = "backup_secure_${System.currentTimeMillis()}.enc"
                    val file = File(requireContext().filesDir, fileName)
                    if(file.exists()) file.delete()

                    val mainKey = MasterKey.Builder(requireContext())
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

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
                    shareFile(file, "application/octet-stream", getString(R.string.backup_encrypted))
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun shareFile(file: File, mimeType: String, title: String) {
        try {
            val authority = "${requireContext().packageName}.provider"
            val uri = FileProvider.getUriForFile(requireContext(), authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        tvEmail.text = user?.email ?: "user@example.com"
        tvTimezone.text = TimeZone.getDefault().id
        if (user != null) {
            firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    tvUsername.text = document.getString("username") ?: "User"
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