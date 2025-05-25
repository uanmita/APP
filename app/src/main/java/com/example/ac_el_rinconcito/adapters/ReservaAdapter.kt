package com.example.ac_el_rinconcito.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ac_el_rinconcito.databinding.ItemReservaBinding
import com.example.ac_el_rinconcito.models.Reserva
import java.text.SimpleDateFormat
import java.util.Locale

class ReservaAdapter(
    private var reservas: List<Reserva> = emptyList(),
    private val onReservaClick: (Reserva) -> Unit
) : RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    class ReservaViewHolder(private val binding: ItemReservaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reserva: Reserva, dateFormat: SimpleDateFormat, onReservaClick: (Reserva) -> Unit) {
            binding.textViewFechas.text = "${dateFormat.format(reserva.fechaInicio)} - ${dateFormat.format(reserva.fechaFin)}"
            binding.textViewEstado.text = reserva.estado
            val precioTexto = if (reserva.precio > 0.0) "%.2f €".format(reserva.precio) else "No calculado"
            binding.textViewPrecio.text = precioTexto
            // Icono de origen
            val iconRes = when (reserva.origen) {
                "app" -> com.example.ac_el_rinconcito.R.drawable.ic_app
                else -> com.example.ac_el_rinconcito.R.drawable.ic_web
            }
            binding.imageViewOrigen.setImageResource(iconRes)
            // Animación simple (escalado)
            binding.imageViewOrigen.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction {
                binding.imageViewOrigen.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }.start()
            // Tooltip
            val tooltip = if (reserva.origen == "app") "Reserva creada en la app" else "Reserva creada en la web"
            binding.imageViewOrigen.contentDescription = tooltip
            binding.imageViewOrigen.setOnLongClickListener {
                android.widget.Toast.makeText(binding.root.context, tooltip, android.widget.Toast.LENGTH_SHORT).show()
                true
            }
            binding.root.setOnClickListener { onReservaClick(reserva) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val binding = ItemReservaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReservaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        holder.bind(reservas[position], dateFormat, onReservaClick)
    }

    override fun getItemCount() = reservas.size

    fun updateReservas(newReservas: List<Reserva>) {
        reservas = newReservas
        notifyDataSetChanged()
    }
} 