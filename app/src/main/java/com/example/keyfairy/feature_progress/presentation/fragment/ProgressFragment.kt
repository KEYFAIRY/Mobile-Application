package com.example.keyfairy.feature_progress.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentProgressBinding
import com.example.keyfairy.feature_progress.presentation.viewModel.ProgressViewModel
import com.example.keyfairy.feature_progress.presentation.state.GraphState
import com.example.keyfairy.feature_reports.presentation.fragment.ReportsFragment
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.navigateWithBackStack
import com.example.keyfairy.utils.storage.SecureStorage
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

class ProgressFragment : BaseFragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupObservers()
        loadData()
    }

    private fun loadData() {
        val idStudent = SecureStorage.getUid()
        val practiceDateTime = LocalDateTime.now()
        val anio = practiceDateTime.year
        val weekFields = WeekFields.of(Locale.getDefault()) // o Locale("es", "ES") para español
        val semana = practiceDateTime.get(weekFields.weekOfWeekBasedYear())


        viewModel.loadTopEscalasGraph(idStudent, anio, semana)
        viewModel.loadPosturasGraph(idStudent, anio, semana)
        viewModel.loadNotasGraph(idStudent, anio, semana)
        viewModel.loadErroresPosturalesSemana(idStudent, anio, semana)
        viewModel.loadErroresMusicalesSemana(idStudent, anio, semana)
    }

    private fun setupObservers() {
        // Escalas más practicadas
        viewModel.topEscalasGraph.observe(viewLifecycleOwner) { state ->
            when(state) {
                is GraphState.Success -> binding.topEscalasImage.setImageBitmap(state.bitmap)
                else -> {}
            }
        }

        // Posturas
        viewModel.posturasGraph.observe(viewLifecycleOwner) { state ->
            when(state) {
                is GraphState.Success -> binding.posturasImage.setImageBitmap(state.bitmap)
                else -> {}
            }
        }

        // Notas
        viewModel.notasGraph.observe(viewLifecycleOwner) { state ->
            when(state) {
                is GraphState.Success -> binding.notasImage.setImageBitmap(state.bitmap)
                else -> {}
            }
        }

        // Errores posturales
        viewModel.erroresPosturalesGraph.observe(viewLifecycleOwner) { state ->
            when(state) {
                is GraphState.Success -> binding.erroresPosturalesImage.setImageBitmap(state.bitmap)
                else -> {}
            }
        }
        viewModel.currentPosturalScaleIndex.observe(viewLifecycleOwner) { index ->
            val scales = viewModel.posturalScales.value ?: return@observe
            val scaleName = scales.getOrNull(index) ?: return@observe
            binding.posturalScaleText.text = "Progreso en errores posturales: $scaleName"
        }

        // Errores musicales
        viewModel.erroresMusicalesGraph.observe(viewLifecycleOwner) { state ->
            when(state) {
                is GraphState.Success -> binding.erroresMusicalesImage.setImageBitmap(state.bitmap)
                else -> {}
            }
        }
        viewModel.currentMusicalScaleIndex.observe(viewLifecycleOwner) { index ->
            val scales = viewModel.musicalScales.value ?: return@observe
            val scaleName = scales.getOrNull(index) ?: return@observe
            binding.musicalScaleText.text = "Progreso en errores musicales: $scaleName"
        }
    }

    private fun setupClickListeners() {
        // Navegación informes
        binding.reportsButton.setOnClickListener {
            safeNavigate {
                navigateWithBackStack(ReportsFragment(), R.id.fragment_container)
            }
        }

        // Escalas posturales
        binding.previousPosturalScaleButton.setOnClickListener {
            viewModel.previousPosturalScale()
        }
        binding.nextPosturalScaleButton.setOnClickListener {
            viewModel.nextPosturalScale()
        }

        // Escalas musicales
        binding.previousMusicalScaleButton.setOnClickListener {
            viewModel.previousMusicalScale()
        }
        binding.nextMusicalScaleButton.setOnClickListener {
            viewModel.nextMusicalScale()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}