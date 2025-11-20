package com.example.sleeptrackerapp

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query as RetrofitQuery
import java.text.SimpleDateFormat
import java.util.*

// --- DATA MODELS ---
data class WeatherResponse(
    val main: Main,
    val sys: Sys,
    val dt: Long,
    val name: String
)

data class Main(
    val temp: Double,
    val humidity: Int
)

data class Sys(
    val sunrise: Long,
    val sunset: Long
)

data class SleepSessionData(
    val date: Long,
    val dayLabel: String,
    val durationHours: Float,
    val qualityScore: Int
)

// --- WEATHER API ---
interface WeatherService {
    @GET("data/2.5/weather?units=metric")
    suspend fun fetchCurrentWeather(
        @RetrofitQuery("lat") lat: Double,
        @RetrofitQuery("lon") lon: Double,
        @RetrofitQuery("appid") apiKey: String
    ): WeatherResponse
}

object WeatherApiClient {
    private const val BASE_URL = "https://api.openweathermap.org/"
    val service: WeatherService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherService::class.java)
    }
}

private fun convertUnixToTime(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(date)
}

// --- RECYCLER VIEW (Days) ---
data class DayItem(
    val dayOfMonth: String,
    val dayName: String,
    val date: Calendar,
    var isSelected: Boolean = false
)

