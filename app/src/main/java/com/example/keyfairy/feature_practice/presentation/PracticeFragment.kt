package com.example.keyfairy.feature_practice.presentation

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.R
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.navigateWithBackStack
import kotlinx.coroutines.launch

class PracticeFragment : BaseFragment() {

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

        setupRecyclerView(view)
        setupViewModel()
        setupObservers()
        setupSearchFilter(view)

        loadScales()
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view_escalas)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        escalasAdapter = ScaleAdapter(emptyList()) { escalaSeleccionada ->
            safeNavigate {
                val fragment = SpeedAndDistanceFragment().apply {
                    arguments = Bundle().apply {
                        putString("escala_data", escalaSeleccionada)
                    }
                }
                navigateWithBackStack(fragment, R.id.fragment_container)
            }
        }
        recyclerView.adapter = escalasAdapter
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[PracticeViewModel::class.java]
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.escalas.collect { listaEscalas ->
                if (isFragmentActive) {
                    listaCompletaEscalas = listaEscalas
                    escalasAdapter.updateData(listaCompletaEscalas)
                    Log.d("PracticeFragment", "Escalas actualizadas: ${listaEscalas.size} elementos")
                }
            }
        }
    }

    private fun setupSearchFilter(view: View) {
        val filtroInput = view.findViewById<EditText>(R.id.editTextFiltro)
        filtroInput.addTextChangedListener { editable ->
            if (isFragmentActive) {
                val textoFiltro = editable.toString().trim()
                Log.d("PracticeFragment", "Filtro cambiado: $textoFiltro")
                escalasAdapter.filtrarPorNota(textoFiltro)
            }
        }
    }

    private fun loadScales() {
        if (isFragmentActive && ::viewModel.isInitialized) {
            Log.d("PracticeFragment", "Cargando escalas...")
            viewModel.cargarEscalas(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFragmentActive) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        if (listaCompletaEscalas.isEmpty()) {
            loadScales()
        }
    }

    override fun onPause() {
        super.onPause()
        // Limpiar el filtro si se navega fuera del fragment
        if (hasNavigatedAway) {
            view?.findViewById<EditText>(R.id.editTextFiltro)?.setText("")
        }
    }

    fun refreshData() {
        if (isFragmentActive && ::viewModel.isInitialized) {
            Log.d("PracticeFragment", "Refrescando datos de escalas...")
            viewModel.cargarEscalas(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar referencias
        if (::recyclerView.isInitialized) {
            recyclerView.adapter = null
        }
    }
}