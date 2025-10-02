package com.example.keyfairy.feature_check_video.presentation.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.databinding.FragmentCheckVideoBinding
import com.example.keyfairy.feature_check_video.data.repository.PracticeRepositoryImpl
import com.example.keyfairy.feature_check_video.domain.model.Practice
import com.example.keyfairy.feature_check_video.domain.use_case.RegisterPracticeUseCase
import com.example.keyfairy.feature_check_video.presentation.state.RegisterPracticeState
import com.example.keyfairy.feature_check_video.presentation.viewmodel.RegisterPracticeViewModel
import com.example.keyfairy.feature_check_video.presentation.viewmodel.RegisterPracticeViewModelFactory
import com.example.keyfairy.utils.storage.SecureStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CheckVideoFragment : Fragment() {

    private var _binding: FragmentCheckVideoBinding? = null
    private val binding get() = _binding!!

    private lateinit var registerPracticeViewModel: RegisterPracticeViewModel

    private var videoUri: Uri? = null
    private var videoFile: File? = null


    // Practice data from previous fragment
    private var escalaName: String? = null
    private var escalaNotes: Int? = null
    private var octaves: Int? = null
    private var bpm: Int? = null
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

        extractArguments()
        setupViewModel()
        setupVideoPlayer()
        setupObservers()
        setupClickListeners()
    }

    private fun extractArguments() {
        arguments?.let { bundle ->
            videoUri = bundle.getParcelable(ARG_VIDEO_URI)
            escalaName = bundle.getString(ARG_ESCALA_NAME)
            escalaNotes = bundle.getInt(ARG_ESCALA_NOTES)
            octaves = bundle.getInt(ARG_OCTAVES)
            bpm = bundle.getInt(ARG_BPM)
            videoDurationSeconds = bundle.getInt(ARG_VIDEO_DURATION)
        }

        if (videoUri == null) {
            Toast.makeText(requireContext(), "No video to display", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        // URI -> File
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
            // Set up media controller for play/pause controls
            val mediaController = android.widget.MediaController(requireContext())
            mediaController.setAnchorView(binding.videoView)
            binding.videoView.setMediaController(mediaController)

            // Set video URI and prepare
            binding.videoView.setVideoURI(uri)

            binding.videoView.setOnPreparedListener { mediaPlayer ->
                Log.d("VideoPlayback", "Video prepared and ready to play")
                binding.videoView.start() // Auto-play when ready
            }

            binding.videoView.setOnErrorListener { _, what, extra ->
                Log.e("VideoPlayback", "Error playing video: what=$what, extra=$extra")
                Toast.makeText(requireContext(), "Error playing video", Toast.LENGTH_SHORT).show()
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
                    showSuccess("¡Práctica enviada exitosamente! ID: ${state.practiceResult.practiceId}")
                    navigateBack()
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
            deleteVideoAndRetry()
        }

        // Button send - sends the practice to the server
        binding.btnDelete.setOnClickListener {
            sendPracticeToServer()
        }
    }

    private fun deleteVideoAndRetry() {
        videoUri?.let { uri ->
            try {
                val deleted = requireContext().contentResolver.delete(uri, null, null)
                if (deleted > 0) {
                    Log.d("VideoPlayback", "Video deleted successfully")
                    Toast.makeText(requireContext(), "Video eliminado. Repitiendo práctica...", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("VideoPlayback", "Failed to delete video")
                }
            } catch (e: Exception) {
                Log.e("VideoPlayback", "Error deleting video: ${e.message}")
            }

            // Go back to the previous fragment
            parentFragmentManager.popBackStack()
        }
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

        // Create Practice object
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val practice = Practice(
            practiceId = 0, // backend will assign this
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
            else -> "Major" // Default
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            // Create a temporary file in cache
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File(requireContext().cacheDir, "practice_video_${System.currentTimeMillis()}.mp4")

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
        binding.btnDelete.isEnabled = false
        binding.btnSave.isEnabled = false
        binding.btnDelete.text = "Enviando..."
    }

    private fun hideLoading() {
        binding.btnDelete.isEnabled = true
        binding.btnSave.isEnabled = true
        binding.btnDelete.text = "Enviar"
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateBack() {
        parentFragmentManager.popBackStack()
        parentFragmentManager.popBackStack()
    }

    override fun onPause() {
        super.onPause()
        // Check if _binding is not null before accessing its members
        if (_binding != null) {
            binding.videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if _binding is not null before accessing its members
        if (_binding != null && !binding.videoView.isPlaying) {
            binding.videoView.resume()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        registerPracticeViewModel.resetState()
        _binding = null
    }
}