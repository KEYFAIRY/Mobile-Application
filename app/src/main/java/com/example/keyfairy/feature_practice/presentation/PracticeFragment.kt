package com.example.keyfairy.feature_practice.presentation

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.keyfairy.R
import com.example.keyfairy.feature_home.presentation.HomeActivity
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.SharedScalesViewModel
import com.example.keyfairy.utils.common.navigateWithBackStack
import kotlinx.coroutines.launch

class PracticeFragment : BaseFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var escalasAdapter: ScaleAdapter
    private lateinit var sharedViewModel: SharedScalesViewModel

    private var listaCompletaEscalas = emptyList<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_practice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupSharedViewModel()
        setupObservers()
        setupSearchFilter(view)
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

    private fun setupSharedViewModel() {
        sharedViewModel = (activity as HomeActivity).getSharedScalesViewModel()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.escalas.collect { listaEscalas ->
                if (isFragmentActive) {
                    listaCompletaEscalas = listaEscalas
                    escalasAdapter.updateData(listaCompletaEscalas)
                    Log.d("PracticeFragment", "Escalas actualizadas desde SharedViewModel: ${listaEscalas.size} elementos")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.isLoading.collect { isLoading ->
                if (isFragmentActive) {
                    Log.d("PracticeFragment", "Estado de carga: $isLoading")
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

    override fun onResume() {
        super.onResume()
        if (isFragmentActive) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        refreshAdapterIfNeeded()
    }

    private fun refreshAdapterIfNeeded() {
        if (isFragmentActive && ::escalasAdapter.isInitialized) {
            // Obtener las escalas actuales del ViewModel compartido
            val currentEscalas = sharedViewModel.escalas.value

            Log.d("PracticeFragment", "refreshAdapterIfNeeded - Escalas en ViewModel: ${currentEscalas.size}")
            Log.d("PracticeFragment", "refreshAdapterIfNeeded - Escalas en adapter: ${escalasAdapter.itemCount}")

            if (currentEscalas.isNotEmpty() && escalasAdapter.itemCount == 0) {
                // Si hay escalas en el ViewModel pero el adapter está vacío, actualizar
                Log.d("PracticeFragment", "Actualizando adapter con escalas existentes")
                listaCompletaEscalas = currentEscalas
                escalasAdapter.updateData(listaCompletaEscalas)
            } else if (currentEscalas.isNotEmpty()) {
                // Si ambos tienen datos, asegurar que estén sincronizados
                Log.d("PracticeFragment", "Sincronizando adapter con ViewModel")
                listaCompletaEscalas = currentEscalas
                escalasAdapter.updateData(listaCompletaEscalas)
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && isFragmentActive) {
            Log.d("PracticeFragment", "Fragment visible de nuevo, refrescando adapter")
            refreshAdapterIfNeeded()
        }
    }

    override fun onPause() {
        super.onPause()
        if (hasNavigatedAway) {
            view?.findViewById<EditText>(R.id.editTextFiltro)?.setText("")
        }
    }

    fun refreshData() {
        if (isFragmentActive) {
            Log.d("PracticeFragment", "Forzando recarga de escalas...")
            sharedViewModel.forceReloadEscalas(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::recyclerView.isInitialized) {
            recyclerView.adapter = null
        }
    }
}