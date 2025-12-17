package com.aymenzemrani.sleeptrackerapp

import android.app.Dialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlin.math.sqrt

class SleepTrackingDialogFragment : DialogFragment(), SensorEventListener {

    private lateinit var etSleepDuration: TextInputEditText
    private lateinit var spinnerSleepQuality: Spinner
    private lateinit var btnStartManual: MaterialButton
    private lateinit var btnAddManual: MaterialButton
    private lateinit var tvCurrentTime: TextView
    private lateinit var btnClose: ImageButton

    private lateinit var timer: CountDownTimer
    private var isTracking = false
    private var startTimeMillis: Long = 0

    // New properties for post-tracking selection
    private var tempDurationString: String = ""
    private var isWaitingForQualitySelection = false

    // Sensors
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var movementCount = 0
    private var lastMovementTime: Long = 0

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_sleep_tracking, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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
        val qualityOptions = arrayOf(
            "Sélectionnez la qualité",
            "Excellente",
            "Bonne",
            "Moyenne",
            "Mauvaise",
            "Très mauvaise"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, qualityOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSleepQuality.adapter = adapter

        spinnerSleepQuality.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (isWaitingForQualitySelection && position != 0) {
                    val selectedQuality = qualityOptions[position]
                    saveSleepData(tempDurationString, selectedQuality)
                    isWaitingForQualitySelection = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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
                stopSensorTracking()
                isTracking = false
            }
            dismiss()
        }
    }

    // --- AUTOMATIC TRACKING LOGIC ---

    private fun startSleepTracking() {
        isTracking = true
        isWaitingForQualitySelection = false // Reset state
        movementCount = 0

        btnStartManual.text = "Arrêter le suivi"
        btnStartManual.setBackgroundColor(requireContext().getColor(android.R.color.holo_red_dark))

        // Clear the field and disable input while tracking
        etSleepDuration.setText("00:00:00")
        etSleepDuration.isEnabled = false
        // Removed: etSleepDuration.hint = "Suivi en cours..."

        spinnerSleepQuality.isEnabled = false
        btnAddManual.isEnabled = false

        startTimeMillis = System.currentTimeMillis()
        startTimer()
        startSensorTracking()

        Toast.makeText(requireContext(), "Suivi auto démarré.", Toast.LENGTH_SHORT).show()
    }

    private fun stopSleepTracking() {
        val endTimeMillis = System.currentTimeMillis()
        isTracking = false

        btnStartManual.text = "Démarrer le suivi"
        btnStartManual.setBackgroundColor(requireContext().getColor(R.color.purple_primary))

        stopTimer()
        stopSensorTracking()

        // --- Calculate Duration ---
        val elapsedTimeMillis = endTimeMillis - startTimeMillis
        val totalSeconds = (elapsedTimeMillis / 1000).toInt()

        var hours = totalSeconds / 3600
        var minutes = (totalSeconds % 3600) / 60

        // FORCE 1 MINUTE if test is short (so it doesn't show 00:00)
        if (totalSeconds > 0 && hours == 0 && minutes == 0) {
            minutes = 1
        }

        val durationString = String.format("%02d:%02d", hours, minutes)
        tempDurationString = durationString
        etSleepDuration.setText(durationString)

        // Enable spinner and wait for selection
        spinnerSleepQuality.isEnabled = true
        spinnerSleepQuality.setSelection(0) // Select "Sélectionnez la qualité"
        isWaitingForQualitySelection = true

        Toast.makeText(
            requireContext(),
            "Veuillez sélectionner la qualité pour enregistrer.",
            Toast.LENGTH_LONG
        ).show()

        // Disable Start/Add buttons while waiting
        btnAddManual.isEnabled = false
        btnStartManual.isEnabled = false
    }

    private fun startSensorTracking() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopSensorTracking() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

            if (gForce > 1.1f) {
                val now = System.currentTimeMillis()
                if (now - lastMovementTime > 500) {
                    movementCount++
                    lastMovementTime = now
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    // --- TIMER ---

    private fun startTimer() {
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsedMillis = System.currentTimeMillis() - startTimeMillis
                val totalSeconds = (elapsedMillis / 1000).toInt()
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val secs = totalSeconds % 60

                val timeString = String.format("%02d:%02d:%02d", hours, minutes, secs)
                tvCurrentTime.text = "Temps écoulé: $timeString"
                etSleepDuration.setText(timeString)
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

    // --- MANUAL ENTRY ---

    private fun addManualSleepEntry() {
        val duration = etSleepDuration.text.toString()
        val qualityPosition = spinnerSleepQuality.selectedItemPosition
        val quality = spinnerSleepQuality.selectedItem.toString()

        if (qualityPosition == 0) {
            Toast.makeText(context, "Veuillez sélectionner une qualité valide", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (duration.isEmpty()) {
            etSleepDuration.error = "Requis"
            return
        }
        if (!isValidDurationFormat(duration)) {
            etSleepDuration.error = "Format HH:MM invalide"
            return
        }

        // Disable buttons immediately to prevent double-click or "not working" feeling
        btnAddManual.isEnabled = false
        btnAddManual.text = "..."
        btnStartManual.isEnabled = false

        saveSleepData(duration, quality)
    }

    private fun isValidDurationFormat(duration: String): Boolean {
        return duration.matches(Regex("^\\d{1,2}:\\d{2}$"))
    }

    private fun saveSleepData(durationStr: String, qualityStr: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Erreur: Non connecté", Toast.LENGTH_SHORT).show()
            resetButtons()
            return
        }

        val sharedPref = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("last_sleep_date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            .putString("last_sleep_duration", durationStr)
            .putString("last_sleep_quality", qualityStr)
            .apply()

        val parts = durationStr.split(":")
        val hours = parts[0].toFloat()
        val minutes = parts[1].toFloat()
        val durationFloat = hours + (minutes / 60f)

        val qualityScore = when(qualityStr) {
            "Excellente" -> 100
            "Bonne" -> 80
            "Moyenne" -> 60
            "Mauvaise" -> 40
            "Très mauvaise" -> 20
            else -> 0 // Fallback
        }

        val source = if (movementCount > 0) "Capteurs" else "Manuel"

        val sleepSession = hashMapOf(
            "date" to System.currentTimeMillis(),
            "durationString" to durationStr,
            "durationHours" to durationFloat,
            "qualityText" to qualityStr,
            "qualityScore" to qualityScore,
            "source" to source,
            "movements" to movementCount
        )

        firestore.collection("users").document(user.uid)
            .collection("sleep_sessions")
            .add(sleepSession)
            .addOnSuccessListener {
                if (context != null) {
                    Toast.makeText(context, "Session enregistrée !", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
            .addOnFailureListener { e ->
                if (context != null) {
                    Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    resetButtons()
                }
            }
    }

    private fun resetButtons() {
        if (context != null) {
            btnAddManual.isEnabled = true
            btnAddManual.text = getString(R.string.btn_add_manual)
            btnStartManual.isEnabled = true
        }
    }

    private fun updateCurrentTime() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        tvCurrentTime.text = "Heure actuelle: $currentTime"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTracking) {
            stopTimer()
            stopSensorTracking()
        }
    }
}