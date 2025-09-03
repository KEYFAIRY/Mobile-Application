package com.example.keyfairy.feature_reports.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.R

class ReportsAdapter : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    // Datos de prueba (dummy), sin modelo de datos
    private val nombres = listOf(
        "Práctica Escala Do Mayor",
        "Práctica Escala Re Menor",
        "Práctica Acordes Básicos",
        "Práctica Arpegios",
        "Práctica Técnica Avanzada"
    )

    private val notas = listOf(
        "Notas: Do, Re, Mi, Fa, Sol, La, Si",
        "Notas: Re, Mi, Fa, Sol, La, Sib, Do",
        "Notas: Do, Fa, Sol",
        "Notas: Do, Mi, Sol, Do",
        "Notas: Escala cromática completa"
    )

    private val fechas = listOf(
        "Fecha: 12 de mayo 5 pm",
        "Fecha: 15 de mayo 6 pm",
        "Fecha: 18 de mayo 4 pm",
        "Fecha: 20 de mayo 7 pm",
        "Fecha: 25 de mayo 3 pm"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_reporte, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.nombreEscala.text = nombres[position]
        holder.notas.text = notas[position]
        holder.fecha.text = fechas[position]

        // Imagen estática de momento
        holder.icon.setImageResource(R.drawable.pdf_blue)

        // Acción del botón
        holder.btnVer.setOnClickListener {
            // Por ahora solo un log o print de prueba
            println("Clic en Ver de: ${nombres[position]}")
        }
    }

    override fun getItemCount(): Int = nombres.size

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.practice_icon)
        val nombreEscala: TextView = itemView.findViewById(R.id.text_nombre_escala)
        val notas: TextView = itemView.findViewById(R.id.text_notas)
        val fecha: TextView = itemView.findViewById(R.id.text_fecha)
        val btnVer: Button = itemView.findViewById(R.id.button_ver_detalle)
    }
}
