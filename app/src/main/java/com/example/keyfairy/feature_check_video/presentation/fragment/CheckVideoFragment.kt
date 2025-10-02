package com.example.keyfairy.feature_check_video.presentation.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentCheckVideoBinding
import com.example.keyfairy.feature_calibrate.presentation.CalibrateCameraFragment
import com.example.keyfairy.feature_check_video.data.repository.PracticeRepositoryImpl
import com.example.keyfairy.feature_check_video.domain.model.Practice
import com.example.keyfairy.feature_check_video.domain.use_case.RegisterPracticeUseCase
import com.example.keyfairy.feature_check_video.presentation.state.RegisterPracticeState
import com.example.keyfairy.feature_check_video.presentation.viewmodel.RegisterPracticeViewModel
import com.example.keyfairy.feature_check_video.presentation.viewmodel.RegisterPracticeViewModelFactory
import com.example.keyfairy.feature_home.presentation.HomeActivity
import com.example.keyfairy.feature_practice.presentation.PracticeFragment
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.navigateAndClearStack
import com.example.keyfairy.utils.storage.SecureStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CheckVideoFragment : BaseFragment() {

    private var _binding: FragmentCheckVideoBinding? = null
    private val binding get() = _binding!!

    private lateinit var registerPracticeViewModel: RegisterPracticeViewModel

    private var videoUri: Uri? = null
    private var videoFile: File? = null

    private var escalaName: String? = null
    private var escalaNotes: Int? = null
    private var octaves: Int? = null
    private var bpm: Int? = null
    private var noteType: String? = null
    private var escalaData: String? = null
    private var videoDurationSeconds: Int = 0

    companion object {
        private const val ARG_VIDEO_URI = "video_uri"
        private const val ARG_ESCALA_NAME = "escalaName"
        private const val ARG_ESCALA_NOTES = "escalaNotes"
        private const val ARG_OCTAVES = "octaves"
        private const val ARG_BPM = "bpm"
        private const val ARG_VIDEO_DURATION = "videoDuration"

        fun newInstance(
            videoUri: Uri,
            escalaName: String,
            escalaNotes: Int,
            octaves: Int,
            bpm: Int,
            videoDurationSeconds: Int
        ): CheckVideoFragment {
            val fragment = CheckVideoFragment()
            val args = Bundle().apply {
                putParcelable(ARG_VIDEO_URI, videoUri)
                putString(ARG_ESCALA_NAME, escalaName)
                putInt(ARG_ESCALA_NOTES, escalaNotes)
                putInt(ARG_OCTAVES, octaves)
                putInt(ARG_BPM, bpm)
                putInt(ARG_VIDEO_DURATION, videoDurationSeconds)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFullscreenMode()
        extractArguments()
        setupViewModel()
        setupVideoPlayer()
        setupObservers()
        setupClickListeners()
    }

    private fun setupFullscreenMode() {
        // Mantener fullscreen mode y bottom navigation oculta
        (activity as? HomeActivity)?.enableFullscreen()
        (activity as? HomeActivity)?.hideBottomNavigation()
    }

    private fun extractArguments() {
        arguments?.let { bundle ->
            videoUri = bundle.getParcelable(ARG_VIDEO_URI)
            escalaName = bundle.getString(ARG_ESCALA_NAME)
            escalaNotes = bundle.getInt(ARG_ESCALA_NOTES)
            octaves = bundle.getInt(ARG_OCTAVES)
            bpm = bundle.getInt(ARG_BPM)
            videoDurationSeconds = bundle.getInt(ARG_VIDEO_DURATION)
            noteType = bundle.getString("noteType")
            escalaData = bundle.getString("escala_data")
        }

        if (videoUri == null) {
            if (isFragmentActive) {
                Toast.makeText(requireContext(), "No video to display", Toast.LENGTH_SHORT).show()
                returnToPracticeFragment()
            }
            return
        }

        videoFile = uriToFile(videoUri!!)
    }

    private fun setupViewModel() {
        val practiceRepository = PracticeRepositoryImpl()
        val registerPracticeUseCase = RegisterPracticeUseCase(practiceRepository)
        val factory = RegisterPracticeViewModelFactory(registerPracticeUseCase)

        registerPracticeViewModel = ViewModelProvider(this, factory)[RegisterPracticeViewModel::class.java]
    }

    private fun setupVideoPlayer() {
        videoUri?.let { uri ->
            val mediaController = android.widget.MediaController(requireContext())
            mediaController.setAnchorView(binding.videoView)
            binding.videoView.setMediaController(mediaController)

            binding.videoView.setVideoURI(uri)

            binding.videoView.setOnPreparedListener { mediaPlayer ->
                Log.d("VideoPlayback", "Video prepared and ready to play")

                try {
                    mediaPlayer.setVideoScalingMode(
                        android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    )
                } catch (e: Exception) {
                    Log.e("VideoPlayback", "Error setting video scaling: ${e.message}")
                }

                if (isFragmentActive) {
                    binding.videoView.start()
                }
            }

            binding.videoView.setOnErrorListener { _, what, extra ->
                Log.e("VideoPlayback", "Error playing video: what=$what, extra=$extra")
                if (isFragmentActive) {
                    Toast.makeText(requireContext(), "Error playing video", Toast.LENGTH_SHORT).show()
                }
                true
            }

            binding.videoView.setOnCompletionListener {
                Log.d("VideoPlayback", "Video playback completed")
            }
        }
    }

    private fun setupObservers() {
        registerPracticeViewModel.registerPracticeState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegisterPracticeState.Idle -> hideLoading()
                is RegisterPracticeState.Loading -> showLoading()
                is RegisterPracticeState.Success -> {
                    hideLoading()
                    if (isFragmentActive) {
                        showSuccess("¡Práctica enviada exitosamente! ID: ${state.practiceResult.practiceId}")
                        // Pequeño delay para que el usuario vea el mensaje de éxito
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (isFragmentActive) {
                                returnToPracticeFragment()
                            }
                        }, 1500)
                    }
                }
                is RegisterPracticeState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Button retry - deletes the video and retries the practice
        binding.btnSave.setOnClickListener {
            safeNavigate {
                deleteVideoAndRetry()
            }
        }

        // Button send - sends the practice to the server
        binding.btnDelete.setOnClickListener {
            safeNavigate {
                sendPracticeToServer()
            }
        }
    }

    private fun deleteVideoAndRetry() {
        videoUri?.let { uri ->
            try {
                val deleted = requireContext().contentResolver.delete(uri, null, null)
                if (deleted > 0) {
                    Log.d("VideoPlayback", "Video deleted successfully")
                    if (isFragmentActive) {
                        Toast.makeText(
                            requireContext(),
                            "Video eliminado. Repitiendo práctica...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("VideoPlayback", "Error deleting video: ${e.message}")
            }

            navigateToCalibration()
        }
    }

    private fun navigateToCalibration() {
        val calibrationFragment = CalibrateCameraFragment().apply {
            arguments = Bundle().apply {
                putString("escalaName", escalaName)
                putInt("escalaNotes", escalaNotes ?: 8)
                putInt("octaves", octaves ?: 1)
                putInt("bpm", bpm ?: 120)
                putString("noteType", noteType)
                putString("escala_data", escalaData)
            }
        }

        // Navegación lineal: reemplaza sin back stack para volver a calibrar
        navigateAndClearStack(calibrationFragment, R.id.fragment_container)
    }

    private fun sendPracticeToServer() {
        val uid = SecureStorage.getUid()
        if (uid.isNullOrEmpty()) {
            showError("Error: Usuario no autenticado")
            return
        }

        val videoFile = this.videoFile
        if (videoFile == null || !videoFile.exists()) {
            showError("Error: Archivo de video no encontrado")
            return
        }

        if (escalaName.isNullOrEmpty()) {
            showError("Error: Datos de práctica incompletos")
            return
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val practice = Practice(
            practiceId = 0,
            date = currentDate,
            time = currentTime,
            duration = videoDurationSeconds,
            uid = uid,
            videoLocalRoute = videoFile.absolutePath,
            scale = escalaName!!,
            scaleType = determineScaleType(escalaName!!),
            reps = octaves ?: 1,
            bpm = bpm ?: 120
        )

        Log.d("CheckVideo", "Sending practice: $practice")
        registerPracticeViewModel.registerPractice(practice, videoFile)
    }

    private fun determineScaleType(scaleName: String): String {
        return when {
            scaleName.contains("Major", ignoreCase = true) -> "Major"
            scaleName.contains("Minor", ignoreCase = true) -> "Minor"
            else -> "Major"
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File(
                requireContext().cacheDir,
                "practice_video_${System.currentTimeMillis()}.mp4"
            )

            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            Log.e("CheckVideo", "Error converting URI to File: ${e.message}")
            null
        }
    }

    private fun showLoading() {
        if (!isFragmentActive) return

        binding.btnDelete.isEnabled = false
        binding.btnSave.isEnabled = false
        binding.btnDelete.text = "Enviando..."
    }

    private fun hideLoading() {
        if (!isFragmentActive) return

        binding.btnDelete.isEnabled = true
        binding.btnSave.isEnabled = true
        binding.btnDelete.text = "Enviar"
    }

    private fun showError(message: String) {
        if (!isFragmentActive) return
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        if (!isFragmentActive) return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun returnToPracticeFragment() {
        safeNavigate {
            // Restaurar navegación normal
            (activity as? HomeActivity)?.disableFullscreen()
            (activity as? HomeActivity)?.showBottomNavigation()

            val practiceFragment = PracticeFragment()

            // Navegación que limpia todo el stack y regresa a PracticeFragment
            navigateAndClearStack(practiceFragment, R.id.fragment_container)

            // También actualizar la selección del bottom navigation
            (activity as? HomeActivity)?.returnToMainNavigation(practiceFragment)

            Log.d("CheckVideo", "Navigating back to PracticeFragment - Flow completed")
        }
    }

    override fun onPause() {
        super.onPause()
        if (_binding != null && isFragmentActive) {
            binding.videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && isFragmentActive && !binding.videoView.isPlaying) {
            binding.videoView.resume()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Limpiar video player
        if (_binding != null) {
            binding.videoView.stopPlayback()
        }

        // Limpiar ViewModel state
        if (::registerPracticeViewModel.isInitialized) {
            registerPracticeViewModel.resetState()
        }

        // Limpiar archivo temporal
        videoFile?.let { file ->
            try {
                if (file.exists()) {
                    file.delete()
                    Log.d("CheckVideo", "Temporary video file cleaned up")
                }
            } catch (e: Exception) {
                Log.e("CheckVideo", "Error cleaning up temporary file: ${e.message}")
            }
        }

        _binding = null
    }
}