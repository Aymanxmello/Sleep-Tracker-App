package com.example.sleeptrackerapp

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SleepTrackingDialogFragment : DialogFragment() {

    private lateinit var etSleepDuration: TextInputEditText
    private lateinit var spinnerSleepQuality: Spinner
    private lateinit var btnStartManual: MaterialButton
    private lateinit var btnAddManual: MaterialButton
    private lateinit var tvCurrentTime: TextView
    private lateinit var btnClose: ImageButton

    private lateinit var timer: CountDownTimer
    private var isTracking = false
    private var startTimeMillis: Long = 0

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_sleep_tracking, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews(view)
        setupSpinner()
        setupClickListeners()
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun initializeViews(view: View) {
        etSleepDuration = view.findViewById(R.id.et_sleep_duration)
        spinnerSleepQuality = view.findViewById(R.id.spinner_sleep_quality)
        btnStartManual = view.findViewById(R.id.btn_start_manual)
        btnAddManual = view.findViewById(R.id.btn_add_manual)
        tvCurrentTime = view.findViewById(R.id.tv_current_time)
        btnClose = view.findViewById(R.id.btn_close)

        updateCurrentTime()
    }

    private fun setupSpinner() {
        val qualityOptions = arrayOf("Excellente", "Bonne", "Moyenne", "Mauvaise", "Très mauvaise")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, qualityOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSleepQuality.adapter = adapter
    }

    private fun setupClickListeners() {
        btnStartManual.setOnClickListener {
            if (!isTracking) {
                startSleepTracking()
            } else {
                stopSleepTracking()
            }
        }

        btnAddManual.setOnClickListener {
            addManualSleepEntry()
        }

        btnClose.setOnClickListener {
            if (isTracking) {
                stopTimer()
                isTracking = false
            }
            dismiss()
        }
    }

    private fun startSleepTracking() {
        isTracking = true
        btnStartManual.text = "Arrêter le suivi"
        btnStartManual.setBackgroundColor(requireContext().getColor(android.R.color.holo_red_dark))

        startTimeMillis = System.currentTimeMillis()
        startTimer()

        etSleepDuration.isEnabled = false
        spinnerSleepQuality.isEnabled = false
        btnAddManual.isEnabled = false
        etSleepDuration.setText("")

        Toast.makeText(requireContext(), "Suivi du sommeil démarré", Toast.LENGTH_SHORT).show()
    }

    private fun stopSleepTracking() {
        val endTimeMillis = System.currentTimeMillis()
        isTracking = false
        btnStartManual.text = "Démarrer le suivi"
        btnStartManual.setBackgroundColor(requireContext().getColor(R.color.purple_primary))

        stopTimer()

        val elapsedTimeMillis = endTimeMillis - startTimeMillis
        val totalSeconds = (elapsedTimeMillis / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        val durationString = String.format("%02d:%02d", hours, minutes)
        etSleepDuration.setText(durationString)

        etSleepDuration.isEnabled = true
        spinnerSleepQuality.isEnabled = true
        btnAddManual.isEnabled = true

        Toast.makeText(requireContext(), "Suivi terminé.", Toast.LENGTH_SHORT).show()
    }

    private fun startTimer() {
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsedMillis = System.currentTimeMillis() - startTimeMillis
                val totalSeconds = (elapsedMillis / 1000).toInt()
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val secs = totalSeconds % 60
                tvCurrentTime.text = String.format("Temps écoulé: %02d:%02d:%02d", hours, minutes, secs)
            }
            override fun onFinish() { }
        }
        timer.start()
    }

    private fun stopTimer() {
        if (::timer.isInitialized) {
            timer.cancel()
        }
        updateCurrentTime()
    }

    private fun addManualSleepEntry() {
        val duration = etSleepDuration.text.toString()
        val quality = spinnerSleepQuality.selectedItem.toString()

        if (duration.isEmpty()) {
            etSleepDuration.error = "Requis"
            return
        }
        if (!isValidDurationFormat(duration)) {
            etSleepDuration.error = "Format HH:MM invalide"
            return
        }

        saveSleepData(duration, quality)
    }

    private fun isValidDurationFormat(duration: String): Boolean {
        return duration.matches(Regex("^\\d{1,2}:\\d{2}$"))
    }

    private fun saveSleepData(durationStr: String, qualityStr: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Erreur: Non connecté", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Save to SharedPreferences (for fast "Last Night" access)
        val sharedPref = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("last_sleep_date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            .putString("last_sleep_duration", durationStr)
            .putString("last_sleep_quality", qualityStr)
            .apply()

        // 2. Prepare data for Firestore
        // Convert HH:MM to Float hours (e.g., "07:30" -> 7.5)
        val parts = durationStr.split(":")
        val hours = parts[0].toFloat()
        val minutes = parts[1].toFloat()
        val durationFloat = hours + (minutes / 60f)

        // Map quality text to Score (0-100)
        val qualityScore = when(qualityStr) {
            "Excellente" -> 100
            "Bonne" -> 80
            "Moyenne" -> 60
            "Mauvaise" -> 40
            else -> 20
        }

        val sleepSession = hashMapOf(
            "date" to System.currentTimeMillis(),
            "durationString" to durationStr,
            "durationHours" to durationFloat,
            "qualityText" to qualityStr,
            "qualityScore" to qualityScore
        )

        // 3. Save to Firestore
        firestore.collection("users").document(user.uid)
            .collection("sleep_sessions")
            .add(sleepSession)
            .addOnSuccessListener {
                Toast.makeText(context, "Session enregistrée !", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erreur de sauvegarde: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCurrentTime() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        tvCurrentTime.text = "Heure actuelle: $currentTime"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTracking && ::timer.isInitialized) {
            timer.cancel()
        }
    }
}