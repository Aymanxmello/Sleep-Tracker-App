package com.example.sleeptrackerapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AccountFragment : Fragment() {

    interface LogoutListener {
        fun onLogoutClicked()
    }

    private var logoutListener: LogoutListener? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Vues
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvDailyGoal: TextView
    private lateinit var tvTimezone: TextView
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var btnLogout: Button
    private lateinit var btnExportCsv: Button // Nouveau bouton

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LogoutListener) {
            logoutListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews(view)
        loadUserData()

        btnLogout.setOnClickListener {
            logoutListener?.onLogoutClicked()
        }

        // Gestion du switch Notifications
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Programmer un rappel à 22h30 par défaut
                scheduleSleepReminder(requireContext(), 22, 30)
                Toast.makeText(context, "Rappels activés (22:30)", Toast.LENGTH_SHORT).show()
            } else {
                cancelSleepReminder(requireContext())
                Toast.makeText(context, "Rappels désactivés", Toast.LENGTH_SHORT).show()
            }
        }

        // Gestion de l'Export CSV (Ajoutez ce bouton dans votre XML si non présent, ou utilisez un menu)
        // Pour cet exemple, supposons qu'on ajoute un bouton dans le layout ou qu'on utilise un existant
        // btnExportCsv.setOnClickListener { exportDataToCsv() }

        return view
    }

    private fun initializeViews(view: View) {
        tvUsername = view.findViewById(R.id.tv_username)
        tvEmail = view.findViewById(R.id.tv_email)
        tvDailyGoal = view.findViewById(R.id.tv_daily_goal)
        tvTimezone = view.findViewById(R.id.tv_timezone)
        switchNotifications = view.findViewById(R.id.switch_notifications)
        btnLogout = view.findViewById(R.id.btn_logout)
        // btnExportCsv = view.findViewById(R.id.btn_export_csv) // Décommentez si vous ajoutez le bouton au XML
    }

    private fun loadUserData() {
        val user = auth.currentUser
        tvEmail.text = user?.email ?: "utilisateur@example.com"
        tvTimezone.text = TimeZone.getDefault().id

        if (user != null) {
            firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    tvUsername.text = document.getString("username") ?: "Utilisateur"
                }

            firestore.collection("users").document(user.uid).collection("goals")
                .orderBy("dateCreated", Query.Direction.DESCENDING).limit(1).get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        tvDailyGoal.text = "${docs.documents[0].getDouble("duration")}h"
                    }
                }
        }
    }

    // Fonction d'export CSV
    fun exportDataToCsv() {
        val user = auth.currentUser ?: return
        Toast.makeText(context, "Génération du CSV...", Toast.LENGTH_SHORT).show()

        firestore.collection("users").document(user.uid).collection("sleep_sessions")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val fileName = "sleep_data_export.csv"
                    val file = File(requireContext().cacheDir, fileName)
                    val writer = FileWriter(file)

                    // En-tête CSV
                    writer.append("Date,Durée (h),Qualité\n")

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                    for (doc in documents) {
                        val dateMillis = doc.getLong("date") ?: 0L
                        val dateStr = dateFormat.format(Date(dateMillis))
                        val duration = doc.getDouble("durationHours") ?: 0.0
                        val quality = doc.getString("qualityText") ?: "N/A"

                        writer.append("$dateStr,$duration,$quality\n")
                    }
                    writer.flush()
                    writer.close()

                    shareCsvFile(file)

                } catch (e: Exception) {
                    Toast.makeText(context, "Erreur lors de l'export", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
    }

    private fun shareCsvFile(file: File) {
        // Note: Pour Android 7+, utilisez FileProvider dans le Manifest
        // Pour simplifier ici, on peut partager le contenu en texte brut si FileProvider est trop complexe à configurer immédiatement
        val content = file.readText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mon historique de sommeil")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, "Exporter via"))
    }

    override fun onDetach() {
        super.onDetach()
        logoutListener = null
    }

    companion object {
        fun newInstance() = AccountFragment()
    }
}