package com.example.keyfairy.feature_reports.presentation.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.keyfairy.databinding.FragmentCompletedPracticeBinding
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.feature_reports.presentation.state.PosturalErrorsState
import com.example.keyfairy.feature_reports.presentation.state.PosturalErrorsUiEvent
import com.example.keyfairy.feature_reports.presentation.viewmodel.PosturalErrorsViewModel
import com.example.keyfairy.feature_reports.presentation.viewmodel.PosturalErrorsViewModelFactory
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.NavigationManager
import kotlinx.coroutines.launch

class CompletedPracticeFragment : BaseFragment() {

    companion object {
        private const val TAG = "CompletedPracticeFragment"
        private const val ARG_PRACTICE_ITEM = "practice_item"

        fun newInstance(practiceItem: Practice): CompletedPracticeFragment {
            return CompletedPracticeFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRACTICE_ITEM, practiceItem)
                }
            }
        }
    }
    private var _binding: FragmentCompletedPracticeBinding? = null
    private val binding get() = _binding!!

    private lateinit var practiceItem: Practice
    private lateinit var posturalErrorsViewModel: PosturalErrorsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompletedPracticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        extractArguments()
        setupViewModel()
        setupViews()
        loadData()
        observeViewModel()
    }

    private fun extractArguments() {
        practiceItem = arguments?.getParcelable(ARG_PRACTICE_ITEM)
            ?: throw IllegalArgumentException("PracticeItem is required")

        Log.d(TAG, "📋 Practice item received: ID=${practiceItem.practiceId}, Scale=${practiceItem.getScaleFullName()}")
    }

    private fun setupViewModel() {
        val factory = PosturalErrorsViewModelFactory(practiceItem.practiceId)
        posturalErrorsViewModel = ViewModelProvider(this, factory)[PosturalErrorsViewModel::class.java]
    }

    private fun setupViews() {
        setupClickListeners()
        Log.d(TAG, "✅ Views initialized successfully")
    }

    private fun loadData() {
        try {
            // Cargar información básica de la práctica
            loadPracticeInfo()

            // Cargar estado del PDF
            loadPdfState()

            // Cargar datos de errores musicales (mock por ahora)
            loadMusicalErrorsData()

            Log.d(TAG, "✅ Data loaded successfully for practice ${practiceItem.practiceId}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading data: ${e.message}", e)
            showError("Error al cargar los datos de la práctica")
        }
    }

    private fun observeViewModel() {
        // Observar estado de errores posturales
        viewLifecycleOwner.lifecycleScope.launch {
            posturalErrorsViewModel.uiState.collect { state ->
                handlePosturalErrorsState(state)
            }
        }

        // Observar eventos
        viewLifecycleOwner.lifecycleScope.launch {
            posturalErrorsViewModel.uiEvent.collect { event ->
                handleUiEvent(event)
            }
        }
    }

    private fun handlePosturalErrorsState(state: PosturalErrorsState) {
        when (state) {
            is PosturalErrorsState.Initial -> {
                Log.d(TAG, "Initial state")
            }
            is PosturalErrorsState.Loading -> {
                Log.d(TAG, "Loading postural errors...")
                binding.numPosturalErrors.text = "..."
            }
            is PosturalErrorsState.Success -> {
                Log.d(TAG, "✅ Postural errors loaded: ${state.numErrors}")
                binding.numPosturalErrors.text = state.numErrors.toString()
                binding.btnViewPosturalErrors.isEnabled = state.numErrors > 0
            }
            is PosturalErrorsState.Error -> {
                Log.e(TAG, "❌ Error loading postural errors: ${state.message}")
                binding.numPosturalErrors.text = "0"
                binding.btnViewPosturalErrors.isEnabled = false
            }
        }
    }

    private fun handleUiEvent(event: PosturalErrorsUiEvent) {
        when (event) {
            is PosturalErrorsUiEvent.ShowError -> {
                showError(event.message)
            }
            is PosturalErrorsUiEvent.NavigateBack -> {
                handleNavigateBack()
            }
        }
    }

    private fun handleNavigateBack() {
        if (isFragmentActive) {
            safeNavigate {
                val didGoBack = NavigationManager.goBack(parentFragmentManager)

                if (!didGoBack) {
                    Log.d(TAG, "No fragments in back stack, finishing activity")
                    requireActivity().finish()
                } else {
                    Log.d(TAG, "Navigated back successfully")
                }
            }
        }
    }

    private fun loadPracticeInfo() {
        with(binding) {
            date.text = practiceItem.date
            hour.text = practiceItem.time
            bpm.text = practiceItem.bpm.toString()
            figure.text = practiceItem.figure
            octaves.text = practiceItem.octaves.toString()
        }

        Log.d(TAG, "📋 Practice info loaded: ${practiceItem.getScaleFullName()}")
    }

    private fun loadPdfState() {
        val hasPdf = !practiceItem.pdfUrl.isNullOrEmpty()

        with(binding) {
            if (hasPdf) {
                statePdf.text = "Informe listo"
                btnDownloadPdf.text = "Descargar"
                btnDownloadPdf.isEnabled = true
                pdfImg.alpha = 1.0f
                Log.d(TAG, "📄 PDF available: ${practiceItem.pdfUrl}")
            } else {
                statePdf.text = "Informe en progreso"
                btnDownloadPdf.text = "No disponible"
                btnDownloadPdf.isEnabled = false
                pdfImg.alpha = 0.5f
                Log.d(TAG, "📄 PDF not available yet")
            }
        }
    }

    private fun loadMusicalErrorsData() {
        val musicalErrorsCount = generateMockMusicalErrors()
        binding.numMusicalErrors.text = musicalErrorsCount.toString()

        Log.d(TAG, "🎵 Musical errors loaded: $musicalErrorsCount")
    }

    private fun setupClickListeners() {
        with(binding) {
            // ✅ Configurar listeners directamente con binding
            btnDownloadPdf.setOnClickListener {
                if (isFragmentActive) {
                    safeNavigate {
                        downloadPdf()
                    }
                }
            }

            btnViewPosturalErrors.setOnClickListener {
                if (isFragmentActive) {
                    safeNavigate {
                        viewPosturalErrors()
                    }
                }
            }

            btnViewMusicalErrors.setOnClickListener {
                if (isFragmentActive) {
                    safeNavigate {
                        viewMusicalErrors()
                    }
                }
            }
        }
    }

    private fun downloadPdf() {
        val pdfUrl = practiceItem.pdfUrl

        if (pdfUrl.isNullOrEmpty()) {
            showError("El reporte PDF no está disponible aún")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(pdfUrl)
                type = "application/pdf"
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
                Log.d(TAG, "📄 Opening PDF: $pdfUrl")
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                startActivity(browserIntent)
                Log.d(TAG, "🌐 Opening PDF in browser: $pdfUrl")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening PDF: ${e.message}", e)
            showError("Error al abrir el reporte PDF")
        }
    }

    private fun viewPosturalErrors() {
        Log.d(TAG, "👤 Viewing postural errors for practice ${practiceItem.practiceId}")

        Toast.makeText(
            requireContext(),
            "Ver errores posturales de la práctica #${practiceItem.practiceId}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun viewMusicalErrors() {
        Log.d(TAG, "🎵 Viewing musical errors for practice ${practiceItem.practiceId}")

        Toast.makeText(
            requireContext(),
            "Ver errores musicales de la práctica #${practiceItem.practiceId}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showError(message: String) {
        if (isFragmentActive) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun generateMockMusicalErrors(): Int {
        return (practiceItem.practiceId % 8) + 2
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 Fragment resumed for practice ${practiceItem.practiceId}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "🗑️ Fragment destroyed for practice ${practiceItem.practiceId}")
    }
}