class DayAdapter(
    private val days: List<DayItem>
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardDay)
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)

        init {
            itemView.isClickable = false
            itemView.isFocusable = false
        }

        fun bind(day: DayItem) {
            tvDay.text = day.dayOfMonth
            tvDayName.text = day.dayName
            val context = itemView.context
            if (day.isSelected) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.cyan_accent))
                tvDay.setTextColor(ContextCompat.getColor(context, R.color.dark_blue_primary))
                tvDayName.setTextColor(ContextCompat.getColor(context, R.color.dark_blue_primary))
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.dark_blue_tertiary))
                tvDay.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                tvDayName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
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
    private lateinit var qualityChart: LineChart
    private lateinit var fabAddSleep: FloatingActionButton
    private lateinit var tvSleepDuration: TextView
    private lateinit var tvSleepQuality: TextView
    private lateinit var rvDays: RecyclerView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvCityName: TextView

    // Weather Views
    private lateinit var tvTemperature: TextView
    private lateinit var tvSunriseTime: TextView
    private lateinit var tvSunsetTime: TextView

    // KPI Views
    private lateinit var tvKpiAverage: TextView
    private lateinit var tvKpiBest: TextView
    private lateinit var tvKpiWorst: TextView

    // Firebase & Location
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Default Location (Paris)
    private var currentLat: Double = 48.8566
    private var currentLon: Double = 2.3522

    companion object {
        fun newInstance() = DashboardFragment()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                getUserLocation()
            } else {
                Toast.makeText(context, "Location denied. Using default city.", Toast.LENGTH_SHORT).show()
                setupDaySelector()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        initializeViews(view)
        setupCharts()
        setupClickListeners()

        // 1. START STARS
        setupStarryBackground(view)

        checkLocationPermissions()
    }

    // --- STARRY BACKGROUND FUNCTION ---
    private fun setupStarryBackground(view: View) {
        val container = view.findViewById<FrameLayout>(R.id.star_container)
        if (container == null) return

        container.post {
            val width = container.width
            val height = container.height
            val random = Random()

            // Create 50 stars
            for (i in 0 until 50) {
                val star = View(context)

                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(Color.WHITE)
                star.background = drawable

                // Random Size (2dp to 5dp)
                val density = resources.displayMetrics.density
                val size = ((2..5).random() * density).toInt()
                val params = FrameLayout.LayoutParams(size, size)

                // Random Position
                params.leftMargin = random.nextInt(width)
                params.topMargin = random.nextInt(height)
                star.layoutParams = params

                container.addView(star)

                // Animation
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
    // --------------------------------

    override fun onResume() {
        super.onResume()
        loadDataFromFirestore()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getUserLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLon = location.longitude
                }
                setupDaySelector()
            }
            .addOnFailureListener {
                setupDaySelector()
            }
    }

    private fun initializeViews(view: View) {
        sleepChart = view.findViewById(R.id.sleep_chart)
        qualityChart = view.findViewById(R.id.quality_chart)
        fabAddSleep = view.findViewById(R.id.fab_add_sleep)
        tvSleepDuration = view.findViewById(R.id.tv_sleep_duration)
        tvSleepQuality = view.findViewById(R.id.tv_sleep_quality)

        rvDays = view.findViewById(R.id.rv_days)
        tvSelectedDate = view.findViewById(R.id.tv_selected_date)
        tvCityName = view.findViewById(R.id.tv_city_name)
        tvTemperature = view.findViewById(R.id.tv_temperature)
        tvSunriseTime = view.findViewById(R.id.tv_sunrise_time)
        tvSunsetTime = view.findViewById(R.id.tv_sunset_time)

        tvKpiAverage = view.findViewById(R.id.tv_kpi_average)
        tvKpiBest = view.findViewById(R.id.tv_kpi_best)
        tvKpiWorst = view.findViewById(R.id.tv_kpi_worst)
    }

    private fun setupDaySelector() {
        val daysList = generateDaysAroundToday()

        rvDays.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvDays.adapter = DayAdapter(daysList)

        rvDays.scrollToPosition(0)

        updateDayDisplay(daysList[2])
    }

    private fun generateDaysAroundToday(): List<DayItem> {
        val days = mutableListOf<DayItem>()
        val calendar = Calendar.getInstance()
        val dayOfMonthFormat = SimpleDateFormat("dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

        calendar.add(Calendar.DAY_OF_YEAR, -2)

        for (i in 0 until 5) {
            val dateCopy = calendar.clone() as Calendar
            days.add(
                DayItem(
                    dayOfMonth = dayOfMonthFormat.format(dateCopy.time),
                    dayName = dayNameFormat.format(dateCopy.time),
                    date = dateCopy,
                    isSelected = (i == 2)
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return days
    }

    @SuppressLint("SetTextI18n")
    private fun updateDayDisplay(dayItem: DayItem) {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        tvSelectedDate.text = dateFormat.format(dayItem.date.time)

        // --- WEATHER FETCHING ---
        val API_KEY = "70b11520bda551f50bd6a599271e69bf"

        tvCityName.text = "Chargement..."
        tvTemperature.text = "..."
        tvSunsetTime.text = "..."
        tvSunriseTime.text = "..."

        lifecycleScope.launch {
            try {
                val response = WeatherApiClient.service.fetchCurrentWeather(currentLat, currentLon, API_KEY)

                tvCityName.text = "📍 ${response.name}"
                tvTemperature.text = String.format("%.1f°C", response.main.temp)
                tvSunsetTime.text = "🌅 ${convertUnixToTime(response.sys.sunset)}"
                tvSunriseTime.text = "🌄 ${convertUnixToTime(response.sys.sunrise)}"

            } catch (e: Exception) {
                tvCityName.text = "📍 Inconnu"
                tvTemperature.text = "N/A"
            }
        }
    }

    private fun setupCharts() {
        // Bar Chart
        sleepChart.setDrawBarShadow(false)
        sleepChart.setDrawValueAboveBar(true)
        sleepChart.description.isEnabled = false
        sleepChart.legend.isEnabled = false
        sleepChart.setPinchZoom(false)
        sleepChart.setDrawGridBackground(false)
        sleepChart.axisRight.isEnabled = false

        val xAxis = sleepChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.WHITE
        xAxis.granularity = 1f

        val leftAxis = sleepChart.axisLeft
        leftAxis.textColor = Color.WHITE
        leftAxis.axisMinimum = 0f

        // Line Chart
        qualityChart.description.isEnabled = false
        qualityChart.legend.isEnabled = false
        qualityChart.setPinchZoom(false)
        qualityChart.setDrawGridBackground(false)
        qualityChart.axisRight.isEnabled = false

        val qXAxis = qualityChart.xAxis
        qXAxis.position = XAxis.XAxisPosition.BOTTOM
        qXAxis.setDrawGridLines(false)
        qXAxis.textColor = Color.WHITE
        qXAxis.granularity = 1f

        val qLeftAxis = qualityChart.axisLeft
        qLeftAxis.textColor = Color.WHITE
        qLeftAxis.axisMinimum = 0f
        qLeftAxis.axisMaximum = 100f
    }

    private fun loadDataFromFirestore() {
        val user = auth.currentUser ?: return

        firestore.collection("users").document(user.uid)
            .collection("sleep_sessions")
            .orderBy("date", Query.Direction.ASCENDING)
            .limitToLast(7)
            .get()
            .addOnSuccessListener { documents ->
                val sessionList = mutableListOf<SleepSessionData>()
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

                for (doc in documents) {
                    val dateMillis = doc.getLong("date") ?: 0L
                    val durationHours = doc.getDouble("durationHours")?.toFloat() ?: 0f
                    val qualityScore = doc.getLong("qualityScore")?.toInt() ?: 0

                    val dayLabel = dayFormat.format(Date(dateMillis))
                    sessionList.add(SleepSessionData(dateMillis, dayLabel, durationHours, qualityScore))
                }

                updateDashboardWithRealData(sessionList)
            }

        if (context != null) {
            val sharedPref = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
            val lastDuration = sharedPref.getString("last_sleep_duration", "00:00")
            val lastQuality = sharedPref.getString("last_sleep_quality", "--")

            tvSleepDuration.text = formatDurationForDisplay(lastDuration)
            tvSleepQuality.text = lastQuality
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateDashboardWithRealData(sessions: List<SleepSessionData>) {
        if (sessions.isEmpty() || context == null) return

        // BarChart
        val barEntries = sessions.mapIndexed { index, session ->
            BarEntry(index.toFloat(), session.durationHours)
        }
        val barDataSet = BarDataSet(barEntries, "Durée")
        barDataSet.color = ContextCompat.getColor(requireContext(), R.color.purple_primary)
        barDataSet.valueTextColor = Color.WHITE
        barDataSet.valueTextSize = 12f
        barDataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.1fh", value)
            }
        }

        val barData = BarData(barDataSet)
        barData.barWidth = 0.6f
        sleepChart.data = barData
        sleepChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return sessions.getOrNull(value.toInt())?.dayLabel ?: ""
            }
        }
        sleepChart.invalidate()

        // LineChart
        val lineEntries = sessions.mapIndexed { index, session ->
            Entry(index.toFloat(), session.qualityScore.toFloat())
        }
        val lineDataSet = LineDataSet(lineEntries, "Qualité")
        lineDataSet.color = ContextCompat.getColor(requireContext(), R.color.cyan_accent)
        lineDataSet.lineWidth = 3f
        lineDataSet.circleRadius = 5f
        lineDataSet.setCircleColor(Color.WHITE)
        lineDataSet.valueTextColor = Color.WHITE
        lineDataSet.valueTextSize = 12f
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineDataSet.setDrawFilled(true)
        lineDataSet.fillColor = ContextCompat.getColor(requireContext(), R.color.cyan_accent)
        lineDataSet.fillAlpha = 50

        val lineData = LineData(lineDataSet)
        qualityChart.data = lineData
        qualityChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return sessions.getOrNull(value.toInt())?.dayLabel ?: ""
            }
        }
        qualityChart.invalidate()

        // KPIs
        val avgDuration = sessions.map { it.durationHours }.average()
        val avgH = avgDuration.toInt()
        val avgM = ((avgDuration - avgH) * 60).toInt()
        tvKpiAverage.text = "${avgH}h ${avgM}m"

        val bestSession = sessions.maxByOrNull { it.qualityScore }
        tvKpiBest.text = "${bestSession?.qualityScore ?: 0}%"

        val worstSession = sessions.minByOrNull { it.qualityScore }
        tvKpiWorst.text = "${worstSession?.qualityScore ?: 0}%"
    }

    private fun formatDurationForDisplay(duration: String?): String {
        if (duration == null || !duration.matches(Regex("^\\d{1,2}:\\d{2}$"))) {
            return "--h --m"
        }
        val parts = duration.split(":")
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        return "${hours}h ${minutes}m"
    }

    private fun setupClickListeners() {
        fabAddSleep.setOnClickListener {
            val dialog = SleepTrackingDialogFragment()
            dialog.show(childFragmentManager, "SleepTrackingDialog")
        }
    }
}