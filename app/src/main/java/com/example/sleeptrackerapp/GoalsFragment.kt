package com.example.sleeptrackerapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data model for a Goal
data class GoalModel(
    val id: String = "",
    val duration: Float = 0f,
    val quality: Int = 0,
    val dateCreated: Long = 0,
    val dateString: String = "",
    val isReached: Boolean = false
)

class GoalsFragment : Fragment() {

    private lateinit var etGoalDuration: EditText
    private lateinit var etQualityTarget: EditText
    private lateinit var btnSaveGoal: Button
    private lateinit var rvGoalHistory: RecyclerView
    private lateinit var tvProgressText: TextView
    private lateinit var progressBarGoals: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: GoalHistoryAdapter
    private val goalsList = mutableListOf<GoalModel>()

    companion object {
        fun newInstance() = GoalsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_goals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Bind Views
        etGoalDuration = view.findViewById(R.id.et_goal_duration)
        etQualityTarget = view.findViewById(R.id.et_quality_target)
        btnSaveGoal = view.findViewById(R.id.btn_save_goal)
        rvGoalHistory = view.findViewById(R.id.rv_goal_history)
        tvProgressText = view.findViewById(R.id.tv_progress_text)
        progressBarGoals = view.findViewById(R.id.progress_bar_goals)

        setupRecyclerView()
        setupSaveButton()

        // Load data
        fetchGoalHistory()
        updateTrackingProgress()
    }

    private fun setupRecyclerView() {
        adapter = GoalHistoryAdapter(goalsList)
        rvGoalHistory.layoutManager = LinearLayoutManager(context)
        rvGoalHistory.adapter = adapter
    }

    private fun setupSaveButton() {
        btnSaveGoal.setOnClickListener {
            val durationStr = etGoalDuration.text.toString()
            val qualityStr = etQualityTarget.text.toString()

            if (durationStr.isEmpty() || qualityStr.isEmpty()) {
                Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = durationStr.toFloatOrNull()
            val quality = qualityStr.toIntOrNull()

            if (duration == null || quality == null) {
                Toast.makeText(context, "Format invalide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent double clicks
            btnSaveGoal.isEnabled = false
            btnSaveGoal.text = "Enregistrement..."

            saveGoalToFirestore(duration, quality)
        }
    }

    private fun saveGoalToFirestore(duration: Float, quality: Int) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
            resetSaveButton()
            return
        }

        val timestamp = System.currentTimeMillis()
        val dateString = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))

        // Determine if goal is "reached" based on last sleep data immediately (optional logic)
        val isReached = checkIfGoalReached(duration)

        val goalMap = hashMapOf(
            "duration" to duration,
            "quality" to quality,
            "dateCreated" to timestamp,
            "dateString" to dateString,
            "isReached" to isReached
        )

        firestore.collection("users").document(user.uid)
            .collection("goals")
            .add(goalMap)
            .addOnSuccessListener {
                if (isAdded && context != null) {
                    Toast.makeText(context, "Objectif enregistré !", Toast.LENGTH_SHORT).show()
                    etGoalDuration.text.clear()
                    etQualityTarget.text.clear()

                    // Refresh the list and tracking UI
                    fetchGoalHistory()
                    updateTrackingUI(duration)
                    resetSaveButton()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded && context != null) {
                    Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    resetSaveButton()
                }
            }
    }

    private fun resetSaveButton() {
        if (isAdded && context != null) {
            btnSaveGoal.isEnabled = true
            btnSaveGoal.text = getString(R.string.btn_save)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchGoalHistory() {
        val user = auth.currentUser ?: return

        firestore.collection("users").document(user.uid)
            .collection("goals")
            .orderBy("dateCreated", Query.Direction.DESCENDING)
            .limit(10) // Show last 10 goals
            .get()
            .addOnSuccessListener { documents ->
                goalsList.clear()
                for (document in documents) {
                    val goal = GoalModel(
                        id = document.id,
                        duration = document.getDouble("duration")?.toFloat() ?: 0f,
                        quality = document.getLong("quality")?.toInt() ?: 0,
                        dateCreated = document.getLong("dateCreated") ?: 0,
                        dateString = document.getString("dateString") ?: "",
                        isReached = document.getBoolean("isReached") ?: false
                    )
                    goalsList.add(goal)
                }
                adapter.notifyDataSetChanged()

                // If we have goals, use the latest one to update the "Tracking" section
                if (goalsList.isNotEmpty()) {
                    updateTrackingUI(goalsList[0].duration)
                }
            }
            .addOnFailureListener {
                // Handle error silently or log
            }
    }

    private fun checkLastSleepDuration(): Float {
        // Fetch the last sleep duration from SharedPreferences
        val sharedPref = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        val lastDurationStr = sharedPref.getString("last_sleep_duration", "00:00") ?: "00:00"

        // Convert HH:MM to float hours (e.g., "07:30" -> 7.5)
        return try {
            val parts = lastDurationStr.split(":")
            if (parts.size == 2) {
                val hours = parts[0].toFloat()
                val minutes = parts[1].toFloat()
                hours + (minutes / 60f)
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun checkIfGoalReached(targetDuration: Float): Boolean {
        val lastDuration = checkLastSleepDuration()
        // Goal is reached if we slept at least 95% of the target
        return lastDuration >= (targetDuration * 0.95f)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTrackingUI(targetDuration: Float) {
        updateTrackingProgress(targetDuration)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTrackingProgress(targetDuration: Float = 8f) {
        val lastSleepDuration = checkLastSleepDuration()

        // Calculate percentage (capped at 100%)
        var percentage = 0
        if (targetDuration > 0) {
            percentage = ((lastSleepDuration / targetDuration) * 100).toInt()
        }
        if (percentage > 100) percentage = 100

        progressBarGoals.progress = percentage

        // Format text
        val sleepFormatted = String.format("%.1fh", lastSleepDuration)
        val targetFormatted = String.format("%.1fh", targetDuration)

        tvProgressText.text = "$percentage% de l'objectif ($targetFormatted) atteint\nDernière nuit : $sleepFormatted"
    }
}

// Adapter Class
class GoalHistoryAdapter(private val items: List<GoalModel>) :
    RecyclerView.Adapter<GoalHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_goal_date)
        val tvDesc: TextView = view.findViewById(R.id.tv_goal_desc)
        val tvStatus: TextView = view.findViewById(R.id.tv_goal_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_history, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvDate.text = item.dateString
        holder.tvDesc.text = "Objectif ${item.duration}h / Qualité ${item.quality}%"

        if (item.isReached) {
            holder.tvStatus.text = "Atteint"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.cyan_accent))
        } else {
            holder.tvStatus.text = "Non atteint"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_light))
        }
    }

    override fun getItemCount() = items.size
}