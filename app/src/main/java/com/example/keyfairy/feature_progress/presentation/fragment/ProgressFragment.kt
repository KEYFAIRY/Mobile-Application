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
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ImageButton

class ProgressFragment : BaseFragment() {

    private val TAG = "ProgressFragment"
    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProgressViewModel
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

    private fun showZoomDialog(bitmap: Bitmap) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_zoom_image, null)

        val imageView = dialogView.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id.zoomImageView)
        imageView.setImageBitmap(bitmap)

        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)

        val closeButton = dialogView.findViewById<ImageButton>(R.id.closeButton)
        closeButton.setOnClickListener { dialog.dismiss() }

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        dialogView.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }


    private fun setupViewModel() {
        val repository = ProgressRepositoryImpl()
        val factory = ProgressViewModelFactory(
            GetTopEscalasSemanalesUseCase(repository),
            GetTiempoPosturasSemanalesUseCase(repository),
            GetNotasResumenSemanalesUseCase(repository),
            GetErroresPosturalesSemanalesUseCase(repository),
            GetErroresMusicalesSemanalesUseCase(repository)
        )
        viewModel = ViewModelProvider(this, factory)[ProgressViewModel::class.java]
    }

    private fun updateWeekDisplay() {
        val firstDay = currentDate.with(weekFields.dayOfWeek(), 1)
        val lastDay = currentDate.with(weekFields.dayOfWeek(), 7)
        val formatter = DateTimeFormatter.ofPattern("MMM dd", Locale("es", "ES"))
        binding.dateRangeTextView.text = "${formatter.format(firstDay)} - ${formatter.format(lastDay)}"
    }

    private fun loadDataForCurrentWeek() {
        val idStudent = SecureStorage.getUid()
        Log.d(TAG, "📅 Cargando datos usuario=$idStudent año=$currentYear semana=$currentWeek")

        resetAllGraphs()

        viewModel.clearGraphStates()

        viewModel.loadTopEscalasGraph(idStudent, currentYear, currentWeek)
        viewModel.loadPosturasGraph(idStudent, currentYear, currentWeek)
        viewModel.loadNotasGraph(idStudent, currentYear, currentWeek)
        viewModel.loadErroresPosturalesSemana(idStudent, currentYear, currentWeek)
        viewModel.loadErroresMusicalesSemana(idStudent, currentYear, currentWeek)
    }

    private fun resetAllGraphs() {
        with(binding) {
            topEscalasImage.setImageDrawable(null)
            posturasImage.setImageDrawable(null)
            notasImage.setImageDrawable(null)
            erroresPosturalesImage.setImageDrawable(null)
            erroresMusicalesImage.setImageDrawable(null)

            topEscalasImage.visibility = View.INVISIBLE
            posturasImage.visibility = View.INVISIBLE
            notasImage.visibility = View.INVISIBLE
            erroresPosturalesImage.visibility = View.INVISIBLE
            erroresMusicalesImage.visibility = View.INVISIBLE

        }
    }

    private fun setupObservers() {
        fun isFragmentActive(): Boolean = isAdded && _binding != null

        viewModel.topEscalasGraph.observe(viewLifecycleOwner) { state ->
            if (!isFragmentActive()) return@observe
            when (state) {
                is GraphState.Success -> {
                    binding.topEscalasImage.visibility = View.VISIBLE
                    binding.topEscalasImage.setImageBitmap(state.bitmap)
                }
                is GraphState.Error -> {
                    binding.topEscalasImage.setImageDrawable(null)
                    binding.topEscalasImage.visibility = View.INVISIBLE
                    Toast.makeText(requireContext(), "No hay datos de escalas más practicadas.", Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }

        viewModel.posturasGraph.observe(viewLifecycleOwner) { state ->
            if (!isFragmentActive()) return@observe
            when (state) {
                is GraphState.Success -> {
                    binding.posturasImage.visibility = View.VISIBLE
                    binding.posturasImage.setImageBitmap(state.bitmap)
                }
                is GraphState.Error -> {
                    binding.posturasImage.setImageDrawable(null)
                    binding.posturasImage.visibility = View.INVISIBLE
                    Toast.makeText(requireContext(), "No hay datos de posturas.", Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }

        viewModel.notasGraph.observe(viewLifecycleOwner) { state ->
            if (!isFragmentActive()) return@observe
            when (state) {
                is GraphState.Success -> {
                    binding.notasImage.visibility = View.VISIBLE
                    binding.notasImage.setImageBitmap(state.bitmap)
                }
                is GraphState.Error -> {
                    binding.notasImage.setImageDrawable(null)
                    binding.notasImage.visibility = View.INVISIBLE
                    Toast.makeText(requireContext(), "No hay datos de notas.", Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }

        viewModel.erroresPosturalesGraph.observe(viewLifecycleOwner) { state ->
            if (!isFragmentActive()) return@observe
            when (state) {
                is GraphState.Success -> {
                    binding.erroresPosturalesImage.visibility = View.VISIBLE
                    binding.erroresPosturalesImage.setImageBitmap(state.bitmap)
                }
                is GraphState.Error -> {
                    binding.erroresPosturalesImage.setImageDrawable(null)
                    binding.erroresPosturalesImage.visibility = View.INVISIBLE
                    binding.posturalScaleText.text = "Progreso en errores posturales:"
                    Toast.makeText(requireContext(), "No hay errores posturales esta semana.", Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }

        viewModel.currentPosturalScaleIndex.observe(viewLifecycleOwner) { index ->
            if (!isFragmentActive()) return@observe
            val scales = viewModel.posturalScales.value ?: return@observe
            val scaleName = scales.getOrNull(index) ?: return@observe
            binding.posturalScaleText.text = "Errores posturales: $scaleName"
        }

        viewModel.erroresMusicalesGraph.observe(viewLifecycleOwner) { state ->
            if (!isFragmentActive()) return@observe
            when (state) {
                is GraphState.Success -> {
                    binding.erroresMusicalesImage.visibility = View.VISIBLE
                    binding.erroresMusicalesImage.setImageBitmap(state.bitmap)
                }
                is GraphState.Error -> {
                    binding.erroresMusicalesImage.setImageDrawable(null)
                    binding.erroresMusicalesImage.visibility = View.INVISIBLE
                    binding.musicalScaleText.text = "Progreso en errores musicales:"
                    Toast.makeText(requireContext(), "No hay errores musicales esta semana.", Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }

        viewModel.currentMusicalScaleIndex.observe(viewLifecycleOwner) { index ->
            if (!isFragmentActive()) return@observe
            val scales = viewModel.musicalScales.value ?: return@observe
            val scaleName = scales.getOrNull(index) ?: return@observe
            binding.musicalScaleText.text = "Errores musicales: $scaleName"
        }

        viewModel.posturalScales.observe(viewLifecycleOwner) { scales ->
            val hasMultiple = scales.size > 1
            binding.previousPosturalScaleButton.visibility = if (hasMultiple) View.VISIBLE else View.INVISIBLE
            binding.nextPosturalScaleButton.visibility = if (hasMultiple) View.VISIBLE else View.INVISIBLE
            Log.d(TAG, "Postural scales: ${scales.size} → botones ${if (hasMultiple) "habilitados" else "inhabilitados"}")
        }


        viewModel.musicalScales.observe(viewLifecycleOwner) { scales ->
            val hasMultiple = scales.size > 1
            binding.previousMusicalScaleButton.visibility = if (hasMultiple) View.VISIBLE else View.INVISIBLE
            binding.nextMusicalScaleButton.visibility = if (hasMultiple) View.VISIBLE else View.INVISIBLE
            Log.d(TAG, "Musical scales: ${scales.size} → botones ${if (hasMultiple) "habilitados" else "inhabilitados"}")
        }
    }

    private fun setupClickListeners() {
        binding.previousWeekButton.setOnClickListener {
            currentDate = currentDate.minusWeeks(1)
            currentYear = currentDate.year
            currentWeek = currentDate.get(weekFields.weekOfWeekBasedYear())
            updateWeekDisplay()
            loadDataForCurrentWeek()
        }

        binding.nextWeekButton.setOnClickListener {
            currentDate = currentDate.plusWeeks(1)
            currentYear = currentDate.year
            currentWeek = currentDate.get(weekFields.weekOfWeekBasedYear())
            updateWeekDisplay()
            loadDataForCurrentWeek()
        }

        binding.reportsButton.setOnClickListener {
            safeNavigate {
                navigateWithBackStack(ReportsFragment(), R.id.fragment_container)
            }
        }

        binding.previousPosturalScaleButton.setOnClickListener { viewModel.previousPosturalScale() }
        binding.nextPosturalScaleButton.setOnClickListener { viewModel.nextPosturalScale() }
        binding.previousMusicalScaleButton.setOnClickListener { viewModel.previousMusicalScale() }
        binding.nextMusicalScaleButton.setOnClickListener { viewModel.nextMusicalScale() }

        binding.topEscalasImage.setOnClickListener {
            val drawable = binding.topEscalasImage.drawable
            if (drawable is BitmapDrawable) {
                showZoomDialog(drawable.bitmap)
            } else {
                Toast.makeText(requireContext(), "Imagen no disponible para expandir", Toast.LENGTH_SHORT).show()
            }
        }

        binding.posturasImage.setOnClickListener {
            val drawable = binding.posturasImage.drawable
            if (drawable is BitmapDrawable) {
                showZoomDialog(drawable.bitmap)
            } else {
                Toast.makeText(requireContext(), "Imagen no disponible para expandir", Toast.LENGTH_SHORT).show()
            }
        }

        binding.notasImage.setOnClickListener {
            val drawable = binding.notasImage.drawable
            if (drawable is BitmapDrawable) {
                showZoomDialog(drawable.bitmap)
            } else {
                Toast.makeText(requireContext(), "Imagen no disponible para expandir", Toast.LENGTH_SHORT).show()
            }
        }

        binding.erroresPosturalesImage.setOnClickListener {
            val drawable = binding.erroresPosturalesImage.drawable
            if (drawable is BitmapDrawable) {
                showZoomDialog(drawable.bitmap)
            } else {
                Toast.makeText(requireContext(), "Imagen no disponible para expandir", Toast.LENGTH_SHORT).show()
            }
        }

        binding.erroresMusicalesImage.setOnClickListener {
            val drawable = binding.erroresMusicalesImage.drawable
            if (drawable is BitmapDrawable) {
                showZoomDialog(drawable.bitmap)
            } else {
                Toast.makeText(requireContext(), "Imagen no disponible para expandir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resetAllGraphs()
        _binding = null
    }
}
