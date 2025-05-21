package com.example.ac_el_rinconcito

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
            iniciarSesion()
        }

        // Configurar el listener para el botón de registro
        binding.buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun iniciarSesion() {
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()
        val recordar = binding.checkBoxRemember.isChecked

        if (email.isNotEmpty() && password.isNotEmpty()) {
            mostrarProgressBar(true)

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    mostrarProgressBar(false)

                    if (task.isSuccessful) {
                        // Guardar o limpiar credenciales según el checkbox
                        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
                        val editor = prefs.edit()
                        if (recordar) {
                            editor.putString("email", email)
                            editor.putString("password", password)
                            editor.putBoolean("recordar", true)
                        } else {
                            editor.clear()
                        }
                        editor.apply()

                        // Inicio de sesión correcto
                        Toast.makeText(this, "Inicio de sesión correcto.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Inicio de sesión fallido
                        val exception = task.exception
                        Log.e("LoginActivity", "Error en el inicio de sesión: ", exception)
                        Toast.makeText(this, "Error en el inicio de sesión: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarProgressBar(mostrar: Boolean) {
        binding.progressBarLogin.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !mostrar
        binding.buttonRegister.isEnabled = !mostrar
    }
}

