package com.example.ac_el_rinconcito

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ac_el_rinconcito.adapters.ReservaAdapter
import com.example.ac_el_rinconcito.adapters.VehiculoAdapter
import com.example.ac_el_rinconcito.databinding.ActivityProfileBinding
import com.example.ac_el_rinconcito.models.Reserva
import com.example.ac_el_rinconcito.models.Vehiculo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var vehiculoAdapter: VehiculoAdapter
    private lateinit var reservaAdapter: ReservaAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Perfil de Usuario"

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configurar RecyclerViews
        setupRecyclerViews()

        // Cargar datos del usuario
        cargarDatosUsuario()

        // Configurar listeners de botones
        configurarBotones()
    }

    private fun setupRecyclerViews() {
        // Configurar RecyclerView de vehículos
        vehiculoAdapter = VehiculoAdapter { vehiculo ->
            Toast.makeText(this, "Vehículo: ${vehiculo.marca} ${vehiculo.modelo} (${vehiculo.matricula})", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewVehiculos.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = vehiculoAdapter
        }

        // Configurar RecyclerView de reservas
        reservaAdapter = ReservaAdapter { reserva ->
            Toast.makeText(this, "Reserva: ${reserva.id}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewReservas.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = reservaAdapter
        }
    }

    private fun cargarDatosUsuario() {
        val user = auth.currentUser
        user?.let { currentUser ->
            // Mostrar email
            binding.textViewEmail.text = currentUser.email

            // Obtener datos adicionales de Firestore
            db.collection("customers").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nombre = document.getString("name") ?: "Usuario"
                        binding.textViewName.text = nombre
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Error al cargar datos del usuario", e)
                    Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            // Cargar vehículos del usuario
            cargarVehiculos(currentUser.uid)

            // Cargar reservas del usuario
            currentUser.email?.let { cargarReservas(it) }
        }
    }

    private fun cargarVehiculos(userId: String) {
        db.collection("vehicles")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val vehiculos = documents.mapNotNull { doc ->
                    try {
                        Vehiculo(
                            id = doc.id,
                            marca = doc.getString("make") ?: "",
                            modelo = doc.getString("model") ?: "",
                            matricula = doc.getString("plate") ?: "",
                            userId = doc.getString("userId") ?: userId,
                            tipo = doc.getString("type") ?: "",
                            longitud = doc.getString("length") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("ProfileActivity", "Error al convertir documento de vehículo", e)
                        null
                    }
                }
                vehiculoAdapter.updateVehiculos(vehiculos)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar vehículos", e)
                Toast.makeText(this, "Error al cargar vehículos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarReservas(email: String) {
        db.collection("reservas")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                val reservas = documents.mapNotNull { doc ->
                    try {
                        val fechaLlegada = doc.getString("fechaLlegada")
                        val fechaSalida = doc.getString("fechaSalida")
                        val fechaReserva = doc.getString("fechaReserva")
                        Reserva(
                            id = doc.id,
                            userId = "", // No hay userId, puedes dejarlo vacío o usar el email
                            vehiculoId = doc.getString("reservaId") ?: "",
                            fechaInicio = fechaLlegada?.let { dateFormat.parse(it) } ?: Date(),
                            fechaFin = fechaSalida?.let { dateFormat.parse(it) } ?: Date(),
                            estado = doc.getString("estado") ?: "PENDIENTE",
                            precio = 0.0, // Si tienes campo precio, ponlo aquí
                            nombre = doc.getString("nombre") ?: "",
                            comentarios = doc.getString("comentarios") ?: "",
                            adultos = doc.getLong("adultos")?.toInt() ?: 0,
                            ninos = doc.getLong("ninos")?.toInt() ?: 0,
                            mascotas = doc.getLong("mascotas")?.toInt() ?: 0,
                            serviciosAdicionales = (doc.get("serviciosAdicionales") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        )
                    } catch (e: Exception) {
                        Log.e("ProfileActivity", "Error al convertir documento de reserva", e)
                        null
                    }
                }
                reservaAdapter.updateReservas(reservas)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar reservas", e)
                Toast.makeText(this, "Error al cargar reservas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarBotones() {
        binding.buttonEditProfile.setOnClickListener {
            // TODO: Implementar edición de perfil
            Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        binding.buttonChangePassword.setOnClickListener {
            // TODO: Implementar cambio de contraseña
            Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        binding.buttonAddVehiculo.setOnClickListener {
            // TODO: Implementar agregar vehículo
            Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        binding.buttonNuevaReserva.setOnClickListener {
            // TODO: Implementar nueva reserva
            Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 