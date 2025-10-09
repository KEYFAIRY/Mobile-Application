package com.example.keyfairy.feature_reports.presentation.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentCompletedPracticeBinding
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.feature_reports.presentation.state.DownloadReportEvent
import com.example.keyfairy.feature_reports.presentation.state.DownloadReportState
import com.example.keyfairy.feature_reports.presentation.state.MusicalErrorsEvent
import com.example.keyfairy.feature_reports.presentation.state.MusicalErrorsState
import com.example.keyfairy.feature_reports.presentation.state.PosturalErrorsState
import com.example.keyfairy.feature_reports.presentation.state.PosturalErrorsEvent
import com.example.keyfairy.feature_reports.presentation.viewmodel.PracticeErrorsViewModel
import com.example.keyfairy.feature_reports.presentation.viewmodel.PracticeErrorsViewModelFactory
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.NavigationManager
import com.example.keyfairy.utils.storage.SecureStorage
import kotlinx.coroutines.launch
import java.io.File

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
    private lateinit var practiceErrorsViewModel: PracticeErrorsViewModel

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
        val factory = PracticeErrorsViewModelFactory(practiceItem.practiceId)
        practiceErrorsViewModel = ViewModelProvider(
            requireActivity(),
            factory
        )[PracticeErrorsViewModel::class.java]

        Log.d(TAG, "✅ ViewModel setup with Activity scope")
    }

    private fun setupViews() {
        setupClickListeners()
        Log.d(TAG, "✅ Views initialized successfully")
    }

    private fun loadData() {
        try {
            loadPracticeInfo()
            loadPdfState()

            Log.d(TAG, "✅ Data loaded successfully for practice ${practiceItem.practiceId}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading data: ${e.message}", e)
            showError("Error al cargar los datos de la práctica")
        }
    }

    private fun observeViewModel() {
        // PosturalErrors
        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.posturalErrorsState.collect { state ->
                handlePosturalErrorsState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.posturalErrorsEvent.collect { event ->
                handlePosturalErrorEvent(event)
            }
        }

        // Musical Errors
        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.musicalErrorsState.collect { state ->
                handleMusicalErrorsState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.musicalErrorsEvent .collect { event ->
                handleMusicalErrorEvent(event)
            }
        }

        // PDF
        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.downloadState.collect { state ->
                handleDownloadState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.downloadEvent.collect { event ->
                handleDownloadEvent(event)
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
                binding.btnViewPosturalErrors.isEnabled = false
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

    private fun handlePosturalErrorEvent(event: PosturalErrorsEvent) {
        when (event) {
            is PosturalErrorsEvent.ShowError -> {
                showError(event.message)
            }
            is PosturalErrorsEvent.NavigateBack -> {
                handleNavigateBack()
            }
        }
    }

    private fun handleMusicalErrorsState(state: MusicalErrorsState) {
        when (state) {
            is MusicalErrorsState.Initial -> {
                Log.d(TAG, "Initial state")
            }
            is MusicalErrorsState.Loading -> {
                Log.d(TAG, "Loading musical errors...")
                binding.numMusicalErrors.text = "..."
                binding.btnViewMusicalErrors.isEnabled = false
            }
            is MusicalErrorsState.Success -> {
                Log.d(TAG, "✅ Musical errors loaded: ${state.numErrors}")
                binding.numMusicalErrors.text = state.numErrors.toString()
                binding.btnViewMusicalErrors.isEnabled = state.numErrors > 0
            }
            is MusicalErrorsState.Error -> {
                Log.e(TAG, "❌ Error loading musical errors: ${state.message}")
                binding.numMusicalErrors.text = "0"
                binding.btnViewMusicalErrors.isEnabled = false
            }
        }
    }

    private fun handleMusicalErrorEvent(event: MusicalErrorsEvent) {
        when (event) {
            is MusicalErrorsEvent.ShowError -> {
                showError(event.message)
            }
            is MusicalErrorsEvent.NavigateBack -> {
                handleNavigateBack()
            }
        }
    }
    private fun handleDownloadState(state: DownloadReportState) {
        when (state) {
            is DownloadReportState.Idle -> {
                updateButtonState()
            }
            is DownloadReportState.Downloading -> {
                binding.btnDownloadPdf.isEnabled = false
                binding.btnDownloadPdf.text = "Descargando..."
                Log.d(TAG, "📥 Downloading report...")
            }
            is DownloadReportState.Success -> {
                binding.btnDownloadPdf.isEnabled = true
                updateButtonState()
                Log.d(TAG, "✅ Download completed")
            }
            is DownloadReportState.Error -> {
                binding.btnDownloadPdf.isEnabled = true
                updateButtonState()
                Log.e(TAG, "❌ Download error: ${state.message}")
            }
        }
    }

    private fun handleDownloadEvent(event: DownloadReportEvent) {
        when (event) {
            is DownloadReportEvent.ShowError -> {
                showError(event.message)
            }
            is DownloadReportEvent.OpenPdf -> {
                if (isValidPdfFile(event.file)) {
                    showSuccess("Reporte descargado exitosamente")
                    openPdfFile(event.file)
                    updateButtonState()
                } else {
                    Log.e(TAG, "❌ Downloaded PDF is invalid")
                    showError("El archivo descargado está corrupto")
                    try {
                        event.file.delete()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting corrupted file: ${e.message}")
                    }
                }
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
        val practiceWithNoErrors = practiceItem.pdfUrl.equals("None")

        with(binding) {
            if (hasPdf && !practiceWithNoErrors) {
                updateButtonState()
                pdfImg.alpha = 1.0f
                Log.d(TAG, "📄 PDF available: ${practiceItem.pdfUrl}")
            } else if (practiceWithNoErrors) {
                statePdf.text = "¡Felicidades!"
                btnDownloadPdf.text = "Sin errores"
                btnDownloadPdf.isEnabled = false
                pdfImg.alpha = 0.7f
                Log.d(TAG, "🎉 Practice completed without errors")
            } else {
                statePdf.text = "Informe en progreso"
                btnDownloadPdf.text = "No disponible"
                btnDownloadPdf.isEnabled = false
                pdfImg.alpha = 0.5f
                Log.d(TAG, "📄 PDF not available yet")
            }
        }
    }

    private fun updateButtonState() {
        val localPdfFile = getLocalPdfFile()
        val hasLocalPdf = pdfFileExists(localPdfFile)

        with(binding) {
            if (hasLocalPdf) {
                statePdf.text = "Informe descargado"
                btnDownloadPdf.text = "Abrir reporte"
                btnDownloadPdf.isEnabled = true
                Log.d(TAG, "📄 PDF available locally: ${localPdfFile.absolutePath}")
            } else {
                statePdf.text = "Informe listo"
                btnDownloadPdf.text = "Descargar"
                btnDownloadPdf.isEnabled = true
                Log.d(TAG, "📄 PDF ready for download")
            }
        }
    }

    private fun getLocalPdfFile(): File {
        val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KeyFairy_Reports")
        } else {
            File(Environment.getExternalStorageDirectory(), "Download/KeyFairy_Reports")
        }

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
            Log.d(TAG, "📁 Created downloads directory: ${downloadsDir.absolutePath}")
        }

        val fileName = "reporte_practica_${practiceItem.practiceId}.pdf"
        return File(downloadsDir, fileName)
    }

    private fun setupClickListeners() {
        with(binding) {
            btnDownloadPdf.setOnClickListener {
                if (isFragmentActive) {
                    safeNavigate {
                        handlePdfAction()
                    }
                }
            }

            btnDownloadPdf.setOnLongClickListener {
                showPdfLocation()
                true
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

    private fun handlePdfAction() {
        val localPdfFile = getLocalPdfFile()
        val hasLocalPdf = pdfFileExists(localPdfFile)

        if (hasLocalPdf) {
            Log.d(TAG, "📄 Opening existing PDF: ${localPdfFile.absolutePath}")
            openPdfFile(localPdfFile)
        } else {
            Log.d(TAG, "📥 PDF not found locally, starting download")
            downloadPdf()
        }
    }

    private fun downloadPdf() {
        val uid = SecureStorage.getUid() ?: ""

        if (uid.isEmpty()) {
            showError("Error: Usuario no autenticado")
            return
        }

        val downloadsDir = getLocalPdfFile().parentFile!!
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
            Log.d(TAG, "📁 Created downloads directory: ${downloadsDir.absolutePath}")
        }

        val fileName = "reporte_practica_${practiceItem.practiceId}.pdf"
        val destinationFile = File(downloadsDir, fileName)

        if (destinationFile.exists() && !isValidPdfFile(destinationFile)) {
            destinationFile.delete()
            Log.d(TAG, "🗑️ Deleted corrupted PDF file")
        }

        Log.d(TAG, "📥 Starting download to: ${destinationFile.absolutePath}")
        practiceErrorsViewModel.downloadReport(uid, destinationFile)
    }

    private fun pdfFileExists(file: File): Boolean {
        return try {
            file.exists() && file.length() >= 1024
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking PDF existence: ${e.message}")
            false
        }
    }

    private fun isValidPdfFile(file: File): Boolean {
        return try {
            if (!file.exists() || file.length() < 1024) {
                Log.d(TAG, "📄 PDF file too small or doesn't exist: ${file.length()} bytes")
                return false
            }

            file.inputStream().use { input ->
                val header = ByteArray(5)
                val bytesRead = input.read(header)
                val headerString = String(header, 0, bytesRead)

                val isValid = headerString.startsWith("%PDF")
                Log.d(TAG, "📄 PDF validation - Valid: $isValid, Size: ${file.length()} bytes")

                isValid
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating PDF: ${e.message}", e)
            false
        }
    }

    private fun openPdfFile(file: File) {
        try {
            if (!file.exists()) {
                Log.e(TAG, "❌ PDF file does not exist: ${file.absolutePath}")
                showError("El archivo PDF no existe")
                return
            }

            Log.d(TAG, "📄 Opening PDF file: ${file.absolutePath}")
            Log.d(TAG, "📄 File size: ${file.length()} bytes")

            val authority = "${requireContext().packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                authority,
                file
            )

            val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooserIntent = Intent.createChooser(pdfIntent, "Abrir PDF con:")

            try {
                startActivity(chooserIntent)
                Log.d(TAG, "✅ PDF chooser opened successfully")
            } catch (e: android.content.ActivityNotFoundException) {
                Log.e(TAG, "❌ No PDF reader app found")
                showPdfAlternatives()
            }

        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "❌ FileProvider error: ${e.message}", e)
            showFileProviderError(file)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening PDF: ${e.message}", e)
            showGenericPdfError(file)
        }
    }

    private fun showFileProviderError(file: File) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Error de configuración")
            .setMessage("Hubo un problema con la configuración de archivos. ¿Deseas intentar compartir el archivo?")
            .setPositiveButton("Compartir") { _, _ ->
                shareFileAlternative(file)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showGenericPdfError(file: File) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Error abriendo PDF")
            .setMessage("No se pudo abrir el archivo PDF. ¿Qué deseas hacer?")
            .setPositiveButton("Compartir archivo") { _, _ ->
                shareFileAlternative(file)
            }
            .setNeutralButton("Mostrar ubicación") { _, _ ->
                showFileLocation()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPdfAlternatives() {
        val options = arrayOf(
            "Buscar app PDF en Play Store",
            "Mostrar ubicación del archivo",
            "Cancelar"
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("No se pudo abrir el PDF")
            .setMessage("No tienes ninguna aplicación para leer PDFs instalada.")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openPlayStoreForPdfApps()
                    1 -> showFileLocation()
                    2 -> { /* Cancelar */ }
                }
            }
            .show()
    }

    private fun shareFileAlternative(file: File) {
        try {
            val authority = "${requireContext().packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(requireContext(), authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte de Práctica ${practiceItem.practiceId}")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file: ${e.message}", e)
            showError("Error al compartir el archivo")
        }
    }

    private fun showPdfLocation() {
        val localFile = getLocalPdfFile()
        if (localFile.exists()) {
            val message = "Archivo guardado en:\nDescargas/KeyFairy_Reports/\n\nNombre: reporte_practica_${practiceItem.practiceId}.pdf"
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            Log.d(TAG, "📍 PDF location: ${localFile.absolutePath}")
        } else {
            Toast.makeText(requireContext(), "El archivo no existe en el dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFileLocation() {
        val localFile = getLocalPdfFile()
        val message = if (localFile.exists()) {
            "Archivo guardado en:\nDescargas/KeyFairy_Reports/\n\nPuedes buscarlo desde el explorador de archivos o instalar una app para leer PDFs."
        } else {
            "El archivo no existe en el dispositivo"
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Ubicación del archivo")
            .setMessage(message)
            .setPositiveButton("Entendido", null)
            .setNeutralButton("Buscar app PDF") { _, _ ->
                openPlayStoreForPdfApps()
            }
            .show()
    }

    private fun openPlayStoreForPdfApps() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://search?q=pdf reader")
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/search?q=pdf reader")
                }
                startActivity(intent)
            } catch (e2: Exception) {
                showError("No se pudo abrir Play Store")
            }
        }
    }

    private fun viewPosturalErrors() {
        Log.d(TAG, "👤 Viewing postural errors for practice ${practiceItem.practiceId}")

        val currentState = practiceErrorsViewModel.posturalErrorsState.value

        if (currentState !is PosturalErrorsState.Success) {
            showError("Los errores posturales aún se están cargando")
            return
        }

        if (currentState.numErrors == 0) {
            showError("No hay errores posturales para mostrar")
            return
        }

        if (practiceItem.localVideoUrl.isNullOrEmpty()) {
            showError("El video de la práctica no está disponible")
            return
        }

        if (isFragmentActive) {
            safeNavigate {
                val posturalErrorsDetailFragment = PosturalErrorsDetailFragment.newInstance(
                    practiceId = practiceItem.practiceId,
                    videoUrl = practiceItem.localVideoUrl
                )

                NavigationManager.navigateToFragment(
                    fragmentManager = parentFragmentManager,
                    fragment = posturalErrorsDetailFragment,
                    containerId = R.id.fragment_container,
                    navigationType = NavigationManager.NavigationType.REPLACE_WITH_BACK_STACK
                )
            }
        }
    }

    private fun viewMusicalErrors() {
        Log.d(TAG, "👤 Viewing musical errors for practice ${practiceItem.practiceId}")

        val currentState = practiceErrorsViewModel.musicalErrorsState.value

        if (currentState !is MusicalErrorsState.Success) {
            showError("Los errores musicales aún se están cargando")
            return
        }

        if (currentState.numErrors == 0) {
            showError("No hay errores musicales para mostrar")
            return
        }

        if (practiceItem.localVideoUrl.isNullOrEmpty()) {
            showError("El video de la práctica no está disponible")
            return
        }

        if (isFragmentActive) {
            safeNavigate {
                val musicalalErrorsDetailFragment = MusicalErrorsDetailFragment.newInstance(
                    practiceId = practiceItem.practiceId,
                    videoUrl = practiceItem.localVideoUrl
                )

                NavigationManager.navigateToFragment(
                    fragmentManager = parentFragmentManager,
                    fragment = musicalalErrorsDetailFragment,
                    containerId = R.id.fragment_container,
                    navigationType = NavigationManager.NavigationType.REPLACE_WITH_BACK_STACK
                )
            }
        }
    }

    private fun showError(message: String) {
        if (isFragmentActive) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSuccess(message: String) {
        if (isFragmentActive) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadPdfState()
        Log.d(TAG, "🔄 Fragment resumed for practice ${practiceItem.practiceId}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "🗑️ Fragment destroyed for practice ${practiceItem.practiceId}")
    }
}