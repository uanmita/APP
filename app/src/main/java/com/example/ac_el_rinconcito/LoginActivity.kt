package com.example.ac_el_rinconcito

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ac_el_rinconcito.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Recuperar credenciales si están guardadas
        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val emailGuardado = prefs.getString("email", "") ?: ""
        val passwordGuardado = prefs.getString("password", "") ?: ""
        val recordar = prefs.getBoolean("recordar", false)
        binding.editTextEmail.setText(emailGuardado)
        binding.editTextPassword.setText(passwordGuardado)
        binding.checkBoxRemember.isChecked = recordar

        // Configurar el listener para el botón de inicio de sesión
        binding.buttonLogin.setOnClickListener {
            login()
        }

        // Configurar el listener para el botón de registro
        binding.buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showCustomToast(message: String) {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.custom_toast, null)
        val text = layout.findViewById<TextView>(R.id.toastText)
        text.text = message

        val toast = Toast(applicationContext)
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }

    private fun login() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val checkBoxTerms = binding.checkBoxTerms

        if (email.isEmpty() || password.isEmpty()) {
            showCustomToast("Por favor, completa todos los campos")
            return
        }

        if (!checkBoxTerms.isChecked) {
            showCustomToast("Debes aceptar las condiciones de uso para continuar")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showCustomToast("Inicio de sesión correcto")
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    showCustomToast("Error de autenticación: ${task.exception?.message}")
                }
            }
    }

    private fun mostrarProgressBar(mostrar: Boolean) {
        binding.progressBarLogin.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !mostrar
        binding.buttonRegister.isEnabled = !mostrar
    }
}

