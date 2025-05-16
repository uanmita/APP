package com.example.ac_el_rinconcito.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ac_el_rinconcito.databinding.ItemVehiculoBinding
import com.example.ac_el_rinconcito.models.Vehiculo

class VehiculoAdapter(
    private var vehiculos: List<Vehiculo> = emptyList(),
    private val onVehiculoClick: (Vehiculo) -> Unit
) : RecyclerView.Adapter<VehiculoAdapter.VehiculoViewHolder>() {

    class VehiculoViewHolder(private val binding: ItemVehiculoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(vehiculo: Vehiculo, onVehiculoClick: (Vehiculo) -> Unit) {
            binding.textViewMarcaModelo.text = "${vehiculo.marca} ${vehiculo.modelo}"
            binding.textViewMatricula.text = vehiculo.matricula
            binding.root.setOnClickListener { onVehiculoClick(vehiculo) }
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
        holder.bind(vehiculos[position], onVehiculoClick)
    }

    override fun getItemCount() = vehiculos.size

    fun updateVehiculos(newVehiculos: List<Vehiculo>) {
        vehiculos = newVehiculos
        notifyDataSetChanged()
    }
} 