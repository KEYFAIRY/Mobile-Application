package com.example.keyfairy.feature_practice.domain.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.R

class ScaleAdapter(private val escalas: List<String>) : RecyclerView.Adapter<ScaleAdapter.ScaleViewHolder>() {

    class ScaleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreEscala: TextView = view.findViewById(R.id.text_nombre_escala)
        val notasEscala: TextView = view.findViewById(R.id.text_notas)
        val fecha: TextView = view.findViewById(R.id.text_fecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScaleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_escala, parent, false)
        return ScaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScaleViewHolder, position: Int) {
        val item = escalas[position]
        val partes = item.split(":", limit = 2)

        if (partes.size == 2) {
            holder.nombreEscala.text = partes[0].trim()
            holder.notasEscala.text = "Notas: ${partes[1].trim()}"
        } else {
            holder.nombreEscala.text = item
            holder.notasEscala.text = "Notas: N/A"
        }

        // Fecha fija por ahora
        holder.fecha.text = "Fecha: Aun no las has practicado"
    }

    override fun getItemCount() = escalas.size
}