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
        vehiculoAdapter = VehiculoAdapter(
            onVehiculoClick = { vehiculo ->
                Toast.makeText(this, "Vehículo: ${vehiculo.marca} ${vehiculo.modelo} (${vehiculo.matricula})", Toast.LENGTH_SHORT).show()
            },
            onDeleteVehiculo = { vehiculo ->
                eliminarVehiculo(vehiculo)
            }
        )
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

    private fun eliminarVehiculo(vehiculo: Vehiculo) {
        db.collection("vehicles").document(vehiculo.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Vehículo eliminado", Toast.LENGTH_SHORT).show()
                cargarVehiculos(auth.currentUser?.uid ?: "")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar vehículo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarBotones() {
        binding.buttonEditProfile.setOnClickListener {
            // Diálogo para editar nombre
            val editText = android.widget.EditText(this)
            editText.hint = "Nuevo nombre"
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Editar perfil")
                .setView(editText)
                .setPositiveButton("Guardar") { _, _ ->
                    val nuevoNombre = editText.text.toString().trim()
                    val user = auth.currentUser
                    if (user != null && nuevoNombre.isNotEmpty()) {
                        db.collection("customers").document(user.uid)
                            .update("name", nuevoNombre)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                                binding.textViewName.text = nuevoNombre
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        binding.buttonChangePassword.setOnClickListener {
            // Diálogo para cambiar contraseña
            val editText = android.widget.EditText(this)
            editText.hint = "Nueva contraseña"
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cambiar contraseña")
                .setView(editText)
                .setPositiveButton("Cambiar") { _, _ ->
                    val nuevaPassword = editText.text.toString().trim()
                    val user = auth.currentUser
                    if (user != null && nuevaPassword.length >= 6) {
                        user.updatePassword(nuevaPassword)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.buttonAddVehiculo.setOnClickListener {
            // TODO: Implementar agregar vehículo
            Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        binding.buttonNuevaReserva.setOnClickListener {
            mostrarDialogoNuevaReserva()
        }

        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun mostrarDialogoNuevaReserva() {
        val context = this
        // Cargar vehículos del usuario antes de mostrar el diálogo
        val userId = auth.currentUser?.uid ?: return
        db.collection("vehicles")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val vehiculos = documents.mapNotNull { doc ->
                    Vehiculo(
                        id = doc.id,
                        marca = doc.getString("make") ?: "",
                        modelo = doc.getString("model") ?: "",
                        matricula = doc.getString("plate") ?: "",
                        userId = doc.getString("userId") ?: userId,
                        tipo = doc.getString("type") ?: "",
                        longitud = doc.getString("length") ?: ""
                    )
                }
                mostrarDialogoNuevaReservaConVehiculos(vehiculos)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al cargar vehículos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoNuevaReservaConVehiculos(vehiculos: List<Vehiculo>) {
        val context = this
        val layout = android.widget.ScrollView(context)
        val innerLayout = android.widget.LinearLayout(context)
        innerLayout.orientation = android.widget.LinearLayout.VERTICAL
        innerLayout.setPadding(50, 40, 50, 10)

        // Spinner para elegir vehículo
        val spinnerVehiculos = android.widget.Spinner(context)
        val vehiculoLabels = vehiculos.map { "${it.marca} ${it.modelo} (${it.matricula})" }
        val adapter = android.widget.ArrayAdapter(context, android.R.layout.simple_spinner_item, vehiculoLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVehiculos.adapter = adapter
        innerLayout.addView(spinnerVehiculos)

        val editTextNombre = android.widget.EditText(context)
        editTextNombre.hint = "Nombre"
        innerLayout.addView(editTextNombre)

        val editTextFechaReserva = android.widget.EditText(context)
        editTextFechaReserva.hint = "Fecha reserva (yyyy-MM-ddTHH:mm:ss.SSSZ)"
        innerLayout.addView(editTextFechaReserva)

        val editTextFechaLlegada = android.widget.EditText(context)
        editTextFechaLlegada.hint = "Fecha llegada (yyyy-MM-dd)"
        innerLayout.addView(editTextFechaLlegada)

        val editTextFechaSalida = android.widget.EditText(context)
        editTextFechaSalida.hint = "Fecha salida (yyyy-MM-dd)"
        innerLayout.addView(editTextFechaSalida)

        val editTextAdultos = android.widget.EditText(context)
        editTextAdultos.hint = "Adultos"
        editTextAdultos.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        innerLayout.addView(editTextAdultos)

        val editTextNinos = android.widget.EditText(context)
        editTextNinos.hint = "Niños"
        editTextNinos.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        innerLayout.addView(editTextNinos)

        val editTextMascotas = android.widget.EditText(context)
        editTextMascotas.hint = "Mascotas"
        editTextMascotas.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        innerLayout.addView(editTextMascotas)

        val editTextComentarios = android.widget.EditText(context)
        editTextComentarios.hint = "Comentarios"
        innerLayout.addView(editTextComentarios)

        // Servicios adicionales como checkboxes (excepto Vehículo extra)
        val serviciosDisponibles = listOf(
            "Electricidad",
            "Late check-out",
            "Lavado"
        )
        val checkBoxesServicios = serviciosDisponibles.map { servicio ->
            android.widget.CheckBox(context).apply { text = servicio }
        }
        val serviciosLayout = android.widget.LinearLayout(context)
        serviciosLayout.orientation = android.widget.LinearLayout.VERTICAL
        serviciosLayout.setPadding(0, 16, 0, 0)
        val labelServicios = android.widget.TextView(context)
        labelServicios.text = "Servicios adicionales:"
        serviciosLayout.addView(labelServicios)
        checkBoxesServicios.forEach { serviciosLayout.addView(it) }

        // Vehículo extra como RadioGroup
        val labelVehiculoExtra = android.widget.TextView(context)
        labelVehiculoExtra.text = "Vehículo extra (elige uno):"
        serviciosLayout.addView(labelVehiculoExtra)
        val radioGroupVehiculoExtra = android.widget.RadioGroup(context)
        val radioCoche = android.widget.RadioButton(context)
        radioCoche.text = "Coche (5€)"
        val radioMoto = android.widget.RadioButton(context)
        radioMoto.text = "Moto (5€)"
        radioGroupVehiculoExtra.addView(radioCoche)
        radioGroupVehiculoExtra.addView(radioMoto)
        serviciosLayout.addView(radioGroupVehiculoExtra)

        innerLayout.addView(serviciosLayout)

        layout.addView(innerLayout)

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Nueva Reserva")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val user = auth.currentUser
                val nombre = editTextNombre.text.toString().trim()
                val fechaReserva = editTextFechaReserva.text.toString().trim()
                val fechaLlegada = editTextFechaLlegada.text.toString().trim()
                val fechaSalida = editTextFechaSalida.text.toString().trim()
                val adultos = editTextAdultos.text.toString().toIntOrNull() ?: 0
                val ninos = editTextNinos.text.toString().toIntOrNull() ?: 0
                val mascotas = editTextMascotas.text.toString().toIntOrNull() ?: 0
                val comentarios = editTextComentarios.text.toString().trim()
                val serviciosAdicionales = checkBoxesServicios.filter { it.isChecked }.map { it.text.toString() }.toMutableList()
                val vehiculoExtraSeleccionado = when (radioGroupVehiculoExtra.checkedRadioButtonId) {
                    radioCoche.id -> "Vehículo extra coche (5€)"
                    radioMoto.id -> "Vehículo extra moto (5€)"
                    else -> null
                }
                if (vehiculoExtraSeleccionado != null) serviciosAdicionales.add(vehiculoExtraSeleccionado)
                val vehiculoSeleccionado = if (vehiculos.isNotEmpty()) vehiculos[spinnerVehiculos.selectedItemPosition] else null
                if (user != null && nombre.isNotEmpty() && fechaLlegada.isNotEmpty() && fechaSalida.isNotEmpty() && vehiculoSeleccionado != null) {
                    val reserva = hashMapOf(
                        "nombre" to nombre,
                        "email" to (user.email ?: ""),
                        "fechaReserva" to fechaReserva,
                        "fechaLlegada" to fechaLlegada,
                        "fechaSalida" to fechaSalida,
                        "adultos" to adultos,
                        "ninos" to ninos,
                        "mascotas" to mascotas,
                        "comentarios" to comentarios,
                        "serviciosAdicionales" to serviciosAdicionales,
                        "vehiculoId" to vehiculoSeleccionado.id,
                        "vehiculoNombre" to "${vehiculoSeleccionado.marca} ${vehiculoSeleccionado.modelo} (${vehiculoSeleccionado.matricula})",
                        "estado" to "pendiente"
                    )
                    db.collection("reservas")
                        .add(reserva)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Reserva creada", Toast.LENGTH_SHORT).show()
                            user.email?.let { cargarReservas(it) }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Completa los campos obligatorios y selecciona un vehículo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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