package com.example.ac_el_rinconcito.models

import java.util.Date

data class Reserva(
    val id: String = "",
    val userId: String = "",
    val vehiculoId: String = "",
    val fechaInicio: Date = Date(),
    val fechaFin: Date = Date(),
    val estado: String = "PENDIENTE", // PENDIENTE, CONFIRMADA, CANCELADA
    val precio: Double = 0.0,
    val nombre: String = "",
    val comentarios: String = "",
    val adultos: Int = 0,
    val ninos: Int = 0,
    val mascotas: Int = 0,
    val serviciosAdicionales: List<String> = emptyList(),
    val origen: String = "",
    val tipoVehiculo: String = ""
) 