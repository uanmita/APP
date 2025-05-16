package com.example.ac_el_rinconcito

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ac_el_rinconcito.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configurar el listener para el botón de registro
        binding.buttonRegister.setOnClickListener {
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()
        val confirmPassword = binding.editTextConfirmPassword.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
            if (password == confirmPassword) {
                mostrarProgressBar(true)

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        mostrarProgressBar(false)

                        if (task.isSuccessful) {
                            // Registro exitoso
                            Toast.makeText(this, "Registro exitoso.", Toast.LENGTH_SHORT).show()
                            // Volver a la pantalla de login
                            finish()
                        } else {
                            // Error en el registro
                            val exception = task.exception
                            Log.e("RegisterActivity", "Error en el registro: ${exception?.message}", exception)
                            Toast.makeText(this, "Error en el registro: ${exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarProgressBar(mostrar: Boolean) {
        binding.progressBarRegister.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !mostrar
    }
} 