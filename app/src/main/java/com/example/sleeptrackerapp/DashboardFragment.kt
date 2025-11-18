package com.example.sleeptrackerapp


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sleeptrackerapp.R
import com.example.sleeptrackerapp.SleepTrackingDialogFragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashboardFragment : AppCompatActivity() {

    private lateinit var sleepChart: BarChart
    private lateinit var fabAddSleep: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_dashboard)

        initializeViews()
        setupSleepChart()
        setupClickListeners()
        updateSleepData()
    }

    private fun initializeViews() {
        sleepChart = findViewById(R.id.sleep_chart)
        fabAddSleep = findViewById(R.id.fab_add_sleep)
    }

    private fun setupSleepChart() {
        // Configuration du graphique
        sleepChart.setDrawBarShadow(false)
        sleepChart.setDrawValueAboveBar(true)
        sleepChart.description.isEnabled = false
        sleepChart.setMaxVisibleValueCount(7)
        sleepChart.setPinchZoom(false)
        sleepChart.setDrawGridBackground(false)

        // Configuration de l'axe X
        val xAxis = sleepChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelCount = 7
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val days = arrayOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
                return days.getOrNull(value.toInt()) ?: ""
            }
        }
        xAxis.textColor = 0xFFFFFFFF.toInt()

        // Configuration de l'axe Y gauche
        val leftAxis = sleepChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        leftAxis.textColor = 0xFFFFFFFF.toInt()
        leftAxis.gridColor = 0x5A5A5A6B.toInt()

        // Masquer l'axe Y droit
        sleepChart.axisRight.isEnabled = false
        sleepChart.legend.isEnabled = false
    }

    private fun updateSleepData() {
        // Données d'exemple pour la semaine
        val sleepEntries = ArrayList<BarEntry>()
        sleepEntries.add(BarEntry(0f, 7.25f))  // Lundi: 7h15
        sleepEntries.add(BarEntry(1f, 6.75f))  // Mardi: 6h45
        sleepEntries.add(BarEntry(2f, 8.0f))   // Mercredi: 8h00
        sleepEntries.add(BarEntry(3f, 7.5f))   // Jeudi: 7h30
        sleepEntries.add(BarEntry(4f, 6.5f))   // Vendredi: 6h30
        sleepEntries.add(BarEntry(5f, 8.5f))   // Samedi: 8h30
        sleepEntries.add(BarEntry(6f, 7.25f))  // Dimanche: 7h15

        val dataSet = BarDataSet(sleepEntries, "Durée de sommeil")
        dataSet.color = 0xFFB4B0FF.toInt()
        dataSet.valueTextColor = 0xFFFFFFFF.toInt()
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val hours = value.toInt()
                val minutes = ((value - hours) * 60).toInt()
                return "${hours}h${minutes}m"
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f

        sleepChart.data = barData
        sleepChart.invalidate()
    }

    private fun setupClickListeners() {
        fabAddSleep.setOnClickListener {
            showAddSleepDialog()
        }
    }

    private fun showAddSleepDialog() {
        // Implémentation de la boîte de dialogue pour ajouter une session de sommeil
        val dialog = SleepTrackingDialogFragment()
        dialog.show(supportFragmentManager, "SleepTrackingDialog")
    }
}

