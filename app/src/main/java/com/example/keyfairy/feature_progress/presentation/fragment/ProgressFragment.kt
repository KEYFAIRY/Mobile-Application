package com.example.keyfairy.feature_progress.presentation.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentProgressBinding
import com.example.keyfairy.feature_progress.data.repository.ProgressRepositoryImpl
import com.example.keyfairy.feature_progress.domain.usecase.*
import com.example.keyfairy.feature_progress.presentation.state.GraphState
import com.example.keyfairy.feature_progress.presentation.viewModel.ProgressViewModel
import com.example.keyfairy.feature_progress.presentation.viewModel.ProgressViewModelFactory
import com.example.keyfairy.feature_reports.presentation.fragment.ReportsFragment
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.navigateWithBackStack
import com.example.keyfairy.utils.storage.SecureStorage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

class ProgressFragment : BaseFragment() {

    private var TAG: String = "ProgressFragment"
    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProgressViewModel

    // State for week navigation
    private lateinit var weekFields: WeekFields
    private var currentDate: LocalDate = LocalDate.now()
    private var currentYear: Int = currentDate.year
    private var currentWeek: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        setupViewModel()

        weekFields = WeekFields.of(Locale.getDefault())
        currentWeek = currentDate.get(weekFields.weekOfWeekBasedYear())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupObservers()
        updateWeekDisplay()
        loadDataForCurrentWeek()
    }

    private fun setupViewModel() {
        val progressRepository = ProgressRepositoryImpl()
        val topEscalasUC = GetTopEscalasSemanalesUseCase(progressRepository)
        val tiempoPosturasUC = GetTiempoPosturasSemanalesUseCase(progressRepository)
        val notasResumenUC = GetNotasResumenSemanalesUseCase(progressRepository)
        val erroresPosturalesUC = GetErroresPosturalesSemanalesUseCase(progressRepository)
        val erroresMusicalesUC = GetErroresMusicalesSemanalesUseCase(progressRepository)

        val factory = ProgressViewModelFactory(
            topEscalasUC,
            tiempoPosturasUC,
            notasResumenUC,
            erroresPosturalesUC,
            erroresMusicalesUC
        )

        viewModel = ViewModelProvider(this, factory)[ProgressViewModel::class.java]
    }

    private fun updateWeekDisplay() {
        // Calculate first and last date of current week
        val firstDayOfWeek = currentDate.with(weekFields.dayOfWeek(), 1)
        val lastDayOfWeek = currentDate.with(weekFields.dayOfWeek(), 7)
        val formatter = DateTimeFormatter.ofPattern("MMM dd", Locale("es", "ES"))
        val first = formatter.format(firstDayOfWeek)
        val last = formatter.format(lastDayOfWeek)
        binding.dateRangeTextView.text = "$first - $last"
    }

    private fun loadDataForCurrentWeek() {
        val idStudent = SecureStorage.getUid()
        Log.d(TAG, "Getting info for user ${idStudent}, year ${currentYear}, week ${currentWeek}")
        viewModel.loadTopEscalasGraph(idStudent, currentYear, currentWeek)
        viewModel.loadPosturasGraph(idStudent, currentYear, currentWeek)
        viewModel.loadNotasGraph(idStudent, currentYear, currentWeek)
        viewModel.loadErroresPosturalesSemana(idStudent, currentYear, currentWeek)
        viewModel.loadErroresMusicalesSemana(idStudent, currentYear, currentWeek)
    }

    private fun setupObservers() {
        // Escalas más practicadas
        viewModel.topEscalasGraph.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GraphState.Success -> {
                    binding.topEscalasImage.visibility = View.VISIBLE
                    binding.topEscalasImage.setImageBitmap(state.bitmap)
                }
                is GraphState.Error -> {
                    binding.topEscalasImage.visibility = View.INVISIBLE
                    Toast.makeText(requireContext(), "No hay datos de escalas más practicadas para esta semana.", Toast.LENGTH_SHORT).show()
                }
                is GraphState.Loading -> {
                    // Optionally show a loading spinner
                }
                else -> { }
            }
        }

        // Posturas
        viewModel.posturasGraph.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GraphState.Success -> {
                    binding.posturasImage.visibility = View.VISIBLE
                    binding.posturasImage.setImageBitmap(state.bitmap)
                }
                is GraphState.Error -> {
                    binding.posturasImage.visibility = View.INVISIBLE
                    Toast.makeText(requireContext(), "No hay datos de posturas para esta semana.", Toast.LENGTH_SHORT).show()
                }
                is GraphState.Loading -> { }
                else -> { }
            }
        }

        // Notas
        viewModel.notasGraph.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GraphState.Success -> {
                    binding.notasImage.visibility = View.VISIBLE
                    binding.notasImage.setImageBitmap(state.bitmap)
                }
                is GraphState.Error -> {
                    binding.notasImage.visibility = View.INVISIBLE
                    Toast.makeText(requireContext(), "No hay datos de notas para esta semana.", Toast.LENGTH_SHORT).show()
                }
                is GraphState.Loading -> { }
                else -> { }
            }
        }

        // Errores posturales
        viewModel.erroresPosturalesGraph.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GraphState.Success -> {
                    binding.erroresPosturalesImage.visibility = View.VISIBLE
                    binding.erroresPosturalesImage.setImageBitmap(state.bitmap)
                }
                is GraphState.Error -> {
                    binding.erroresPosturalesImage.visibility = View.INVISIBLE
                    Toast.makeText(requireContext(), "No hay datos de errores posturales para esta semana.", Toast.LENGTH_SHORT).show()
                }
                is GraphState.Loading -> { }
                else -> { }
            }
        }
        viewModel.currentPosturalScaleIndex.observe(viewLifecycleOwner) { index ->
            val scales = viewModel.posturalScales.value ?: return@observe
            val scaleName = scales.getOrNull(index) ?: return@observe
            binding.posturalScaleText.text = "Progreso en errores posturales: $scaleName"
        }

        // Errores musicales
        viewModel.erroresMusicalesGraph.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GraphState.Success -> {
                    binding.erroresMusicalesImage.visibility = View.VISIBLE
                    binding.erroresMusicalesImage.setImageBitmap(state.bitmap)
                }
                is GraphState.Error -> {
                    binding.erroresMusicalesImage.visibility = View.INVISIBLE
                    Toast.makeText(requireContext(), "No hay datos de errores musicales para esta semana.", Toast.LENGTH_SHORT).show()
                }
                is GraphState.Loading -> { }
                else -> { }
            }
        }
        viewModel.currentMusicalScaleIndex.observe(viewLifecycleOwner) { index ->
            val scales = viewModel.musicalScales.value ?: return@observe
            val scaleName = scales.getOrNull(index) ?: return@observe
            binding.musicalScaleText.text = "Progreso en errores musicales: $scaleName"
        }
    }

    private fun setupClickListeners() {
        // Semana anterior
        binding.previousWeekButton.setOnClickListener {
            currentDate = currentDate.minusWeeks(1)
            currentYear = currentDate.year
            currentWeek = currentDate.get(weekFields.weekOfWeekBasedYear())
            updateWeekDisplay()
            loadDataForCurrentWeek()
        }

        // Semana siguiente
        binding.nextWeekButton.setOnClickListener {
            currentDate = currentDate.plusWeeks(1)
            currentYear = currentDate.year
            currentWeek = currentDate.get(weekFields.weekOfWeekBasedYear())
            updateWeekDisplay()
            loadDataForCurrentWeek()
        }

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