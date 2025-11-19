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
    // Initialisation du bouton X
    private lateinit var btnClose: ImageButton

    private lateinit var timer: CountDownTimer
    private var isTracking = false
    private var startTimeMillis: Long = 0

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
        // Correction: Initialisation du bouton de fermeture
        btnClose = view.findViewById(R.id.btn_close)

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

        // Correction: Utilisation de la variable initialisée pour le clic sur le bouton X
        btnClose.setOnClickListener {
            if (isTracking) {
                // Arrête le timer pour un nettoyage propre
                stopTimer()
                isTracking = false
            }
            dismiss() // Ferme le dialogue
        }
    }

    private fun startSleepTracking() {
        isTracking = true
        btnStartManual.text = "Arrêter le suivi"
        btnStartManual.setBackgroundColor(requireContext().getColor(android.R.color.holo_red_dark))

        startTimeMillis = System.currentTimeMillis()
        startTimer()

        // Désactiver les autres contrôles pendant le tracking
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

        // CALCUL DE LA DURÉE ÉCOULÉE
        val elapsedTimeMillis = endTimeMillis - startTimeMillis
        val totalSeconds = (elapsedTimeMillis / 1000).toInt()

        // Conversion en format HH:MM
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val durationString = String.format("%02d:%02d", hours, minutes)

        // Afficher la durée calculée dans le champ d'entrée
        etSleepDuration.setText(durationString)

        // Réactiver les autres contrôles
        etSleepDuration.isEnabled = true
        spinnerSleepQuality.isEnabled = true
        btnAddManual.isEnabled = true

        Toast.makeText(requireContext(), "Suivi du sommeil arrêté", Toast.LENGTH_SHORT).show()
    }

    private fun startTimer() {
        // Le timer calcule le temps écoulé basé sur startTimeMillis
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsedMillis = System.currentTimeMillis() - startTimeMillis
                val totalSeconds = (elapsedMillis / 1000).toInt()

                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val secs = totalSeconds % 60

                tvCurrentTime.text = String.format("Temps écoulé: %02d:%02d:%02d", hours, minutes, secs)
            }

            override fun onFinish() {
                // Ne devrait pas être appelé
            }
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