package com.example.sleeptrackerapp


import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSelectDob: Button
    private lateinit var tvDobError: TextView
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView

    private var selectedDob: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialisation de Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Connexion des vues
        etUsername = findViewById(R.id.et_username)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnSelectDob = findViewById(R.id.btn_select_dob)
        tvDobError = findViewById(R.id.tv_dob_error)
        btnRegister = findViewById(R.id.btn_register)
        tvGoToLogin = findViewById(R.id.tv_go_to_login)

        btnSelectDob.setOnClickListener {
            showDatePicker()
        }

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val dob = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }
            selectedDob = dob
            btnSelectDob.text = "Date de naissance: ${selectedDay}/${selectedMonth + 1}/${selectedYear}"
            validateAge()
        }, year, month, day)

        // Empêcher la sélection de dates futures
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun validateAge(): Boolean {
        selectedDob?.let { dob ->
            val now = Calendar.getInstance()
            val eighteenYearsAgo = now.apply {
                add(Calendar.YEAR, -18)
            }

            if (dob.before(eighteenYearsAgo)) {
                tvDobError.visibility = View.GONE
                return true
            } else {
                tvDobError.visibility = View.VISIBLE
                return false
            }
        } ?: return false
    }

    private fun registerUser() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || selectedDob == null) {
            Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!validateAge()) {
            Toast.makeText(this, "Vous devez avoir au moins 18 ans.", Toast.LENGTH_LONG).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        // Étape 2: Stocker les infos supplémentaires dans Firestore
                        saveUserToFirestore(it.uid, username, selectedDob!!)

                        Toast.makeText(this, "Inscription réussie.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Log.w("RegisterActivity", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Erreur d'inscription: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserToFirestore(uid: String, username: String, dob: Calendar) {
        val user = hashMapOf(
            "username" to username,
            "email" to auth.currentUser?.email,
            "date_of_birth" to dob.time // Enregistrer la date comme un Timestamp
            // Ajoutez d'autres champs si nécessaire
        )

        firestore.collection("users").document(uid)
            .set(user)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)

            }
    }
}