package com.example.sleeptrackerapp


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import com.example.sleeptrackerapp.R
import com.example.sleeptrackerapp.SleepTrackingDialogFragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton

// CORRECTED: Changed base class from AppCompatActivity to Fragment
class DashboardFragment : Fragment() {

    private lateinit var sleepChart: BarChart
    private lateinit var fabAddSleep: FloatingActionButton
    private lateinit var tvSleepDuration: TextView
    private lateinit var tvSleepQuality: TextView

    companion object {
        fun newInstance() = DashboardFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupSleepChart()
        setupClickListeners()
        // Data is now updated in onResume
    }

    // NEW: Refresh data every time the fragment becomes visible (e.g., after dismissing the dialog)
    override fun onResume() {
        super.onResume()
        updateSleepData()
    }

    private fun initializeViews(view: View) {
        sleepChart = view.findViewById(R.id.sleep_chart)
        fabAddSleep = view.findViewById(R.id.fab_add_sleep)
        tvSleepDuration = view.findViewById(R.id.tv_sleep_duration)
        tvSleepQuality = view.findViewById(R.id.tv_sleep_quality)
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
        xAxis.textColor = ContextCompat.getColor(requireContext(), android.R.color.white)

        // Configuration de l'axe Y gauche
        val leftAxis = sleepChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        leftAxis.textColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        leftAxis.gridColor = 0x5A5A5A6B.toInt()

        // Masquer l'axe Y droit
        sleepChart.axisRight.isEnabled = false
        sleepChart.legend.isEnabled = false
    }

    // MODIFIED: Reads data from SharedPreferences and updates UI elements
    private fun updateSleepData() {
        val sharedPref = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        // Retrieve the last saved sleep data
        val lastDuration = sharedPref.getString("last_sleep_duration", "7:15") // Default for consistency
        val lastQuality = sharedPref.getString("last_sleep_quality", "Bonne")

        // 1. Update Summary Card (Résumé de la nuit dernière)
        tvSleepDuration.text = formatDurationForDisplay(lastDuration)
        tvSleepQuality.text = lastQuality

        // 2. Update Chart (Graphique Hebdomadaire)
        val sleepEntries = ArrayList<BarEntry>()
        // Existing mock data for the week (Lundi to Samedi)
        sleepEntries.add(BarEntry(0f, 7.25f))  // Lundi: 7h15
        sleepEntries.add(BarEntry(1f, 6.75f))  // Mardi: 6h45
        sleepEntries.add(BarEntry(2f, 8.0f))   // Mercredi: 8h00
        sleepEntries.add(BarEntry(3f, 7.5f))   // Jeudi: 7h30
        sleepEntries.add(BarEntry(4f, 6.5f))   // Vendredi: 6h30
        sleepEntries.add(BarEntry(5f, 8.5f))   // Samedi: 8h30

        // Convert stored duration (HH:MM) to float (H.XX) for the chart
        val (hours, minutes) = try {
            val parts = lastDuration?.split(":")
            if (parts?.size == 2) {
                Pair(parts[0].toFloat(), parts[1].toFloat())
            } else {
                Pair(7f, 15f) // Fallback if invalid format
            }
        } catch (e: Exception) {
            Pair(7f, 15f) // Fallback on parsing error
        }

        val lastDurationFloat = hours + (minutes / 60f)

        // Replace the last entry (Dimanche) with the latest saved data
        if (sleepEntries.size >= 7) {
            sleepEntries[6] = BarEntry(6f, lastDurationFloat)
        } else {
            // In case the list had less than 7 items, append the new one as the last entry
            sleepEntries.add(BarEntry(sleepEntries.size.toFloat(), lastDurationFloat))
        }

        val dataSet = BarDataSet(sleepEntries, "Durée de sommeil")
        dataSet.color = 0xFFB4B0FF.toInt()
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val h = value.toInt()
                val m = ((value - h) * 60).toInt()
                return "${h}h${m}m"
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f

        sleepChart.data = barData
        sleepChart.invalidate()
    }

    private fun formatDurationForDisplay(duration: String?): String {
        if (duration == null || !duration.matches(Regex("^\\d{1,2}:\\d{2}$"))) {
            return "7h 15m"
        }
        val parts = duration.split(":")
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        return "${hours}h ${minutes}m"
    }

    private fun setupClickListeners() {
        fabAddSleep.setOnClickListener {
            showAddSleepDialog()
        }
    }

    private fun showAddSleepDialog() {
        // Use childFragmentManager to show the dialog
        val dialog = SleepTrackingDialogFragment()
        dialog.show(childFragmentManager, "SleepTrackingDialog")
    }
}