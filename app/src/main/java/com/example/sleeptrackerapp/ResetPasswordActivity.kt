package com.example.sleeptrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var btnResetPassword: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // Initialisation de Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Connexion des vues
        etEmail = findViewById(R.id.et_email)
        btnResetPassword = findViewById(R.id.btn_reset_password)

        btnResetPassword.setOnClickListener {
            sendResetLink()
        }
    }

    private fun sendResetLink() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer votre email.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Lien de réinitialisation envoyé à votre email.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish() // Fermer l'activité après l'envoi
                } else {
                    Toast.makeText(
                        this,
                        "Erreur lors de l'envoi: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}