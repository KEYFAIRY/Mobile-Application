package com.example.keyfairy.feature_practice.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.R
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.keyfairy.feature_practice.domain.model.ScaleAdapter

class PracticeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var escalasAdapter: ScaleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_practice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar el RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_escalas)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Llamar a la función para obtener las escalas y configurar el RecyclerView
        lifecycleScope.launchWhenStarted {
            val escalas = obtenerEscalasDesdePython()
            setupRecyclerView(escalas)
        }
    }


    private suspend fun obtenerEscalasDesdePython(): List<String> = withContext(Dispatchers.IO) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(requireContext()))
        }
        val py = Python.getInstance()
        try {
            // Nota: Aquí asumo que tu módulo se llama "escalas.py"
            val pyModule = py.getModule("escalas")
            val resultadoPy = pyModule.callAttr("obtener_escalas")
            resultadoPy.asList().map { it.toString() }
        } catch (e: PyException) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun setupRecyclerView(escalas: List<String>) {
        escalasAdapter = ScaleAdapter(escalas)
        recyclerView.adapter = escalasAdapter
    }
}