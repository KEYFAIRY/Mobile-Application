package com.example.keyfairy.feature_reports.presentation.fragment

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.keyfairy.databinding.FragmentPosturalErrorsDetailBinding
import com.example.keyfairy.feature_reports.domain.model.PosturalError
import com.example.keyfairy.feature_reports.presentation.adapter.PosturalErrorsAdapter
import com.example.keyfairy.feature_reports.presentation.state.PosturalErrorsState
import com.example.keyfairy.feature_reports.presentation.state.PosturalErrorsUiEvent
import com.example.keyfairy.feature_reports.presentation.viewmodel.PosturalErrorsViewModel
import com.example.keyfairy.feature_reports.presentation.viewmodel.PosturalErrorsViewModelFactory
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

    private lateinit var posturalErrorsViewModel: PosturalErrorsViewModel
    private lateinit var errorsAdapter: PosturalErrorsAdapter

    private var practiceId: Int = 0
    private var videoUrl: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var stopVideoRunnable: Runnable? = null

    // Guardar orientación previa
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

        if (!validateVideoFile()) return

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

    private fun validateVideoFile(): Boolean {
        return when {
            videoUrl.isEmpty() -> {
                showVideoNotFoundAndGoBack("La URL del video está vacía")
                false
            }
            !File(videoUrl).exists() -> {
                showVideoNotFoundAndGoBack("Video no encontrado")
                false
            }
            !isValidVideoFile(videoUrl) -> {
                showVideoNotFoundAndGoBack("Formato de video no válido")
                false
            }
            else -> true
        }
    }

    private fun isValidVideoFile(filePath: String): Boolean {
        val validExtensions = listOf(".mp4", ".3gp", ".webm", ".mkv", ".avi")
        return validExtensions.any { filePath.lowercase().endsWith(it) }
    }

    private fun setupViewModel() {
        val factory = PosturalErrorsViewModelFactory(practiceId)
        posturalErrorsViewModel =
            ViewModelProvider(requireActivity(), factory)[PosturalErrorsViewModel::class.java]
    }

    private fun setupViews() {
        binding.btnRetry.setOnClickListener {
            posturalErrorsViewModel.retry()
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
            posturalErrorsViewModel.uiState.collect { handleState(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            posturalErrorsViewModel.uiEvent.collect { handleUiEvent(it) }
        }
    }

    // -------------------------------------------------------------------------
    // MANEJO DE ESTADOS
    // -------------------------------------------------------------------------
    private fun handleState(state: PosturalErrorsState) {
        when (state) {
            is PosturalErrorsState.Initial,
            is PosturalErrorsState.Loading -> showLoading()
            is PosturalErrorsState.Success -> if (state.numErrors > 0) showSuccess(state) else showEmpty()
            is PosturalErrorsState.Error -> showError(state.message)
        }
    }

    private fun handleUiEvent(event: PosturalErrorsUiEvent) {
        if (event is PosturalErrorsUiEvent.ShowError) {
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

    private fun showSuccess(state: PosturalErrorsState.Success) {
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
    // VIDEO PLAYER
    // -------------------------------------------------------------------------
    private fun setupVideoPlayer() {
        try {
            val uri = Uri.fromFile(File(videoUrl))
            val mediaController = MediaController(requireContext()).apply {
                setAnchorView(binding.videoView)
            }

            with(binding.videoView) {
                stopPlayback()
                setMediaController(mediaController)
                setVideoURI(uri)
                setOnPreparedListener { mp ->
                    binding.videoLoading.visibility = View.GONE
                    mp.isLooping = false
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Video error $what/$extra")
                    showVideoNotFoundAndGoBack("Error al reproducir el video")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring video: ${e.message}", e)
            showVideoNotFoundAndGoBack("Error al configurar el video")
        }
    }

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
