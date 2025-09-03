package com.example.keyfairy.feature_reports.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.R

class ReportsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reports, container, false)

        // Referencias a los RecyclerView
        val recyclerFinalizadas: RecyclerView = view.findViewById(R.id.recycler_finalizadas)
        val recyclerSinRevisar: RecyclerView = view.findViewById(R.id.recycler_sin_revisar)
        val recyclerSinAnalizar: RecyclerView = view.findViewById(R.id.recycler_sin_analizar)

        // Adapter con datos dummy
        val adapter = ReportsAdapter()

        // Configuración de los RecyclerView
        recyclerFinalizadas.layoutManager = LinearLayoutManager(requireContext())
        recyclerFinalizadas.adapter = adapter

        recyclerSinRevisar.layoutManager = LinearLayoutManager(requireContext())
        recyclerSinRevisar.adapter = adapter

        recyclerSinAnalizar.layoutManager = LinearLayoutManager(requireContext())
        recyclerSinAnalizar.adapter = adapter

        return view
    }
}
