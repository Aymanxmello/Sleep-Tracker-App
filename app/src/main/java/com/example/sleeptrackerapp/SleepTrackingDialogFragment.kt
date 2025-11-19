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
import java.text.SimpleDateFormat
import java.util.*

class SleepTrackingDialogFragment : DialogFragment() {

    private lateinit var etSleepDuration: TextInputEditText
    private lateinit var spinnerSleepQuality: Spinner
    private lateinit var btnStartManual: MaterialButton
    private lateinit var btnAddManual: MaterialButton
    private lateinit var tvCurrentTime: TextView
    private lateinit var timer: CountDownTimer
    private var isTracking = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_sleep_tracking, container, false)
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

        // Afficher l'heure actuelle
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

        // Fermer le dialog
        view?.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener {
            dismiss()
        }
    }

    private fun startSleepTracking() {
        isTracking = true
        btnStartManual.text = "Arrêter le suivi"
        btnStartManual.setBackgroundColor(requireContext().getColor(android.R.color.holo_red_dark))

        // Démarrer le timer
        startTimer()

        // Désactiver les autres contrôles pendant le tracking
        etSleepDuration.isEnabled = false
        spinnerSleepQuality.isEnabled = false
        btnAddManual.isEnabled = false

        Toast.makeText(requireContext(), "Suivi du sommeil démarré", Toast.LENGTH_SHORT).show()
    }

    private fun stopSleepTracking() {
        isTracking = false
        btnStartManual.text = "Démarrer le suivi"
        // Use R.color.purple_primary from resources
        btnStartManual.setBackgroundColor(requireContext().getColor(com.example.sleeptrackerapp.R.color.purple_primary))

        // Arrêter le timer
        stopTimer()

        // Réactiver les autres contrôles
        etSleepDuration.isEnabled = true
        spinnerSleepQuality.isEnabled = true
        btnAddManual.isEnabled = true

        // For simplicity, keeping the mock duration of 8:00 on stop,
        // as the timer logic does not currently expose the final duration.
        etSleepDuration.setText("8:00")

        Toast.makeText(requireContext(), "Suivi du sommeil arrêté", Toast.LENGTH_SHORT).show()
    }

    private fun startTimer() {
        var seconds = 0
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                seconds++
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                tvCurrentTime.text = String.format("Temps écoulé: %02d:%02d:%02d", hours, minutes, secs)
            }

            override fun onFinish() {}
        }
        timer.start()
    }

    private fun stopTimer() {
        timer.cancel()
        tvCurrentTime.text = "Prêt pour le suivi"
    }

    private fun addManualSleepEntry() {
        val duration = etSleepDuration.text.toString()
        val quality = spinnerSleepQuality.selectedItem.toString()

        if (duration.isEmpty()) {
            etSleepDuration.error = "Veuillez entrer la durée du sommeil"
            return
        }

        // Valider le format de durée (HH:MM)
        if (!isValidDurationFormat(duration)) {
            etSleepDuration.error = "Format invalide. Utilisez HH:MM"
            return
        }

        // Sauvegarder les données
        saveSleepData(duration, quality)

        Toast.makeText(requireContext(), "Session de sommeil ajoutée", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun isValidDurationFormat(duration: String): Boolean {
        return duration.matches(Regex("^\\d{1,2}:\\d{2}$"))
    }

    private fun saveSleepData(duration: String, quality: String) {
        // Sauvegarder dans SharedPreferences
        val sharedPref = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        editor.putString("last_sleep_date", date)
        editor.putString("last_sleep_duration", duration)
        editor.putString("last_sleep_quality", quality)
        editor.apply()

        // Removed the incorrect call to (activity as? DashboardActivity)?.updateSleepData()
        // The DashboardFragment's onResume() method will now handle the data refresh.
    }

    private fun updateCurrentTime() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        tvCurrentTime.text = "Heure actuelle: $currentTime"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTracking) {
            timer.cancel()
        }
    }
}