package com.example.keyfairy.feature_practice.presentation

import android.os.Bundle
import android.util.Log
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
import com.example.keyfairy.feature_practice.presentation.PracticeViewModel
import android.widget.EditText
import androidx.core.widget.addTextChangedListener

class PracticeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var escalasAdapter: ScaleAdapter
    private lateinit var viewModel: PracticeViewModel

    private var listaCompletaEscalas = emptyList<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_practice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view_escalas)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        escalasAdapter = ScaleAdapter()
        recyclerView.adapter = escalasAdapter

        viewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[PracticeViewModel::class.java]

        lifecycleScope.launchWhenStarted {
            viewModel.escalas.collect { listaEscalas ->
                listaCompletaEscalas = listaEscalas
                escalasAdapter.updateData(listaCompletaEscalas)
            }
        }

        val filtroInput = view.findViewById<EditText>(R.id.editTextFiltro)
        filtroInput.addTextChangedListener { editable ->
            val textoFiltro = editable.toString().trim()
            Log.d("PracticeFragment", "Filtro cambiado: $textoFiltro")
            escalasAdapter.filtrarPorNota(textoFiltro)
        }
    }
}