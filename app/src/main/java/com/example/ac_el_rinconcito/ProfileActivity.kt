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
import android.widget.TextView
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

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
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Perfil de Usuario"
        }

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

                        // Lógica de avatar: comprobar si hay avatar guardado, si no, asignar aleatorio
                        val avatar = document.getString("avatar")
                        val avatarRes = when (avatar) {
                            "female" -> R.drawable.ic_woman
                            "male" -> R.drawable.ic_man
                            else -> {
                                // Asignar aleatorio y guardar
                                val random = if ((0..1).random() == 0) "male" else "female"
                                db.collection("customers").document(currentUser.uid).update("avatar", random)
                                if (random == "female") R.drawable.ic_woman else R.drawable.ic_man
                            }
                        }
                        binding.profileImage.setImageResource(avatarRes)

                        // Cambiar avatar al hacer click y guardar
                        binding.profileImage.setOnClickListener {
                            val nuevoAvatar = if (avatar == "female") "male" else "female"
                            db.collection("customers").document(currentUser.uid).update("avatar", nuevoAvatar)
                            val nuevoRes = if (nuevoAvatar == "female") R.drawable.ic_woman else R.drawable.ic_man
                            binding.profileImage.setImageResource(nuevoRes)
                        }
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
        Log.d("DEBUG", "UID actual: $userId")
        db.collection("vehicles")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val vehiculos = documents.mapNotNull { doc ->
                    try {
                        Vehiculo(
                            id = doc.id,
                            marca = doc.getString("marca") ?: doc.getString("make") ?: "",
                            modelo = doc.getString("modelo") ?: doc.getString("model") ?: "",
                            matricula = doc.getString("matricula") ?: doc.getString("plate") ?: "",
                            userId = doc.getString("userId") ?: userId,
                            tipo = doc.getString("tipo") ?: doc.getString("type") ?: "",
                            longitud = doc.getString("longitud") ?: doc.getString("length") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("ProfileActivity", "Error al convertir documento de vehículo", e)
                        null
                    }
                }
                Log.d("DEBUG", "Vehículos encontrados: ${vehiculos.size}")
                vehiculoAdapter.updateVehiculos(vehiculos)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar vehículos", e)
                Toast.makeText(this, "Error al cargar vehículos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarReservas(email: String) {
        Log.d("DEBUG", "Email actual: $email")
        db.collection("reservas")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                obtenerPreciosYMoneda { precios, preciosVehiculos, preciosExtras, moneda ->
                    db.collection("vehicles").get().addOnSuccessListener { vehiculosDocs ->
                        val vehiculosMap = vehiculosDocs.associateBy { it.id }
                        val reservas = documents.mapNotNull { doc ->
                            try {
                                val fechaLlegada = doc.getString("fechaLlegada")
                                val fechaSalida = doc.getString("fechaSalida")
                                val fechaReserva = doc.getString("fechaReserva")
                                val adultos = doc.getLong("adultos")?.toInt() ?: 0
                                val ninos = doc.getLong("ninos")?.toInt() ?: 0
                                val mascotas = doc.getLong("mascotas")?.toInt() ?: 0
                                val serviciosAdicionales = (doc.get("serviciosAdicionales") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                val vehiculoId = doc.getString("vehiculoId") ?: ""
                                val vehiculoTipo = vehiculosMap[vehiculoId]?.getString("tipo") ?: vehiculosMap[vehiculoId]?.getString("type") ?: doc.getString("tipoVehiculo") ?: ""
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
                                val dateIn = fechaLlegada?.let { if (it.isNotEmpty()) sdf.parse(it) else null }
                                val dateOut = fechaSalida?.let { if (it.isNotEmpty()) sdf.parse(it) else null }
                                val noches = if (dateIn != null && dateOut != null) {
                                    val diff = dateOut.time - dateIn.time
                                    (diff / (1000 * 60 * 60 * 24)).toInt()
                                } else 1
                                val precioGuardado = when (val p = doc.get("precio")) {
                                    is Number -> p.toDouble()
                                    else -> 0.0
                                }
                                val precioCalculado = if (precioGuardado > 0.0) precioGuardado else calcularPrecioReserva(
                                    adultos, ninos, mascotas, noches, vehiculoTipo, serviciosAdicionales,
                                    precios, preciosVehiculos, preciosExtras
                                ).toDouble()
                                Log.d("DEBUG", "Reserva ${doc.id} - Precio mostrado: $precioCalculado")
                                Reserva(
                                    id = doc.id,
                                    userId = doc.getString("userId") ?: "",
                                    vehiculoId = vehiculoId,
                                    fechaInicio = fechaLlegada?.let { dateFormat.parse(it) } ?: Date(),
                                    fechaFin = fechaSalida?.let { dateFormat.parse(it) } ?: Date(),
                                    estado = doc.getString("estado") ?: "PENDIENTE",
                                    precio = precioCalculado,
                                    nombre = doc.getString("nombre") ?: "",
                                    comentarios = doc.getString("comentarios") ?: "",
                                    adultos = adultos,
                                    ninos = ninos,
                                    mascotas = mascotas,
                                    serviciosAdicionales = serviciosAdicionales,
                                    origen = doc.getString("origen") ?: "web"
                                )
                            } catch (e: Exception) {
                                Log.e("ProfileActivity", "Error al convertir documento de reserva", e)
                                null
                            }
                        }
                        Log.d("DEBUG", "Reservas encontradas: ${reservas.size}")
                        reservaAdapter.updateReservas(reservas)
                    }
                }
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
        val userId = auth.currentUser?.uid ?: return
        Log.d("DEBUG", "Buscando vehículos para userId: $userId")
        
        db.collection("vehicles")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("DEBUG", "Documentos de vehículos encontrados: ${documents.size()}")
                documents.forEach { doc ->
                    Log.d("DEBUG", "Documento vehículo: ${doc.data}")
                }
                
                val vehiculos = documents.mapNotNull { doc ->
                    try {
                        val vehiculo = Vehiculo(
                            id = doc.id,
                            marca = doc.getString("marca") ?: doc.getString("make") ?: "",
                            modelo = doc.getString("modelo") ?: doc.getString("model") ?: "",
                            matricula = doc.getString("matricula") ?: doc.getString("plate") ?: "",
                            userId = doc.getString("userId") ?: userId,
                            tipo = doc.getString("tipo") ?: doc.getString("type") ?: "",
                            longitud = doc.getString("longitud") ?: doc.getString("length") ?: ""
                        )
                        Log.d("DEBUG", "Vehículo mapeado: $vehiculo")
                        vehiculo
                    } catch (e: Exception) {
                        Log.e("DEBUG", "Error al mapear vehículo: ${e.message}")
                        null
                    }
                }
                Log.d("DEBUG", "Total vehículos mapeados: ${vehiculos.size}")
                
                if (vehiculos.isEmpty()) {
                    Toast.makeText(context, "No tienes vehículos registrados. Añade uno primero.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                mostrarDialogoNuevaReservaConVehiculos(vehiculos)
            }
            .addOnFailureListener { e ->
                Log.e("DEBUG", "Error al cargar vehículos: ${e.message}")
                Toast.makeText(context, "Error al cargar vehículos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoNuevaReservaConVehiculos(vehiculos: List<Vehiculo>) {
        Log.d("DEBUG", "Mostrando diálogo con ${vehiculos.size} vehículos")
        val dialogView = layoutInflater.inflate(R.layout.dialog_nueva_reserva, null)
        val spinnerVehiculos = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.spinnerVehiculos)
        val editTextNombre = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextNombre)
        val editTextFechaLlegada = dialogView.findViewById<android.widget.EditText>(R.id.editTextFechaLlegada)
        val editTextFechaSalida = dialogView.findViewById<android.widget.EditText>(R.id.editTextFechaSalida)
        val npAdultos = dialogView.findViewById<android.widget.NumberPicker>(R.id.npAdultos)
        val npNinos = dialogView.findViewById<android.widget.NumberPicker>(R.id.npNinos)
        val npMascotas = dialogView.findViewById<android.widget.NumberPicker>(R.id.npMascotas)
        val editTextComentarios = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextComentarios)
        val layoutServicios = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutServicios)
        val textViewPrecio = dialogView.findViewById<android.widget.TextView>(R.id.textViewPrecio)

        // Configurar el AutoCompleteTextView de vehículos SOLO con marca, modelo y matrícula
        val vehiculoLabels = vehiculos.map { "${it.marca} ${it.modelo} (${it.matricula})" }
        Log.d("DEBUG", "Labels de vehículos: $vehiculoLabels")
        
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            vehiculoLabels
        )
        spinnerVehiculos.setAdapter(adapter)
        
        // Asegurarse de que el AutoCompleteTextView muestre el primer elemento por defecto
        if (vehiculos.isNotEmpty()) {
            spinnerVehiculos.setText(vehiculoLabels[0], false)
            Log.d("DEBUG", "Texto inicial establecido: ${vehiculoLabels[0]}")
        }

        // Configurar el estilo del dropdown
        spinnerVehiculos.setOnClickListener {
            spinnerVehiculos.showDropDown()
        }

        // Asegurarse de que el dropdown se muestre correctamente
        spinnerVehiculos.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                spinnerVehiculos.showDropDown()
            }
        }

        // NumberPickers
        npAdultos.minValue = 0
        npAdultos.maxValue = 20
        npAdultos.value = 2
        npNinos.minValue = 0
        npNinos.maxValue = 10
        npMascotas.minValue = 0
        npMascotas.maxValue = 5

        // Fecha llegada/salida con DatePicker
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
        editTextFechaLlegada.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            val dpd = android.app.DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val mes = (month + 1).toString().padStart(2, '0')
                val dia = dayOfMonth.toString().padStart(2, '0')
                editTextFechaLlegada.setText("$year-$mes-$dia")
            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
            dpd.show()
        }
        editTextFechaSalida.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            val dpd = android.app.DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val mes = (month + 1).toString().padStart(2, '0')
                val dia = dayOfMonth.toString().padStart(2, '0')
                editTextFechaSalida.setText("$year-$mes-$dia")
            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
            dpd.show()
        }

        obtenerPreciosYMoneda { precios, preciosVehiculos, preciosExtras, moneda ->
            val nombresServiciosLegibles = mapOf(
                "late_ch_out" to "Late check out (20.00 horas)",
                "coche_extra" to "Coche extra en parcela",
                "electricidad" to "Electricidad por noche",
                "moto_extra" to "Moto extra en parcela"
            )
            val serviciosDisponibles = preciosExtras.keys.toList()
            val checkBoxesServicios = serviciosDisponibles.map { servicio ->
                android.widget.CheckBox(this).apply {
                    text = nombresServiciosLegibles[servicio] ?: servicio.replaceFirstChar { it.uppercase() }
                }
            }
            layoutServicios.removeAllViews()
            checkBoxesServicios.forEach { layoutServicios.addView(it) }

            // Función para calcular y mostrar el precio en tiempo real
            fun calcularYMostrarPrecio() {
                val dateIn = editTextFechaLlegada.text.toString().let { if (it.isNotEmpty()) sdf.parse(it) else null }
                val dateOut = editTextFechaSalida.text.toString().let { if (it.isNotEmpty()) sdf.parse(it) else null }
                val noches = if (dateIn != null && dateOut != null) {
                    val diff = dateOut.time - dateIn.time
                    (diff / (1000 * 60 * 60 * 24)).toInt()
                } else 1
                val adultos = npAdultos.value
                val ninos = npNinos.value
                val mascotas = npMascotas.value
                val serviciosAdicionales = checkBoxesServicios.filter { it.isChecked }.map { it.text.toString() }
                val vehiculoSeleccionado = if (vehiculos.isNotEmpty()) vehiculos[vehiculoLabels.indexOf(spinnerVehiculos.text.toString())] else null
                val tipoVehiculo = vehiculoSeleccionado?.tipo ?: ""
                val precioTotal = calcularPrecioReserva(
                    adultos, ninos, mascotas, noches, tipoVehiculo, serviciosAdicionales,
                    precios, preciosVehiculos, preciosExtras
                )
                textViewPrecio.text = "Precio estimado: ${precioTotal} $moneda"
            }

            // Listeners para recalcular el precio en tiempo real
            val watcher = object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) { calcularYMostrarPrecio() }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            editTextFechaLlegada.addTextChangedListener(watcher)
            editTextFechaSalida.addTextChangedListener(watcher)
            spinnerVehiculos.addTextChangedListener(watcher)
            checkBoxesServicios.forEach { it.setOnCheckedChangeListener { _, _ -> calcularYMostrarPrecio() } }
            npAdultos.setOnValueChangedListener { _, _, _ -> calcularYMostrarPrecio() }
            npNinos.setOnValueChangedListener { _, _, _ -> calcularYMostrarPrecio() }
            npMascotas.setOnValueChangedListener { _, _, _ -> calcularYMostrarPrecio() }

            // Mostrar precio inicial
            calcularYMostrarPrecio()

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCancelarReserva).setOnClickListener {
                dialog.dismiss()
            }
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonGuardarReserva).setOnClickListener {
                val user = auth.currentUser
                val nombre = editTextNombre.text.toString().trim()
                val fechaLlegada = editTextFechaLlegada.text.toString().trim()
                val fechaSalida = editTextFechaSalida.text.toString().trim()
                val adultos = npAdultos.value
                val ninos = npNinos.value
                val mascotas = npMascotas.value
                val comentarios = editTextComentarios.text.toString().trim()
                val serviciosAdicionales = checkBoxesServicios.filter { it.isChecked }.map { it.text.toString() }
                val vehiculoSeleccionado = if (vehiculos.isNotEmpty()) vehiculos[vehiculoLabels.indexOf(spinnerVehiculos.text.toString())] else null
                val tipoVehiculo = vehiculoSeleccionado?.tipo ?: ""
                val dateIn = fechaLlegada.let { if (it.isNotEmpty()) sdf.parse(it) else null }
                val dateOut = fechaSalida.let { if (it.isNotEmpty()) sdf.parse(it) else null }
                val noches = if (dateIn != null && dateOut != null) {
                    val diff = dateOut.time - dateIn.time
                    (diff / (1000 * 60 * 60 * 24)).toInt()
                } else 1
                val precioTotal = calcularPrecioReserva(
                    adultos, ninos, mascotas, noches, tipoVehiculo, serviciosAdicionales,
                    precios, preciosVehiculos, preciosExtras
                )
                if (user != null && nombre.isNotEmpty() && fechaLlegada.isNotEmpty() && fechaSalida.isNotEmpty() && vehiculoSeleccionado != null) {
                    val reserva = hashMapOf(
                        "nombre" to nombre,
                        "email" to (user.email ?: ""),
                        "fechaReserva" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(java.util.Date()),
                        "fechaLlegada" to fechaLlegada,
                        "fechaSalida" to fechaSalida,
                        "adultos" to adultos,
                        "ninos" to ninos,
                        "mascotas" to mascotas,
                        "comentarios" to comentarios,
                        "serviciosAdicionales" to serviciosAdicionales,
                        "vehiculoId" to vehiculoSeleccionado.id,
                        "vehiculoNombre" to "${vehiculoSeleccionado.marca} ${vehiculoSeleccionado.modelo} (${vehiculoSeleccionado.matricula})",
                        "tipoVehiculo" to tipoVehiculo,
                        "estado" to "pendiente",
                        "precio" to precioTotal,
                        "moneda" to moneda,
                        "origen" to "app"
                    )
                    db.collection("reservas")
                        .add(reserva)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Reserva creada. Precio total: ${precioTotal} $moneda", Toast.LENGTH_LONG).show()
                            user.email?.let { cargarReservas(it) }
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Completa los campos obligatorios y selecciona un vehículo", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.show()
        }
    }

    private fun mostrarDialogoNuevoVehiculo() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_nuevo_vehiculo, null)
        val editTextMarca = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextMarca)
        val editTextModelo = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextModelo)
        val editTextMatricula = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextMatricula)
        val autoCompleteTipo = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.autoCompleteTipo)
        val editTextLongitud = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextLongitud)

        val tiposVehiculo = listOf("autocaravana", "camper", "caravana", "furgoneta camper")
        val tiposVehiculoCapitalizados = tiposVehiculo.map { it.replaceFirstChar { c -> c.uppercase() } }
        val adapterTipo = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tiposVehiculoCapitalizados)
        autoCompleteTipo.setAdapter(adapterTipo)
        // Forzar apertura del menú al hacer clic o enfocar
        autoCompleteTipo.setOnClickListener { autoCompleteTipo.showDropDown() }
        autoCompleteTipo.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) autoCompleteTipo.showDropDown() }
        // Evitar que el usuario escriba valores no válidos
        autoCompleteTipo.setKeyListener(null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCancelarVehiculo).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonGuardarVehiculo).setOnClickListener {
            val user = auth.currentUser
            val marca = editTextMarca.text.toString().trim()
            val modelo = editTextModelo.text.toString().trim()
            val matricula = editTextMatricula.text.toString().trim()
            val tipo = autoCompleteTipo.text.toString().trim()
            val longitud = editTextLongitud.text.toString().trim()
            if (user != null && marca.isNotEmpty() && modelo.isNotEmpty() && matricula.isNotEmpty() && tipo.isNotEmpty()) {
                val tiposVehiculo = listOf("autocaravana", "camper", "caravana", "furgoneta camper")
                val tipoParaGuardar = tiposVehiculo.find { tipo.equals(it, ignoreCase = true) } ?: tipo.lowercase()
                val vehiculo = hashMapOf(
                    "make" to marca,
                    "model" to modelo,
                    "plate" to matricula,
                    "type" to tipoParaGuardar,
                    "length" to longitud,
                    "userId" to user.uid
                )
                db.collection("vehicles")
                    .add(vehiculo)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Vehículo añadido", Toast.LENGTH_SHORT).show()
                        cargarVehiculos(user.uid)
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun mostrarDialogoDetallesReserva(reserva: Reserva) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detalle_reserva, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        dialogView.findViewById<TextView>(R.id.textViewNombre).text = reserva.nombre
        val userEmail = auth.currentUser?.email ?: "-"
        dialogView.findViewById<TextView>(R.id.textViewEmail).text = userEmail
        dialogView.findViewById<TextView>(R.id.textViewFechas).text = "Llegada: ${dateFormat.format(reserva.fechaInicio)}\nSalida: ${dateFormat.format(reserva.fechaFin)}"
        dialogView.findViewById<TextView>(R.id.textViewEstado).text = reserva.estado
        dialogView.findViewById<TextView>(R.id.textViewAdultos).text = "${reserva.adultos}"
        dialogView.findViewById<TextView>(R.id.textViewNinos).text = "${reserva.ninos}"
        dialogView.findViewById<TextView>(R.id.textViewMascotas).text = "${reserva.mascotas}"
        dialogView.findViewById<TextView>(R.id.textViewVehiculo).text = reserva.vehiculoId
        dialogView.findViewById<TextView>(R.id.textViewTipoVehiculo).text = reserva.tipoVehiculo ?: "-"
        dialogView.findViewById<TextView>(R.id.textViewServicios).text = reserva.serviciosAdicionales.joinToString(", ")
        dialogView.findViewById<TextView>(R.id.textViewComentarios).text = reserva.comentarios
        dialogView.findViewById<TextView>(R.id.textViewPrecio).text = String.format("%.2f", reserva.precio)
        dialogView.findViewById<TextView>(R.id.textViewMoneda).text = "€"
        dialogView.findViewById<TextView>(R.id.textViewOrigen).text = if (reserva.origen == "app") "Reserva creada en la app" else "Reserva creada en la web"
        dialogView.findViewById<ImageView>(R.id.imageViewReservaOrigen).setImageResource(
            if (reserva.origen == "app") R.drawable.ic_app else R.drawable.ic_web
        )
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCerrar).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun mostrarDialogoDetallesVehiculo(vehiculo: Vehiculo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detalle_vehiculo, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.textViewMarca).text = vehiculo.marca
        dialogView.findViewById<TextView>(R.id.textViewModelo).text = vehiculo.modelo
        dialogView.findViewById<TextView>(R.id.textViewMatricula).text = vehiculo.matricula
        dialogView.findViewById<TextView>(R.id.textViewTipo).text = vehiculo.tipo
        dialogView.findViewById<TextView>(R.id.textViewLongitud).text = if (vehiculo.longitud.isNotEmpty()) vehiculo.longitud else "No especificada"
        dialogView.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.imageViewVehiculoIcon).setImageResource(R.drawable.ic_autocaravana)
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCerrarVehiculo).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // Utilidad para obtener todos los precios y la moneda de Firestore
    private fun obtenerPreciosYMoneda(callback: (precios: Map<String, Number>, preciosVehiculos: Map<String, Number>, preciosExtras: Map<String, Number>, moneda: String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val pricesRef = db.collection("prices")
        val precios = mutableMapOf<String, Number>()
        val preciosVehiculos = mutableMapOf<String, Number>()
        val preciosExtras = mutableMapOf<String, Number>()
        var moneda = "€"
        pricesRef.document("huespedes").get().addOnSuccessListener { docHuespedes ->
            docHuespedes.data?.forEach { precios[it.key] = it.value as Number }
            pricesRef.document("vehicles_base").get().addOnSuccessListener { docVehiculos ->
                docVehiculos.data?.forEach { preciosVehiculos[it.key] = it.value as Number }
                pricesRef.document("extra_services").get().addOnSuccessListener { docExtras ->
                    docExtras.data?.forEach { preciosExtras[it.key] = it.value as Number }
                    pricesRef.document("config").get().addOnSuccessListener { docConfig ->
                        moneda = docConfig.getString("moneda") ?: "€"
                        callback(precios, preciosVehiculos, preciosExtras, moneda)
                    }
                }
            }
        }
    }

    // Función para calcular el precio total
    private fun calcularPrecioReserva(
        adultos: Int,
        ninos: Int,
        mascotas: Int,
        noches: Int,
        tipoVehiculo: String,
        serviciosAdicionales: List<String>,
        precios: Map<String, Number>,
        preciosVehiculos: Map<String, Number>,
        preciosExtras: Map<String, Number>
    ): Int {
        var total = 0
        total += adultos * (precios["adultos"]?.toInt() ?: 0) * noches
        total += ninos * (precios["niños"]?.toInt() ?: 0) * noches
        total += mascotas * (precios["mascota"]?.toInt() ?: 0) * noches
        // Precio base vehículo SOLO una vez
        total += preciosVehiculos[tipoVehiculo] ?.toInt() ?: 0
        serviciosAdicionales.forEach { servicio ->
            when (servicio) {
                "late_ch_out" -> total += preciosExtras[servicio]?.toInt() ?: 0 // solo una vez
                else -> total += (preciosExtras[servicio]?.toInt() ?: 0) * noches
            }
        }
        return total
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 