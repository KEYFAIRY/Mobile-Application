package com.example.keyfairy.feature_reports.presentation.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentCompletedPracticeBinding
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.feature_reports.presentation.state.DownloadReportEvent
import com.example.keyfairy.feature_reports.presentation.state.DownloadReportState
import com.example.keyfairy.feature_reports.presentation.state.PracticeErrorsState
import com.example.keyfairy.feature_reports.presentation.state.PracticeErrorsEvent
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
        // ViewModel con scope de Activity para compartir con PosturalErrorsDetailFragment
        practiceErrorsViewModel = ViewModelProvider(
            requireActivity(), // ← Activity scope
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
            practiceErrorsViewModel.uiState.collect { state ->
                handlePosturalErrorsState(state)
            }
        }

        // Observar eventos de errores posturales
        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.uiEvent.collect { event ->
                handlePosturalErrorEvent(event)
            }
        }

        // Observar estado de descarga
        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.downloadState.collect { state ->
                handleDownloadState(state)
            }
        }

        // Observar eventos de descarga
        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.downloadEvent.collect { event ->
                handleDownloadEvent(event)
            }
        }
    }

    private fun handlePosturalErrorsState(state: PracticeErrorsState) {
        when (state) {
            is PracticeErrorsState.Initial -> {
                Log.d(TAG, "Initial state")
            }
            is PracticeErrorsState.Loading -> {
                Log.d(TAG, "Loading postural errors...")
                binding.numPosturalErrors.text = "..."
                binding.btnViewPosturalErrors.isEnabled = false
            }
            is PracticeErrorsState.Success -> {
                Log.d(TAG, "✅ Postural errors loaded: ${state.numErrors}")
                binding.numPosturalErrors.text = state.numErrors.toString()
                binding.btnViewPosturalErrors.isEnabled = state.numErrors > 0
            }
            is PracticeErrorsState.Error -> {
                Log.e(TAG, "❌ Error loading postural errors: ${state.message}")
                binding.numPosturalErrors.text = "0"
                binding.btnViewPosturalErrors.isEnabled = false
            }
        }
    }

    private fun handlePosturalErrorEvent(event: PracticeErrorsEvent) {
        when (event) {
            is PracticeErrorsEvent.ShowError -> {
                showError(event.message)
            }
            is PracticeErrorsEvent.NavigateBack -> {
                handleNavigateBack()
            }
        }
    }

    private fun handleDownloadState(state: DownloadReportState) {
        when (state) {
            is DownloadReportState.Idle -> {
                binding.btnDownloadPdf.isEnabled = !practiceItem.pdfUrl.isNullOrEmpty()
                binding.btnDownloadPdf.text = "Descargar"
            }
            is DownloadReportState.Downloading -> {
                binding.btnDownloadPdf.isEnabled = false
                binding.btnDownloadPdf.text = "Descargando..."
                Log.d(TAG, "📥 Downloading report...")
            }
            is DownloadReportState.Success -> {
                binding.btnDownloadPdf.isEnabled = true
                binding.btnDownloadPdf.text = "Descargar"
                Log.d(TAG, "✅ Download completed")
            }
            is DownloadReportState.Error -> {
                binding.btnDownloadPdf.isEnabled = true
                binding.btnDownloadPdf.text = "Descargar"
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
            if (hasPdf) {
                if(practiceWithNoErrors){
                    statePdf.text = "Felicidades"
                    btnDownloadPdf.text = "Sin errores"
                    btnDownloadPdf.isEnabled = false
                }else{
                    val localPdfFile = getLocalPdfFile()
                    val hasLocalPdf = localPdfFile.exists() && isValidPdfFile(localPdfFile)

                    if (hasLocalPdf) {
                        statePdf.text = "Informe descargado"
                        btnDownloadPdf.text = "Abrir reporte"
                        btnDownloadPdf.isEnabled = true
                        pdfImg.alpha = 1.0f
                        Log.d(TAG, "📄 PDF available locally: ${localPdfFile.absolutePath}")
                    } else {
                        statePdf.text = "Informe listo"
                        btnDownloadPdf.text = "Descargar"
                        btnDownloadPdf.isEnabled = true
                        pdfImg.alpha = 1.0f
                        Log.d(TAG, "📄 PDF available online: ${practiceItem.pdfUrl}")
                    }
                }
            } else {
                statePdf.text = "Informe en progreso"
                btnDownloadPdf.text = "No disponible"
                btnDownloadPdf.isEnabled = false
                pdfImg.alpha = 0.5f
                Log.d(TAG, "📄 PDF not available yet")
            }
        }
    }

    private fun getLocalPdfFile(): File {
        // Usar ubicación pública que es accesible en todas las versiones
        val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Usar directorio público de descargas
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KeyFairy_Reports")
        } else {
            // Android 9 y anteriores - Usar directorio externo tradicional
            File(Environment.getExternalStorageDirectory(), "Download/KeyFairy_Reports")
        }

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
            Log.d(TAG, "📁 Created downloads directory: ${downloadsDir.absolutePath}")
        }

        val fileName = "reporte_practica_${practiceItem.practiceId}.pdf"
        return File(downloadsDir, fileName)
    }

    private fun hasStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
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
            btnDownloadPdf.setOnClickListener {
                if (isFragmentActive) {
                    safeNavigate {
                        downloadPdf()
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

    private fun showPdfLocation() {
        val localFile = getLocalPdfFile()
        if (localFile.exists()) {
            val message = "Archivo guardado en:\nDescargas/KeyFairy_Reports/\n\nAccesible desde el explorador de archivos del celular."
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            Log.d(TAG, "📍 PDF location: ${localFile.absolutePath}")
        } else {
            Toast.makeText(requireContext(), "El archivo no existe", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadPdf() {
        val uid = SecureStorage.getUid() ?: ""

        if (uid.isEmpty()) {
            showError("Error: Usuario no autenticado")
            return
        }

        // Verificar permisos de almacenamiento
        if (!hasStoragePermission()) {
            showError("Se necesitan permisos de almacenamiento. Ve a la pantalla principal para concederlos.")
            return
        }

        val downloadsDir = getLocalPdfFile().parentFile!!
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
            Log.d(TAG, "📁 Created downloads directory: ${downloadsDir.absolutePath}")
        }

        val fileName = "reporte_practica_${practiceItem.practiceId}.pdf"
        val destinationFile = File(downloadsDir, fileName)

        if (destinationFile.exists() && isValidPdfFile(destinationFile)) {
            Log.d(TAG, "📄 PDF already exists, opening directly: ${destinationFile.absolutePath}")
            showSuccess("Abriendo reporte existente...")
            openPdfFile(destinationFile)
            return
        }

        Log.d(TAG, "📥 PDF not found or invalid, downloading to: ${destinationFile.absolutePath}")

        if (destinationFile.exists()) {
            destinationFile.delete()
            Log.d(TAG, "🗑️ Deleted corrupted PDF file")
        }

        practiceErrorsViewModel.downloadReport(uid, destinationFile)
    }

    private fun isValidPdfFile(file: File): Boolean {
        return try {
            if (!file.exists() || file.length() < 1024) { // Mínimo 1KB
                Log.d(TAG, "📄 PDF file too small or doesn't exist: ${file.length()} bytes")
                return false
            }

            file.inputStream().use { input ->
                val header = ByteArray(5)
                val bytesRead = input.read(header)
                val headerString = String(header, 0, bytesRead)

                val isValid = headerString.startsWith("%PDF")
                Log.d(TAG, "📄 PDF validation - Header: '$headerString', Valid: $isValid, Size: ${file.length()} bytes")

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

            // Intent específico para PDF
            val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Intent genérico como fallback
            val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Crear chooser para que el usuario seleccione la app
            val chooserIntent = Intent.createChooser(pdfIntent, "Abrir PDF con:")

            // Agregar intent genérico como alternativa
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(genericIntent))

            try {
                startActivity(chooserIntent)
                Log.d(TAG, "✅ PDF chooser opened successfully")
            } catch (e: Exception) {
                // Si el chooser falla, intentar abrir directamente
                tryDirectOpen(uri)
            }

        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "❌ FileProvider error: ${e.message}", e)
            showError("Error de configuración del proveedor de archivos")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening PDF: ${e.message}", e)
            showError("Error al abrir el archivo PDF")
        }
    }

    private fun tryDirectOpen(uri: Uri) {
        try {
            // Intentar con diferentes tipos MIME
            val mimeTypes = arrayOf("application/pdf", "*/*")

            for (mimeType in mimeTypes) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                    Log.d(TAG, "✅ PDF opened with MIME type: $mimeType")
                    return
                }
            }

            // Si nada funciona, mostrar opciones al usuario
            showPdfOpenOptions(uri)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in tryDirectOpen: ${e.message}", e)
            showPdfOpenOptions(uri)
        }
    }

    private fun showPdfOpenOptions(uri: Uri) {
        val options = arrayOf(
            "Abrir con navegador",
            "Compartir archivo",
            "Mostrar ubicación",
            "Cancelar"
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("No se encontró una app para PDFs")
            .setMessage("¿Qué deseas hacer con el archivo?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openWithBrowser(uri)
                    1 -> shareFile(uri)
                    2 -> showFileLocation()
                    3 -> { /* Cancelar */ }
                }
            }
            .show()
    }

    private fun openWithBrowser(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            showError("No se pudo abrir con el navegador")
        }
    }

    private fun shareFile(uri: Uri) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
        } catch (e: Exception) {
            showError("No se pudo compartir el archivo")
        }
    }

    private fun showFileLocation() {
        val localFile = getLocalPdfFile()
        val message = if (localFile.exists()) {
            "Archivo guardado en:\nDescargas/KeyFairy_Reports/\n\nPuedes buscarlo desde el explorador de archivos o descargar una app para leer PDFs como Adobe Reader."
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
        Log.d(TAG, "👤down Viewing postural errors for practice ${practiceItem.practiceId}")

        val currentState = practiceErrorsViewModel.uiState.value

        // Verificar que los datos estén cargados
        if (currentState !is PracticeErrorsState.Success) {
            showError("Los errores posturales aún se están cargando")
            return
        }

        // Verificar que haya errores para mostrar
        if (currentState.numErrors == 0) {
            showError("No hay errores posturales para mostrar")
            return
        }

        // Verificar que el video local exista
        if (practiceItem.localVideoUrl.isNullOrEmpty()) {
            showError("El video de la práctica no está disponible")
            return
        }

        if (isFragmentActive) {
            safeNavigate {
                val posturalErrorsDetailFragment = PosturalErrorsDetailFragment.newInstance(
                    practiceId = practiceItem.practiceId,
                    videoUrl = practiceItem.localVideoUrl // Ya verificado que no es null
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

    private fun showSuccess(message: String) {
        if (isFragmentActive) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
    private fun generateMockMusicalErrors(): Int {
        return (practiceItem.practiceId % 8) + 2
    }

    private fun cleanOldReports() {
        try {
            val downloadsDir = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "KeyFairy_Reports")
            if (!downloadsDir.exists()) return

            val files = downloadsDir.listFiles() ?: return
            val currentTime = System.currentTimeMillis()
            val maxAge = 30L * 24 * 60 * 60 * 1000 // 30 días en milisegundos

            files.forEach { file ->
                if (file.isFile && (currentTime - file.lastModified()) > maxAge) {
                    file.delete()
                    Log.d(TAG, "🗑️ Deleted old report: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old reports: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        cleanOldReports() // Limpiar archivos antiguos
        loadPdfState() // Actualizar estado del PDF
        Log.d(TAG, "🔄 Fragment resumed for practice ${practiceItem.practiceId}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "🗑️ Fragment destroyed for practice ${practiceItem.practiceId}")
    }
}