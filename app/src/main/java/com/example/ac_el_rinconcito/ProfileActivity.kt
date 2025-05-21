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

        // Pull-to-refresh para recargar reservas y vehículos
        val swipeRefreshLayout = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            cargarDatosUsuario()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupRecyclerViews() {
        vehiculoAdapter = VehiculoAdapter(
            onVehiculoClick = { vehiculo ->
                mostrarDialogoDetallesVehiculo(vehiculo)
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
            mostrarDialogoDetallesReserva(reserva)
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
                        val precio = when (val p = doc.get("precio")) {
                            is Number -> p.toDouble()
                            else -> 0.0
                        }
                        Reserva(
                            id = doc.id,
                            userId = "", // No hay userId, puedes dejarlo vacío o usar el email
                            vehiculoId = doc.getString("reservaId") ?: "",
                            fechaInicio = fechaLlegada?.let { dateFormat.parse(it) } ?: Date(),
                            fechaFin = fechaSalida?.let { dateFormat.parse(it) } ?: Date(),
                            estado = doc.getString("estado") ?: "PENDIENTE",
                            precio = precio,
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
            mostrarDialogoNuevoVehiculo()
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

        val buttonAddVehiculo = android.widget.Button(context)
        buttonAddVehiculo.text = "Agregar nuevo vehículo"
        buttonAddVehiculo.setOnClickListener { binding.buttonAddVehiculo.performClick() }
        innerLayout.addView(buttonAddVehiculo)

        val editTextNombre = android.widget.EditText(context)
        editTextNombre.hint = "Nombre"
        innerLayout.addView(editTextNombre)

        // Fecha de reserva automática y solo lectura
        val editTextFechaReserva = android.widget.EditText(context)
        editTextFechaReserva.hint = "Fecha reserva (auto)"
        editTextFechaReserva.isFocusable = false
        editTextFechaReserva.isClickable = false
        val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(java.util.Date())
        editTextFechaReserva.setText(now)
        innerLayout.addView(editTextFechaReserva)

        // Selector de fecha para llegada
        val editTextFechaLlegada = android.widget.EditText(context)
        editTextFechaLlegada.hint = "Fecha llegada (yyyy-MM-dd)"
        editTextFechaLlegada.isFocusable = false
        editTextFechaLlegada.isClickable = true
        innerLayout.addView(editTextFechaLlegada)
        editTextFechaLlegada.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            val dpd = android.app.DatePickerDialog(context, { _, year, month, dayOfMonth ->
                val mes = (month + 1).toString().padStart(2, '0')
                val dia = dayOfMonth.toString().padStart(2, '0')
                editTextFechaLlegada.setText("$year-$mes-$dia")
            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
            dpd.show()
        }

        // Selector de fecha para salida
        val editTextFechaSalida = android.widget.EditText(context)
        editTextFechaSalida.hint = "Fecha salida (yyyy-MM-dd)"
        editTextFechaSalida.isFocusable = false
        editTextFechaSalida.isClickable = true
        innerLayout.addView(editTextFechaSalida)
        editTextFechaSalida.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            val dpd = android.app.DatePickerDialog(context, { _, year, month, dayOfMonth ->
                val mes = (month + 1).toString().padStart(2, '0')
                val dia = dayOfMonth.toString().padStart(2, '0')
                editTextFechaSalida.setText("$year-$mes-$dia")
            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
            dpd.show()
        }

        // NumberPickers para adultos, niños y mascotas
        val npAdultos = android.widget.NumberPicker(context)
        npAdultos.minValue = 0
        npAdultos.maxValue = 20
        npAdultos.value = 2
        val labelAdultos = android.widget.TextView(context)
        labelAdultos.text = "Adultos"
        innerLayout.addView(labelAdultos)
        innerLayout.addView(npAdultos)

        val npNinos = android.widget.NumberPicker(context)
        npNinos.minValue = 0
        npNinos.maxValue = 10
        val labelNinos = android.widget.TextView(context)
        labelNinos.text = "Niños"
        innerLayout.addView(labelNinos)
        innerLayout.addView(npNinos)

        val npMascotas = android.widget.NumberPicker(context)
        npMascotas.minValue = 0
        npMascotas.maxValue = 5
        val labelMascotas = android.widget.TextView(context)
        labelMascotas.text = "Mascotas"
        innerLayout.addView(labelMascotas)
        innerLayout.addView(npMascotas)

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

        // TextView para mostrar el precio total en tiempo real
        val textViewPrecio = android.widget.TextView(context)
        textViewPrecio.textSize = 18f
        textViewPrecio.setPadding(0, 24, 0, 24)
        innerLayout.addView(textViewPrecio)

        layout.addView(innerLayout)

        // Función para calcular y mostrar el precio en tiempo real
        fun calcularYMostrarPrecio() {
            val db = FirebaseFirestore.getInstance()
            val pricesRef = db.collection("prices")
            pricesRef.document("huespedes").get().addOnSuccessListener { docHuespedes ->
                pricesRef.document("vehicles_base").get().addOnSuccessListener { docVehiculos ->
                    val preciosServicios = mapOf(
                        "Electricidad" to 5,
                        "Late check-out" to 10,
                        "Lavado" to 12,
                        "Vehículo extra coche (5€)" to 5,
                        "Vehículo extra moto (5€)" to 5
                    )
                    val precioAdulto = docHuespedes.getLong("adultos")?.toInt() ?: 0
                    val precioNino = docHuespedes.getLong("niños")?.toInt() ?: 0
                    val precioMascota = docHuespedes.getLong("mascota")?.toInt() ?: 0
                    val tipoVehiculo = if (vehiculos.isNotEmpty()) vehiculos[spinnerVehiculos.selectedItemPosition].tipo.lowercase().replace("á", "a").replace(".", "").replace(" ", "") else ""
                    val precioVehiculoBase = when {
                        tipoVehiculo.contains("autocaravana") -> docVehiculos.getLong("autocaravana")?.toInt() ?: 0
                        tipoVehiculo.contains("camper") && !tipoVehiculo.contains("furgoneta") -> docVehiculos.getLong("camper")?.toInt() ?: 0
                        tipoVehiculo.contains("caravana") -> docVehiculos.getLong("caravana")?.toInt() ?: 0
                        tipoVehiculo.contains("furgoneta") -> docVehiculos.getLong("furgoneta camper.")?.toInt() ?: 0
                        else -> 0
                    }
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
                    val dateIn = editTextFechaLlegada.text.toString().let { if (it.isNotEmpty()) sdf.parse(it) else null }
                    val dateOut = editTextFechaSalida.text.toString().let { if (it.isNotEmpty()) sdf.parse(it) else null }
                    val noches = if (dateIn != null && dateOut != null) {
                        val diff = dateOut.time - dateIn.time
                        (diff / (1000 * 60 * 60 * 24)).toInt()
                    } else 1
                    val adultos = npAdultos.value
                    val ninos = npNinos.value
                    val mascotas = npMascotas.value
                    val serviciosAdicionales = checkBoxesServicios.filter { it.isChecked }.map { it.text.toString() }.toMutableList()
                    val vehiculoExtraSeleccionado = when (radioGroupVehiculoExtra.checkedRadioButtonId) {
                        radioCoche.id -> "Vehículo extra coche (5€)"
                        radioMoto.id -> "Vehículo extra moto (5€)"
                        else -> null
                    }
                    if (vehiculoExtraSeleccionado != null) serviciosAdicionales.add(vehiculoExtraSeleccionado)
                    var precioTotal = 0
                    precioTotal += adultos * precioAdulto * noches
                    precioTotal += ninos * precioNino * noches
                    precioTotal += mascotas * precioMascota * noches
                    precioTotal += precioVehiculoBase * noches
                    serviciosAdicionales.forEach { servicio ->
                        when (servicio) {
                            "Electricidad" -> precioTotal += (preciosServicios[servicio] ?: 0) * noches
                            "Late check-out", "Lavado", "Vehículo extra coche (5€)", "Vehículo extra moto (5€)" -> precioTotal += preciosServicios[servicio] ?: 0
                        }
                    }
                    textViewPrecio.text = "Precio total estimado: ${precioTotal}€"
                }
            }
        }

        // Listeners para recalcular el precio en tiempo real
        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { calcularYMostrarPrecio() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        editTextFechaLlegada.addTextChangedListener(watcher)
        editTextFechaSalida.addTextChangedListener(watcher)
        spinnerVehiculos.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) { calcularYMostrarPrecio() }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
        checkBoxesServicios.forEach { it.setOnCheckedChangeListener { _, _ -> calcularYMostrarPrecio() } }
        radioGroupVehiculoExtra.setOnCheckedChangeListener { _, _ -> calcularYMostrarPrecio() }
        npAdultos.setOnValueChangedListener { _, _, _ -> calcularYMostrarPrecio() }
        npNinos.setOnValueChangedListener { _, _, _ -> calcularYMostrarPrecio() }
        npMascotas.setOnValueChangedListener { _, _, _ -> calcularYMostrarPrecio() }

        // Mostrar precio inicial
        calcularYMostrarPrecio()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Nueva Reserva")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val user = auth.currentUser
                val nombre = editTextNombre.text.toString().trim()
                val fechaReserva = editTextFechaReserva.text.toString().trim()
                val fechaLlegada = editTextFechaLlegada.text.toString().trim()
                val fechaSalida = editTextFechaSalida.text.toString().trim()
                val adultos = npAdultos.value
                val ninos = npNinos.value
                val mascotas = npMascotas.value
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
                    calcularPrecioYGuardarReserva(
                        user,
                        nombre,
                        fechaReserva,
                        fechaLlegada,
                        fechaSalida,
                        adultos,
                        ninos,
                        mascotas,
                        comentarios,
                        serviciosAdicionales,
                        vehiculoSeleccionado,
                        vehiculoExtraSeleccionado
                    )
                } else {
                    Toast.makeText(context, "Completa los campos obligatorios y selecciona un vehículo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun calcularPrecioYGuardarReserva(
        user: com.google.firebase.auth.FirebaseUser,
        nombre: String,
        fechaReserva: String,
        fechaLlegada: String,
        fechaSalida: String,
        adultos: Int,
        ninos: Int,
        mascotas: Int,
        comentarios: String,
        serviciosAdicionales: List<String>,
        vehiculoSeleccionado: Vehiculo,
        vehiculoExtraSeleccionado: String?
    ) {
        val context = this
        val db = FirebaseFirestore.getInstance()
        // Leer precios de Firestore
        val pricesRef = db.collection("prices")
        pricesRef.document("huespedes").get().addOnSuccessListener { docHuespedes ->
            pricesRef.document("vehicles_base").get().addOnSuccessListener { docVehiculos ->
                // Leer precios de servicios extra (hardcodeados aquí, puedes leerlos de Firestore si los tienes)
                val preciosServicios = mapOf(
                    "Electricidad" to 5,
                    "Late check-out" to 10,
                    "Lavado" to 12,
                    "Vehículo extra coche (5€)" to 5,
                    "Vehículo extra moto (5€)" to 5
                )
                // Precios de huéspedes
                val precioAdulto = docHuespedes.getLong("adultos")?.toInt() ?: 0
                val precioNino = docHuespedes.getLong("niños")?.toInt() ?: 0
                val precioMascota = docHuespedes.getLong("mascota")?.toInt() ?: 0
                // Precio del vehículo base
                val tipoVehiculo = vehiculoSeleccionado.tipo.lowercase().replace("á", "a").replace(".", "").replace(" ", "")
                val precioVehiculoBase = when {
                    tipoVehiculo.contains("autocaravana") -> docVehiculos.getLong("autocaravana")?.toInt() ?: 0
                    tipoVehiculo.contains("camper") && !tipoVehiculo.contains("furgoneta") -> docVehiculos.getLong("camper")?.toInt() ?: 0
                    tipoVehiculo.contains("caravana") -> docVehiculos.getLong("caravana")?.toInt() ?: 0
                    tipoVehiculo.contains("furgoneta") -> docVehiculos.getLong("furgoneta camper.")?.toInt() ?: 0
                    else -> 0
                }
                // Calcular noches
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
                val dateIn = sdf.parse(fechaLlegada)
                val dateOut = sdf.parse(fechaSalida)
                val noches = if (dateIn != null && dateOut != null) {
                    val diff = dateOut.time - dateIn.time
                    (diff / (1000 * 60 * 60 * 24)).toInt()
                } else 1
                // Calcular precio total
                var precioTotal = 0
                precioTotal += adultos * precioAdulto * noches
                precioTotal += ninos * precioNino * noches
                precioTotal += mascotas * precioMascota * noches
                precioTotal += precioVehiculoBase * noches
                // Servicios adicionales
                serviciosAdicionales.forEach { servicio ->
                    when (servicio) {
                        "Electricidad" -> precioTotal += (preciosServicios[servicio] ?: 0) * noches
                        "Late check-out", "Lavado", "Vehículo extra coche (5€)", "Vehículo extra moto (5€)" -> precioTotal += preciosServicios[servicio] ?: 0
                    }
                }
                // Guardar reserva
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
                    "estado" to "pendiente",
                    "precio" to precioTotal
                )
                db.collection("reservas")
                    .add(reserva)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Reserva creada. Precio total: ${precioTotal}€", Toast.LENGTH_LONG).show()
                        user.email?.let { cargarReservas(it) }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun mostrarDialogoNuevoVehiculo() {
        val context = this
        val layout = android.widget.LinearLayout(context)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val editTextMarca = android.widget.EditText(context)
        editTextMarca.hint = "Marca"
        layout.addView(editTextMarca)

        val editTextModelo = android.widget.EditText(context)
        editTextModelo.hint = "Modelo"
        layout.addView(editTextModelo)

        val editTextMatricula = android.widget.EditText(context)
        editTextMatricula.hint = "Matrícula"
        layout.addView(editTextMatricula)

        // Spinner para tipo de vehículo
        val tiposVehiculo = listOf("autocaravana", "camper", "caravana", "furgoneta camper.")
        val spinnerTipo = android.widget.Spinner(context)
        val adapterTipo = android.widget.ArrayAdapter(context, android.R.layout.simple_spinner_item, tiposVehiculo)
        adapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapterTipo
        layout.addView(spinnerTipo)

        val editTextLongitud = android.widget.EditText(context)
        editTextLongitud.hint = "Longitud (opcional)"
        layout.addView(editTextLongitud)

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Nuevo Vehículo")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val user = auth.currentUser
                val marca = editTextMarca.text.toString().trim()
                val modelo = editTextModelo.text.toString().trim()
                val matricula = editTextMatricula.text.toString().trim()
                val tipo = tiposVehiculo[spinnerTipo.selectedItemPosition]
                val longitud = editTextLongitud.text.toString().trim()
                if (user != null && marca.isNotEmpty() && modelo.isNotEmpty() && matricula.isNotEmpty() && tipo.isNotEmpty()) {
                    val vehiculo = hashMapOf(
                        "make" to marca,
                        "model" to modelo,
                        "plate" to matricula,
                        "type" to tipo,
                        "length" to longitud,
                        "userId" to user.uid
                    )
                    db.collection("vehicles")
                        .add(vehiculo)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Vehículo añadido", Toast.LENGTH_SHORT).show()
                            cargarVehiculos(user.uid)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoDetallesReserva(reserva: Reserva) {
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Detalles de la reserva")
        val mensaje = StringBuilder()
        mensaje.append("Nombre: ${reserva.nombre}\n")
        mensaje.append("Estado: ${reserva.estado}\n")
        mensaje.append("Fecha de llegada: ${dateFormat.format(reserva.fechaInicio)}\n")
        mensaje.append("Fecha de salida: ${dateFormat.format(reserva.fechaFin)}\n")
        mensaje.append("Adultos: ${reserva.adultos}\n")
        mensaje.append("Niños: ${reserva.ninos}\n")
        mensaje.append("Mascotas: ${reserva.mascotas}\n")
        mensaje.append("Precio: ${if (reserva.precio > 0.0) String.format("%.2f €", reserva.precio) else "No calculado"}\n")
        mensaje.append("Servicios adicionales: ${if (reserva.serviciosAdicionales.isNotEmpty()) reserva.serviciosAdicionales.joinToString(", ") else "Ninguno"}\n")
        mensaje.append("Comentarios: ${if (reserva.comentarios.isNotEmpty()) reserva.comentarios else "Sin comentarios"}\n")
        builder.setMessage(mensaje.toString())
        builder.setPositiveButton("Cerrar", null)
        builder.show()
    }

    private fun mostrarDialogoDetallesVehiculo(vehiculo: Vehiculo) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Detalles del vehículo")
        val mensaje = StringBuilder()
        mensaje.append("Marca: ${vehiculo.marca}\n")
        mensaje.append("Modelo: ${vehiculo.modelo}\n")
        mensaje.append("Matrícula: ${vehiculo.matricula}\n")
        mensaje.append("Tipo: ${vehiculo.tipo}\n")
        mensaje.append("Longitud: ${if (vehiculo.longitud.isNotEmpty()) vehiculo.longitud else "No especificada"}\n")
        builder.setMessage(mensaje.toString())
        builder.setPositiveButton("Cerrar", null)
        builder.show()
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