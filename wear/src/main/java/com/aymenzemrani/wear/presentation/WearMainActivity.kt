package com.aymenzemrani.wear.presentation // Make sure this matches your folder structure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat // Added for correct color handling
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlin.math.sqrt
import com.aymenzemrani.wear.R

class WearMainActivity : FragmentActivity(), SensorEventListener {

    private lateinit var tvStatus: TextView
    private lateinit var btnAction: Button
    private lateinit var chronometer: Chronometer

    private var isTracking = false
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var movementCount = 0
    private var lastMovementTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wear_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnAction = findViewById(R.id.btnAction)
        chronometer = findViewById(R.id.tvTimer)

        // Use Context.SENSOR_SERVICE for clarity
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        btnAction.setOnClickListener {
            if (isTracking) stopTracking() else startTracking()
        }
    }

    private fun startTracking() {
        isTracking = true
        movementCount = 0
        tvStatus.text = "Suivi..."
        btnAction.text = "Arrêter"
        // FIXED: Use ContextCompat for colors
        btnAction.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopTracking() {
        isTracking = false
        chronometer.stop()
        sensorManager.unregisterListener(this)

        tvStatus.text = "Terminé"
        btnAction.text = "Démarrer"
        // Reset button color to your primary color (purple)
        btnAction.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_primary))

        // Send data to Phone
        val durationSeconds = (SystemClock.elapsedRealtime() - chronometer.base) / 1000
        sendDataToPhone(durationSeconds, movementCount)
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun sendDataToPhone(durationSec: Long, movements: Int) {
        val dataMapRequest = PutDataMapRequest.create("/sleep_data")
        dataMapRequest.dataMap.putLong("timestamp", System.currentTimeMillis())
        dataMapRequest.dataMap.putLong("duration", durationSec)
        dataMapRequest.dataMap.putInt("movements", movements)

        val putDataRequest = dataMapRequest.asPutDataRequest()
        putDataRequest.setUrgent()

        Wearable.getDataClient(this).putDataItem(putDataRequest).addOnSuccessListener {
            Toast.makeText(this, "Envoyé au téléphone!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Erreur d'envoi", Toast.LENGTH_SHORT).show()
        }
    }
}