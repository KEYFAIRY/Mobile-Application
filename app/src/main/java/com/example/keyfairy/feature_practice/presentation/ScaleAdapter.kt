package com.example.keyfairy.feature_practice.presentation

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.R

class ScaleAdapter(
    private var listaCompleta: List<String> = emptyList(),
    private val onPracticeClick: (String) -> Unit
) : RecyclerView.Adapter<ScaleAdapter.ScaleViewHolder>() {

    private var listaFiltrada = listaCompleta.toMutableList()

    class ScaleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val escalaCompleta: TextView = view.findViewById(R.id.text_nombre_escala)
        val notasTextView: TextView = view.findViewById(R.id.text_notas)
        val practiceButton: ImageView = view.findViewById(R.id.practice_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScaleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_escala, parent, false)
        return ScaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScaleViewHolder, position: Int) {
        val item = listaFiltrada[position]
        val partes = item.split(":")
        val nombre = partes.getOrNull(0)?.trim() ?: ""
        val notas = partes.getOrNull(1)?.trim() ?: ""

        holder.escalaCompleta.text = nombre
        holder.notasTextView.text = notas

        holder.practiceButton.setOnClickListener {
            onPracticeClick(item)
        }
    }

    override fun getItemCount() = listaFiltrada.size

    fun updateData(nuevosDatos: List<String>) {
        listaCompleta = nuevosDatos
        listaFiltrada = nuevosDatos.toMutableList()
        notifyDataSetChanged()
    }

    fun filtrarPorNota(nota: String) {
        Log.d("ScaleAdapter", "Filtrando por nombre de nota: $nota")

        listaFiltrada = if (nota.isBlank()) {
            listaCompleta.toMutableList()
        } else {
            listaCompleta.filter {
                val partes = it.split(":")
                val nombre = partes.getOrNull(0)?.trim() ?: ""
                nombre.contains(nota, ignoreCase = true)
            }.toMutableList()
        }

        notifyDataSetChanged()
    }
}