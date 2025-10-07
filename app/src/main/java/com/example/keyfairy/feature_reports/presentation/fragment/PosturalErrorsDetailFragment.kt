package com.example.keyfairy.feature_reports.presentation.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.keyfairy.databinding.FragmentPosturalErrorsDetailBinding
import com.example.keyfairy.feature_reports.domain.model.PosturalError
import com.example.keyfairy.feature_reports.presentation.adapter.PosturalErrorsAdapter
import com.example.keyfairy.feature_reports.presentation.state.PracticeErrorsState
import com.example.keyfairy.feature_reports.presentation.state.PracticeErrorsEvent
import com.example.keyfairy.feature_reports.presentation.viewmodel.PracticeErrorsViewModel
import com.example.keyfairy.feature_reports.presentation.viewmodel.PracticeErrorsViewModelFactory
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.goBack
import kotlinx.coroutines.launch
import java.io.File

class PosturalErrorsDetailFragment : BaseFragment() {

    companion object {
        private const val TAG = "PosturalErrorsDetailFragment"
        private const val ARG_PRACTICE_ID = "practice_id"
        private const val ARG_VIDEO_URL = "video_url"

        fun newInstance(practiceId: Int, videoUrl: String?): PosturalErrorsDetailFragment {
            return PosturalErrorsDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PRACTICE_ID, practiceId)
                    putString(ARG_VIDEO_URL, videoUrl)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // VARIABLES
    // -------------------------------------------------------------------------
    private var _binding: FragmentPosturalErrorsDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var practiceErrorsViewModel: PracticeErrorsViewModel
    private lateinit var errorsAdapter: PosturalErrorsAdapter
    private var practiceId: Int = 0
    private var videoUrl: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var stopVideoRunnable: Runnable? = null
    private var previousOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    // -------------------------------------------------------------------------
    // CICLO DE VIDA
    // -------------------------------------------------------------------------
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPosturalErrorsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        extractArguments()

        if (!validateVideoAccess()) return

        setupViewModel()
        setupViews()
        setupRecyclerView()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        lockLandscapeOrientation()
        Log.d(TAG, "📱 Orientation locked to landscape")
    }

    override fun onStop() {
        super.onStop()
        restoreOrientation()
        Log.d(TAG, "🔓 Orientation restored to portrait/default")
    }

    override fun onPause() {
        super.onPause()
        stopScheduledVideoPlayback()
        pauseVideoIfPlaying()
    }

