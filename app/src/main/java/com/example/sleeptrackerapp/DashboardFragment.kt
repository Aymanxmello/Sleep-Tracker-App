package com.example.sleeptrackerapp


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sleeptrackerapp.R
import com.example.sleeptrackerapp.SleepTrackingDialogFragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton
// NOUVEAU: Imports pour Coroutines et WeatherService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Helper function to convert Unix timestamp (seconds) to HH:mm string
private fun convertUnixToTime(timestamp: Long): String {
    val date = java.util.Date(timestamp * 1000) // Convert seconds to milliseconds
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(date)
}


// Structure de données pour les jours du RecyclerView
data class DayItem(
    val dayOfMonth: String,
    val dayName: String,
    val date: Calendar,
    var isSelected: Boolean = false
)

// Adaptateur pour afficher les jours
class DayAdapter(
    private val days: List<DayItem>,
    private val onDaySelected: (DayItem) -> Unit
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    private var selectedPosition = days.indexOfFirst { it.isSelected }

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardDay)
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectNewDay(position)
                }
            }
        }

        fun bind(day: DayItem) {
            tvDay.text = day.dayOfMonth
            tvDayName.text = day.dayName

            val context = itemView.context
            if (day.isSelected) {
                // Couleur sélectionnée (e.g., bleu clair)
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.cyan_accent))
                tvDay.setTextColor(ContextCompat.getColor(context, R.color.dark_blue_primary))
                tvDayName.setTextColor(ContextCompat.getColor(context, R.color.dark_blue_primary))
            } else {
                // Couleur désélectionnée (e.g., foncé)
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.dark_blue_tertiary))
                tvDay.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                tvDayName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }

    private fun selectNewDay(position: Int) {
        if (selectedPosition != position) {
            // Désélectionner l'ancien
            if (selectedPosition != RecyclerView.NO_POSITION) {
                days[selectedPosition].isSelected = false
                notifyItemChanged(selectedPosition)
            }
            // Sélectionner le nouveau
            selectedPosition = position
            days[selectedPosition].isSelected = true
            notifyItemChanged(selectedPosition)
            onDaySelected(days[position])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size
}


class DashboardFragment : Fragment() {

    private lateinit var sleepChart: BarChart
    private lateinit var fabAddSleep: FloatingActionButton
    private lateinit var tvSleepDuration: TextView
    private lateinit var tvSleepQuality: TextView
    private lateinit var rvDays: RecyclerView
    private lateinit var tvSelectedDate: TextView
    // Champs pour les données météo
    private lateinit var tvTemperature: TextView
    private lateinit var tvSunriseTime: TextView
    private lateinit var tvSunsetTime: TextView


    companion object {
        fun newInstance() = DashboardFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupSleepChart()
        setupClickListeners()
        setupDaySelector()
    }

    override fun onResume() {
        super.onResume()
        updateSleepData()
    }

    private fun initializeViews(view: View) {
        sleepChart = view.findViewById(R.id.sleep_chart)
        fabAddSleep = view.findViewById(R.id.fab_add_sleep)
        tvSleepDuration = view.findViewById(R.id.tv_sleep_duration)
        tvSleepQuality = view.findViewById(R.id.tv_sleep_quality)

        // Initialisation des vues de date/météo
        rvDays = view.findViewById(R.id.rv_days)
        tvSelectedDate = view.findViewById(R.id.tv_selected_date)
        tvTemperature = view.findViewById(R.id.tv_temperature)
        tvSunriseTime = view.findViewById(R.id.tv_sunrise_time)
        tvSunsetTime = view.findViewById(R.id.tv_sunset_time)
    }

    private fun setupDaySelector() {
        val daysList = generateLastWeekDays()

        if (daysList.isNotEmpty()) {
            daysList.last().isSelected = true
            updateDayDisplay(daysList.last())
        }

        rvDays.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvDays.adapter = DayAdapter(daysList) { selectedDay ->
            updateDayDisplay(selectedDay)
            updateSleepData()
        }
        rvDays.scrollToPosition(daysList.size - 1)
    }

    // MODIFIÉ: Implémente la logique d'appel API asynchrone (OpenWeatherMap)
    private fun updateDayDisplay(dayItem: DayItem) {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        tvSelectedDate.text = dateFormat.format(dayItem.date.time)

        // PARAMÈTRES POUR L'API OWM
        val API_KEY = "4cd29e7ea36ffeaef7cad09a75f90f6c" // Clé API insérée
        val LATITUDE = 48.8566 // Latitude de Paris (Exemple)
        val LONGITUDE = 2.3522 // Longitude de Paris

        // Afficher des marqueurs de chargement en attendant l'API
        tvTemperature.text = "..."
        tvSunsetTime.text = "..."
        tvSunriseTime.text = "..."

        // UTILISATION DE COROUTINES POUR L'APPEL API (Asynchrone)
        lifecycleScope.launch {
            try {
                val response = WeatherApiClient.service.fetchWeather(
                    lat = LATITUDE,
                    lon = LONGITUDE,
                    apiKey = API_KEY
                )

                // Convertir la date du jour sélectionné en timestamp Unix (en secondes) pour comparaison
                val selectedDayStartTimestamp = dayItem.date.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis / 1000L

                // Trouver les données météo qui correspondent au jour sélectionné.
                // On utilise une marge de 24h (86400 secondes) pour la comparaison des dates.
                val dailyData = response.daily.find { daily ->
                    val dayDiff = Math.abs(daily.dt - selectedDayStartTimestamp)
                    dayDiff < 86400
                }

                if (dailyData != null) {
                    // Mettre à jour l'UI avec les données réelles
                    tvTemperature.text = String.format("%.1f°C", dailyData.temp.day)
                    tvSunsetTime.text = "🌅 ${convertUnixToTime(dailyData.sunset)}"
                    tvSunriseTime.text = "🌄 ${convertUnixToTime(dailyData.sunrise)}"
                } else {
                    // Les données pour ce jour ne sont pas disponibles (hors fenêtre de prévision de 7 jours)
                    tvTemperature.text = "N/A"
                    tvSunsetTime.text = "🌅 N/A"
                    tvSunriseTime.text = "🌄 N/A"
                }

            } catch (e: Exception) {
                // Gérer les erreurs (ex: clé API invalide, connexion échouée)
                tvTemperature.text = "Erreur"
                tvSunsetTime.text = "🌅 Erreur"
                tvSunriseTime.text = "🌄 Erreur"
            }
        }
    }

    private fun generateLastWeekDays(): List<DayItem> {
        val days = mutableListOf<DayItem>()
        val calendar = Calendar.getInstance()
        val dayOfMonthFormat = SimpleDateFormat("dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

        // Remonte 6 jours avant aujourd'hui
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0 until 7) {
            val dateCopy = calendar.clone() as Calendar
            days.add(
                DayItem(
                    dayOfMonth = dayOfMonthFormat.format(dateCopy.time),
                    dayName = dayNameFormat.format(dateCopy.time),
                    date = dateCopy,
                    isSelected = (i == 6)
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return days
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

    private fun updateSleepData() {
        val sharedPref = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        val lastDuration = sharedPref.getString("last_sleep_duration", "7:15")
        val lastQuality = sharedPref.getString("last_sleep_quality", "Bonne")

        // 1. Update Summary Card (Résumé de la nuit dernière)
        tvSleepDuration.text = formatDurationForDisplay(lastDuration)
        tvSleepQuality.text = lastQuality

        // 2. Update Chart (Graphique Hebdomadaire)
        val sleepEntries = ArrayList<BarEntry>()
        sleepEntries.add(BarEntry(0f, 7.25f))
        sleepEntries.add(BarEntry(1f, 6.75f))
        sleepEntries.add(BarEntry(2f, 8.0f))
        sleepEntries.add(BarEntry(3f, 7.5f))
        sleepEntries.add(BarEntry(4f, 6.5f))
        sleepEntries.add(BarEntry(5f, 8.5f))

        val (hours, minutes) = try {
            val parts = lastDuration?.split(":")
            if (parts?.size == 2) {
                Pair(parts[0].toFloat(), parts[1].toFloat())
            } else {
                Pair(7f, 15f)
            }
        } catch (e: Exception) {
            Pair(7f, 15f)
        }

        val lastDurationFloat = hours + (minutes / 60f)

        if (sleepEntries.size >= 7) {
            sleepEntries[6] = BarEntry(6f, lastDurationFloat)
        } else {
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
        val dialog = SleepTrackingDialogFragment()
        dialog.show(childFragmentManager, "SleepTrackingDialog")
    }
}