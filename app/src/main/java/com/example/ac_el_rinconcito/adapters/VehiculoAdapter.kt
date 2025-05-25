package com.example.ac_el_rinconcito.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ac_el_rinconcito.databinding.ItemVehiculoBinding
import com.example.ac_el_rinconcito.models.Vehiculo

class VehiculoAdapter(
    private var vehiculos: List<Vehiculo> = emptyList(),
    private val onVehiculoClick: (Vehiculo) -> Unit,
    private val onDeleteVehiculo: (Vehiculo) -> Unit
) : RecyclerView.Adapter<VehiculoAdapter.VehiculoViewHolder>() {

    class VehiculoViewHolder(private val binding: ItemVehiculoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(vehiculo: Vehiculo, onVehiculoClick: (Vehiculo) -> Unit, onDeleteVehiculo: (Vehiculo) -> Unit) {
            binding.textViewMarcaModelo.text = "${vehiculo.marca} ${vehiculo.modelo}"
            binding.textViewMatricula.text = vehiculo.matricula
            // Icono de origen
            val iconRes = when (vehiculo.origen) {
                "app" -> com.example.ac_el_rinconcito.R.drawable.ic_app
                else -> com.example.ac_el_rinconcito.R.drawable.ic_web
            }
            binding.imageViewOrigenVehiculo.setImageResource(iconRes)
            // Animación simple (escalado)
            binding.imageViewOrigenVehiculo.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction {
                binding.imageViewOrigenVehiculo.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }.start()
            // Tooltip
            val tooltip = if (vehiculo.origen == "app") "Vehículo añadido desde la app" else "Vehículo añadido desde la web"
            binding.imageViewOrigenVehiculo.contentDescription = tooltip
            binding.imageViewOrigenVehiculo.setOnLongClickListener {
                android.widget.Toast.makeText(binding.root.context, tooltip, android.widget.Toast.LENGTH_SHORT).show()
                true
            }
            binding.root.setOnClickListener { onVehiculoClick(vehiculo) }
            binding.buttonDeleteVehiculo.setOnClickListener { onDeleteVehiculo(vehiculo) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehiculoViewHolder {
        val binding = ItemVehiculoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VehiculoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehiculoViewHolder, position: Int) {
        holder.bind(vehiculos[position], onVehiculoClick, onDeleteVehiculo)
    }

    override fun getItemCount() = vehiculos.size

    fun updateVehiculos(newVehiculos: List<Vehiculo>) {
        vehiculos = newVehiculos
        notifyDataSetChanged()
    }
} 