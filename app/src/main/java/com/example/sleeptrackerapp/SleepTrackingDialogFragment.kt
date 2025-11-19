package com.example.sleeptrackerapp

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
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
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
    private lateinit var tvSensorStatus: TextView // Nouveau TextView pour le statut
    private lateinit var btnClose: ImageButton

    private lateinit var timer: CountDownTimer
    private var isTracking = false
    private var startTimeMillis: Long = 0

    // Capteurs
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

        // Initialisation Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialisation Capteurs
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

        // Garder l'écran allumé pendant le suivi si nécessaire (optionnel)
        // dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initializeViews(view: View) {
        etSleepDuration = view.findViewById(R.id.et_sleep_duration)
        spinnerSleepQuality = view.findViewById(R.id.spinner_sleep_quality)
        btnStartManual = view.findViewById(R.id.btn_start_manual)
        btnAddManual = view.findViewById(R.id.btn_add_manual)
        tvCurrentTime = view.findViewById(R.id.tv_current_time)
        btnClose = view.findViewById(R.id.btn_close)

        // On ajoute dynamiquement un statut pour les capteurs si non présent dans le XML
        // (Idéalement, ajoutez un TextView dans le XML, ici on utilise tvCurrentTime pour afficher l'info si besoin)
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
                stopSensorTracking()
                isTracking = false
            }
            dismiss()
        }
    }

    // --- LOGIQUE DE SUIVI AUTOMATIQUE (CAPTEURS) ---

    private fun startSleepTracking() {
        isTracking = true
        movementCount = 0 // Réinitialiser les mouvements

        btnStartManual.text = "Arrêter le suivi"
        btnStartManual.setBackgroundColor(requireContext().getColor(android.R.color.holo_red_dark))

        // Désactiver les champs manuels
        etSleepDuration.isEnabled = false
        spinnerSleepQuality.isEnabled = false
        btnAddManual.isEnabled = false
        etSleepDuration.setText("Suivi en cours...")

        // Démarrer Timer et Capteurs
        startTimeMillis = System.currentTimeMillis()
        startTimer()
        startSensorTracking()

        Toast.makeText(requireContext(), "Suivi auto démarré. Posez le téléphone.", Toast.LENGTH_SHORT).show()
    }

    private fun stopSleepTracking() {
        val endTimeMillis = System.currentTimeMillis()
        isTracking = false

        btnStartManual.text = "Démarrer le suivi"
        btnStartManual.setBackgroundColor(requireContext().getColor(R.color.purple_primary))

        stopTimer()
        stopSensorTracking()

        // Calcul de la durée
        val elapsedTimeMillis = endTimeMillis - startTimeMillis
        val totalSeconds = (elapsedTimeMillis / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val durationString = String.format("%02d:%02d", hours, minutes)

        etSleepDuration.setText(durationString)
        etSleepDuration.isEnabled = true
        btnAddManual.isEnabled = true
        spinnerSleepQuality.isEnabled = true

        // --- ANALYSE AUTOMATIQUE DE la QUALITÉ ---
        // Logique simple : Plus il y a de mouvements par heure, moins bonne est la qualité
        val movementsPerHour = if (hours > 0) movementCount / hours else movementCount

        val autoQualityIndex = when {
            movementsPerHour < 5 -> 0 // Excellente (peu de mouvements)
            movementsPerHour < 15 -> 1 // Bonne
            movementsPerHour < 30 -> 2 // Moyenne
            movementsPerHour < 50 -> 3 // Mauvaise
            else -> 4 // Très mauvaise
        }

        spinnerSleepQuality.setSelection(autoQualityIndex)
        Toast.makeText(requireContext(), "Qualité détectée : ${spinnerSleepQuality.selectedItem} ($movementCount mvts)", Toast.LENGTH_LONG).show()
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

            // Calcul de l'accélération (gravité incluse ~9.8)
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

            // Seuil de détection de mouvement (1.0 = statique, > 1.1 = mouvement)
            if (gForce > 1.1f) {
                val now = System.currentTimeMillis()
                // Debounce: compter un mouvement max toutes les 500ms
                if (now - lastMovementTime > 500) {
                    movementCount++
                    lastMovementTime = now
                    // Optionnel : Mettre à jour l'UI pour montrer que le capteur fonctionne
                    // tvCurrentTime.text = "Mouvements détectés: $movementCount"
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non utilisé
    }

    // --- FIN LOGIQUE CAPTEURS ---

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

        // Sauvegarde locale (SharedPreferences)
        val sharedPref = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("last_sleep_date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            .putString("last_sleep_duration", durationStr)
            .putString("last_sleep_quality", qualityStr)
            .apply()

        // Conversion de la durée
        val parts = durationStr.split(":")
        val hours = parts[0].toFloat()
        val minutes = parts[1].toFloat()
        val durationFloat = hours + (minutes / 60f)

        // Score de qualité
        val qualityScore = when(qualityStr) {
            "Excellente" -> 100
            "Bonne" -> 80
            "Moyenne" -> 60
            "Mauvaise" -> 40
            else -> 20
        }

        // Ajout d'un champ "source" pour savoir si c'était auto ou manuel
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
                Toast.makeText(context, "Session enregistrée ($source) !", Toast.LENGTH_SHORT).show()
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
        if (isTracking) {
            stopTimer()
            stopSensorTracking()
        }
    }
}