    override fun onResume() {
        super.onResume()
        // Verificar permisos al volver de configuración
        if (hasVideoPermissions() && _binding != null) {
            // Si ahora tenemos permisos y el video no está configurado, intentar de nuevo
            if (binding.videoView.duration <= 0) {
                Log.d(TAG, "🔄 Permissions granted, retrying video setup")
                handler.postDelayed({
                    if (_binding != null) {
                        setupVideoPlayer()
                    }
                }, 500)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopScheduledVideoPlayback()
        stopVideoPlayback()
        _binding = null
        Log.d(TAG, "🗑️ View destroyed")
    }

    // -------------------------------------------------------------------------
    // CONFIGURACIÓN DE ORIENTACIÓN
    // -------------------------------------------------------------------------
    private fun lockLandscapeOrientation() {
        try {
            if (previousOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                previousOrientation = requireActivity().requestedOrientation
            }
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
        } catch (e: Exception) {
            Log.e(TAG, "Error locking landscape: ${e.message}", e)
        }
    }

    private fun restoreOrientation() {
        try {
            requireActivity().setRequestedOrientation(previousOrientation)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring orientation: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------------------
    // CONFIGURACIÓN DE VISTAS Y VIEWMODEL
    // -------------------------------------------------------------------------
    private fun extractArguments() {
        practiceId = arguments?.getInt(ARG_PRACTICE_ID)
            ?: throw IllegalArgumentException("Practice ID is required")
        videoUrl = arguments?.getString(ARG_VIDEO_URL) ?: ""
        Log.d(TAG, "📋 Practice ID: $practiceId, Video URL: '$videoUrl'")
    }

    private fun validateVideoAccess(): Boolean {
        return when {
            videoUrl.isEmpty() -> {
                showVideoNotFoundAndGoBack("La URL del video está vacía")
                false
            }
            !isValidVideoFile(videoUrl) -> {
                showVideoNotFoundAndGoBack("Formato de video no válido")
                false
            }
            else -> {
                true
            }
        }
    }

    private fun isValidVideoFile(filePath: String): Boolean {
        val validExtensions = listOf(".mp4", ".3gp", ".webm", ".mkv", ".avi")
        return validExtensions.any { filePath.lowercase().endsWith(it) }
    }

    private fun setupViewModel() {
        val factory = PracticeErrorsViewModelFactory(practiceId)
        practiceErrorsViewModel =
            ViewModelProvider(requireActivity(), factory)[PracticeErrorsViewModel::class.java]
    }

    private fun setupViews() {
        binding.btnRetry.setOnClickListener {
            practiceErrorsViewModel.retry()
        }
    }

    private fun setupRecyclerView() {
        errorsAdapter = PosturalErrorsAdapter { error, position ->
            onErrorClicked(error, position)
        }

        binding.recyclerViewErrors.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = errorsAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.uiState.collect { handleState(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            practiceErrorsViewModel.uiEvent.collect { handleUiEvent(it) }
        }
    }

    // -------------------------------------------------------------------------
    // MANEJO DE ESTADOS
    // -------------------------------------------------------------------------
    private fun handleState(state: PracticeErrorsState) {
        when (state) {
            is PracticeErrorsState.Initial,
            is PracticeErrorsState.Loading -> showLoading()
            is PracticeErrorsState.Success -> if (state.numErrors > 0) showSuccess(state) else showEmpty()
            is PracticeErrorsState.Error -> showError(state.message)
        }
    }

    private fun handleUiEvent(event: PracticeErrorsEvent) {
        if (event is PracticeErrorsEvent.ShowError) {
            showToast(event.message)
        }
    }

    private fun showLoading() {
        with(binding) {
            loadingLayout.visibility = View.VISIBLE
            contentLayout.visibility = View.GONE
            errorLayout.visibility = View.GONE
            emptyLayout.visibility = View.GONE
        }
    }

    private fun showSuccess(state: PracticeErrorsState.Success) {
        with(binding) {
            loadingLayout.visibility = View.GONE
            contentLayout.visibility = View.VISIBLE
            errorLayout.visibility = View.GONE
            emptyLayout.visibility = View.GONE
            tvErrorsCount.text = "${state.numErrors} error${if (state.numErrors != 1) "es" else ""}"
            errorsAdapter.submitList(state.errors)
        }
        setupVideoPlayer()
    }

    private fun showError(message: String) {
        with(binding) {
            loadingLayout.visibility = View.GONE
            contentLayout.visibility = View.GONE
            errorLayout.visibility = View.VISIBLE
            emptyLayout.visibility = View.GONE
            tvErrorMessage.text = message
        }
    }

    private fun showEmpty() {
        with(binding) {
            loadingLayout.visibility = View.GONE
            contentLayout.visibility = View.GONE
            errorLayout.visibility = View.GONE
            emptyLayout.visibility = View.VISIBLE
        }
    }

    // -------------------------------------------------------------------------
    // VERIFICACIÓN DE PERMISOS
    // -------------------------------------------------------------------------
    private fun hasVideoPermissions(): Boolean {
        val requiredPermissions = getRequiredVideoPermissions()
        val hasAllPermissions = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }

        Log.d(TAG, "🔐 Video permissions check: $hasAllPermissions")
        return hasAllPermissions
    }

    private fun getRequiredVideoPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                listOf(Manifest.permission.READ_MEDIA_VIDEO)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun showPermissionsNeededDialog() {
        val permissionMessage = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                "Se necesita acceso COMPLETO a videos para reproducir la grabación.\n\n" +
                        "⚠️ IMPORTANTE: En la configuración, asegúrate de conceder acceso completo, NO acceso limitado."
            else ->
                "Se necesita permiso de almacenamiento para acceder al video de análisis."
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Permisos necesarios")
            .setMessage(
                "$permissionMessage\n\n" +
                        "¿Deseas ir a la configuración de la app para habilitar estos permisos?"
            )
            .setPositiveButton("Ir a configuración") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Continuar sin video") { _, _ ->
                showToast("No se puede reproducir el video sin permisos")
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
            showToast("Habilita el acceso completo a videos y regresa a la app")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app settings: ${e.message}", e)
            showToast("No se pudo abrir la configuración")
        }
    }

    // -------------------------------------------------------------------------
    // VIDEO PLAYER - CON VERIFICACIÓN DE PERMISOS PRIMERO
    // -------------------------------------------------------------------------
    private fun setupVideoPlayer() {
        // PRIMERA VERIFICACIÓN: Permisos
        if (!hasVideoPermissions()) {
            Log.w(TAG, "⚠️ Video permissions not granted - showing permission dialog")
            showPermissionsNeededDialog()
            return
        }

        Log.d(TAG, "✅ Video permissions verified, proceeding with video setup")

        try {
            val uri = getVideoUriDirect()
            if (uri == null) {
                Log.e(TAG, "❌ Could not get video URI - file access issue")
                showVideoFileNotFoundError()
                return
            }

            Log.d(TAG, "🎬 Setting up video with URI: $uri")

            val mediaController = MediaController(requireContext()).apply {
                setAnchorView(binding.videoView)
            }

            with(binding.videoView) {
                stopPlayback()
                setMediaController(mediaController)
                setVideoURI(uri)
                setOnPreparedListener { mp ->
                    Log.d(TAG, "✅ Video prepared successfully")
                    binding.videoLoading.visibility = View.GONE
                    mp.isLooping = false
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "❌ Video playback error $what/$extra")
                    showVideoPlaybackError()
                    true
                }
                setOnInfoListener { _, what, extra ->
                    Log.d(TAG, "ℹ️ Video info: what=$what, extra=$extra")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception setting up video: ${e.message}", e)
            showVideoPlaybackError()
        }
    }

    private fun getVideoUriDirect(): Uri? {
        // Intento directo con FileProvider (acceso directo al archivo)
        try {
            val file = File(videoUrl)
            if (file.exists() && file.canRead()) {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                Log.d(TAG, "✅ Direct file access successful: $uri")
                return uri
            } else {
                Log.w(TAG, "⚠️ File not accessible directly: exists=${file.exists()}, canRead=${file.canRead()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Direct file access failed: ${e.message}")
        }

        // Intento con URI directo (para archivos en almacenamiento público)
        try {
            val uri = Uri.fromFile(File(videoUrl))
            Log.d(TAG, "✅ URI from file created: $uri")
            return uri
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ URI from file failed: ${e.message}")
        }

        Log.e(TAG, "❌ All video access methods failed")
        return null
    }

    // -------------------------------------------------------------------------
    // MANEJO DE ERRORES ESPECÍFICOS
    // -------------------------------------------------------------------------
    private fun showVideoFileNotFoundError() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Archivo de video no encontrado")
            .setMessage(
                "No se pudo acceder al archivo de video.\n\n" +
                        "Posibles causas:\n" +
                        "• El video fue movido o eliminado\n" +
                        "• El archivo está en una ubicación no accesible\n" +
                        "• Problemas con el almacenamiento del dispositivo"
            )
            .setPositiveButton("Cerrar") { _, _ ->
                handler.postDelayed({
                    safeNavigate {
                        if (!goBack()) requireActivity().finish()
                    }
                }, 1000)
            }
            .setCancelable(false)
            .show()
    }

    private fun showVideoPlaybackError() {
        // Verificar nuevamente los permisos por si acaso
        if (!hasVideoPermissions()) {
            Log.w(TAG, "❌ Video playback error due to missing permissions")
            showPermissionsNeededDialog()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Error de reproducción")
            .setMessage(
                "Error al reproducir el video.\n\n" +
                        "Posibles causas:\n" +
                        "• El archivo de video está dañado o corrupto\n" +
                        "• Formato de video no compatible\n" +
                        "• Problemas con el reproductor de video del sistema"
            )
            .setPositiveButton("Cerrar") { _, _ ->
                handler.postDelayed({
                    safeNavigate {
                        if (!goBack()) requireActivity().finish()
                    }
                }, 1000)
            }
            .setCancelable(false)
            .show()
    }

    // -------------------------------------------------------------------------
    // CONTROL DE VIDEO
    // -------------------------------------------------------------------------
    private fun stopScheduledVideoPlayback() {
        stopVideoRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun pauseVideoIfPlaying() {
        try {
            if (_binding != null && binding.videoView.isPlaying) {
                binding.videoView.pause()
            }
        } catch (_: Exception) { }
    }

    private fun stopVideoPlayback() {
        try {
            if (_binding != null) binding.videoView.stopPlayback()
        } catch (_: Exception) { }
    }

    // -------------------------------------------------------------------------
    // ERRORES Y UTILIDADES
    // -------------------------------------------------------------------------
    private fun showVideoNotFoundAndGoBack(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        handler.postDelayed({
            safeNavigate {
                if (!goBack()) requireActivity().finish()
            }
        }, 2000)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun onErrorClicked(error: PosturalError, position: Int) {
        // Verificar permisos antes de intentar controlar el video
        if (!hasVideoPermissions()) {
            showToast("Se necesitan permisos de video para esta función")
            showPermissionsNeededDialog()
            return
        }

        Log.d(TAG, "🎯 Error clicked: ${error.minSecInit} - ${error.minSecEnd}")

        try {
            if (!isVideoReady()) {
                showToast("El video aún no está listo")
                return
            }

            stopScheduledVideoPlayback()

            val startTimeMs = convertTimeToMillis(error.minSecInit)
            val endTimeMs = convertTimeToMillis(error.minSecEnd)
            val durationMs = endTimeMs - startTimeMs

            if (durationMs <= 0) {
                showToast("Duración del error inválida")
                return
            }

            Log.d(TAG, "🕐 Target: ${startTimeMs}ms to ${endTimeMs}ms (${durationMs}ms duration)")

            with(binding.videoView) {
                if (isPlaying) {
                    pause()
                    Log.d(TAG, "⏸️ Video paused before seeking")
                }

                seekTo(startTimeMs)
                Log.d(TAG, "🎯 Seek initiated to ${startTimeMs}ms")

                postDelayed({
                    val actualPosition = currentPosition
                    Log.d(TAG, "🎯 After seek - Target: ${startTimeMs}ms, Actual: ${actualPosition}ms, Diff: ${actualPosition - startTimeMs}ms")

                    if (kotlin.math.abs(actualPosition - startTimeMs) > 1000) {
                        Log.w(TAG, "⚠️ Large seek difference (${actualPosition - startTimeMs}ms), re-seeking...")
                        seekTo(startTimeMs)

                        postDelayed({
                            val finalPosition = currentPosition
                            Log.d(TAG, "🎯 After re-seek: ${finalPosition}ms")
                            start()
                            Log.d(TAG, "▶️ Video started after re-seek")
                        }, 500)
                    } else {
                        start()
                        Log.d(TAG, "▶️ Video started at correct position")
                    }

                }, 800)
            }

            stopVideoRunnable = Runnable {
                if (_binding != null && binding.videoView.isPlaying) {
                    val endPosition = binding.videoView.currentPosition
                    binding.videoView.pause()
                    Log.d(TAG, "⏸️ Video paused at ${endPosition}ms (target was ${endTimeMs}ms)")
                }
            }

            val totalDelay = durationMs + 800L // 800ms del delay inicial
            handler.postDelayed(stopVideoRunnable!!, totalDelay)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onErrorClicked: ${e.message}", e)
            showToast("Error al reproducir el error")
        }
    }

    private fun isVideoReady(): Boolean {
        return try {
            binding.videoView.duration > 0
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking video readiness: ${e.message}", e)
            false
        }
    }

    private fun convertTimeToMillis(timeString: String): Int {
        return try {
            val parts = timeString.split(":")
            val minutes = parts[0].toInt()
            val seconds = parts[1].toInt()
            val totalSeconds = minutes * 60 + seconds
            totalSeconds * 1000
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error converting time '$timeString': ${e.message}", e)
            0
        }
    }
}