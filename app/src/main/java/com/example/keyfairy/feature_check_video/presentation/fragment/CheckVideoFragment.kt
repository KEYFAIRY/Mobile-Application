package com.example.keyfairy.feature_check_video.presentation.fragment

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.work.WorkInfo
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentCheckVideoBinding
import com.example.keyfairy.feature_calibrate.presentation.CalibrateCameraFragment
import com.example.keyfairy.feature_check_video.domain.model.Practice
import com.example.keyfairy.feature_home.presentation.HomeActivity
import com.example.keyfairy.feature_practice.presentation.PracticeFragment
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.navigateAndClearStack
import com.example.keyfairy.utils.enums.ScaleType
import com.example.keyfairy.utils.storage.SecureStorage
import com.example.keyfairy.utils.workers.VideoUploadManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CheckVideoFragment : BaseFragment() {

    private var _binding: FragmentCheckVideoBinding? = null
    private val binding get() = _binding!!

    private var backPressedCallback: OnBackPressedCallback? = null
    private lateinit var videoUploadManager: VideoUploadManager

    private var videoUri: Uri? = null
    private var videoFile: File? = null
    private var workId: UUID? = null

    private var escalaName: String? = null
    private var escalaNotes: Int? = null
    private var octaves: Int? = null
    private var bpm: Int? = null
    private var figure: Double? = null
    private var escalaData: String? = null
    private var videoDurationSeconds: Int = 0

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
        setupVideoUploadManager()
        setupBackPressedHandler()
        extractArguments()
        setupVideoPlayer()
        setupClickListeners()
    }

    private fun setupFullscreenMode() {
        (activity as? HomeActivity)?.enableFullscreen()
        (activity as? HomeActivity)?.hideBottomNavigation()
    }

    private fun setupVideoUploadManager() {
        videoUploadManager = VideoUploadManager(requireContext())
    }

    private fun setupBackPressedHandler() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("CheckVideo", "🔙 Back button pressed - handling cleanup")

                workId?.let { id ->
                    videoUploadManager.cancelWork(id)
                    Log.d("CheckVideo", "🚫 Cancelled pending upload work: $id")
                }
                deleteVideoAndReturn()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback!!)
    }

    private fun deleteVideoAndReturn() {
        safeNavigate {
            deleteOriginalVideo()
            if (isFragmentActive) {
                Toast.makeText(
                    requireContext(),
                    "Regresando a práctica...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            returnToPracticeFragment()
        }
    }

    private fun deleteOriginalVideo() {
        videoUri?.let { uri ->
            try {
                val deleted = requireContext().contentResolver.delete(uri, null, null)
                if (deleted > 0) {
                    Log.d("CheckVideo", "✅ Original video deleted from gallery (Movies/KeyFairy)")
                } else {
                    Log.w("CheckVideo", "⚠️ Could not delete original video from MediaStore")
                }
            } catch (e: Exception) {
                Log.e("CheckVideo", "❌ Error deleting original video: ${e.message}")
            }
        }
    }

    private fun extractArguments() {
        arguments?.let { bundle ->
            videoUri = bundle.getParcelable("video_uri")
            escalaName = bundle.getString("escalaName")
            escalaNotes = bundle.getInt("escalaNotes")
            octaves = bundle.getInt("octaves")
            bpm = bundle.getInt("bpm")
            videoDurationSeconds = bundle.getInt("videoDuration")
            figure = bundle.getDouble("figure")
            escalaData = bundle.getString("escala_data")
        }

        if (videoUri == null) {
            if (isFragmentActive) {
                Toast.makeText(requireContext(), "No video to display", Toast.LENGTH_SHORT).show()
                returnToPracticeFragment()
            }
            return
        }

        videoFile = getVideoFile(videoUri!!)
    }

    private fun getVideoFile(uri: Uri): File? {
        return try {
            val cursor = requireContext().contentResolver.query(
                uri,
                arrayOf(MediaStore.Video.Media.DATA),
                null, null, null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    val file = File(filePath)

                    Log.d("CheckVideo", "📹 Found real video file:")
                    Log.d("CheckVideo", "   📁 Path: ${file.absolutePath}")
                    Log.d("CheckVideo", "   📊 Size: ${file.length() / 1024}KB")
                    Log.d("CheckVideo", "   ✅ Exists: ${file.exists()}")

                    if (file.exists()) {
                        return file
                    } else {
                        Log.e("CheckVideo", "❌ Video file does not exist at: $filePath")
                    }
                }
            }

            Log.e("CheckVideo", "❌ Could not get real path from URI: $uri")
            null
        } catch (e: Exception) {
            Log.e("CheckVideo", "❌ Error getting real video file: ${e.message}")
            null
        }
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

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            safeNavigate {
                deleteVideoAndRetry()
            }
        }

        binding.btnDelete.setOnClickListener {
            safeNavigate {
                sendPracticeWithWorkManager()
            }
        }
    }

    private fun sendPracticeWithWorkManager() {
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

        try {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            val practice = Practice(
                uid = uid,
                practiceId = 0,
                date = currentDate,
                time = currentTime,
                scale = escalaName!!,
                scaleType = ScaleType.fromName(escalaName!!).displayName,
                duration = videoDurationSeconds,
                bpm = bpm ?: 0,
                figure = figure ?: 0.0,
                octaves = octaves ?: 0,
                videoLocalRoute = videoFile.absolutePath
            )

            Log.d("CheckVideo", "📤 Scheduling upload with original file")
            Log.d("CheckVideo", "📁 Original path: ${videoFile.absolutePath}")
            Log.d("CheckVideo", "📁 File size: ${videoFile.length() / 1024}KB")
            Log.d("CheckVideo", "📁 File exists: ${videoFile.exists()}")

            workId = videoUploadManager.scheduleVideoUpload(
                practice = practice,
                videoUri = videoUri!!
            )

            Log.d("CheckVideo", "✅ Upload scheduled with ID: $workId")
            Log.d("CheckVideo", "📌 Video will remain at: ${videoFile.absolutePath}")

            workId?.let { id ->
                observeUploadProgress(id)
            }

            showSuccess("✅ Video programado para subida")

            binding.root.postDelayed({
                if (isFragmentActive) {
                    returnToPracticeFragmentAfterScheduling()
                }
            }, 1000)

        } catch (e: Exception) {
            Log.e("CheckVideo", "❌ Error scheduling upload: ${e.message}", e)
            showError("Error al programar la subida: ${e.message}")
        }
    }

    private fun observeUploadProgress(workId: java.util.UUID) {
        videoUploadManager.observeWorkStatus(workId).observe(viewLifecycleOwner) { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.ENQUEUED -> {
                    Log.d("CheckVideo", "⏳ Upload enqueued: ${workInfo.id}")
                }
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getInt("progress", 0)
                    val message = workInfo.progress.getString("message") ?: ""
                    Log.d("CheckVideo", "🔄 Upload progress: $progress% - $message")
                }
                WorkInfo.State.SUCCEEDED -> {
                    val practiceId = workInfo.outputData.getInt("practice_id", 0)
                    val attempts = workInfo.outputData.getInt("attempts", 1)
                    Log.d("CheckVideo", "✅ Upload completed: Practice #$practiceId (after $attempts attempts)")
                    videoUploadManager.cleanupCompletedWork(workId)
                }
                WorkInfo.State.FAILED -> {
                    val error = workInfo.outputData.getString("error") ?: "Error desconocido"
                    val attempts = workInfo.outputData.getInt("attempts", 1)
                    Log.e("CheckVideo", "❌ Upload failed after $attempts attempts: $error")
                }
                WorkInfo.State.BLOCKED -> {
                    Log.d("CheckVideo", "🚫 Upload blocked (waiting for network)")
                }
                WorkInfo.State.CANCELLED -> {
                    Log.d("CheckVideo", "🛑 Upload cancelled")
                }
            }
        }
    }

    private fun deleteVideoAndRetry() {
        workId?.let { id ->
            videoUploadManager.cancelWork(id)
            workId = null
        }

        videoUri?.let { uri ->
            try {
                val deleted = requireContext().contentResolver.delete(uri, null, null)
                if (deleted > 0) {
                    Log.d("VideoPlayback", "✅ Video deleted from gallery for retry")
                    if (isFragmentActive) {
                        Toast.makeText(
                            requireContext(),
                            "Video eliminado. Repitiendo práctica...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("VideoPlayback", "❌ Error deleting video: ${e.message}")
            }

            navigateToCalibration()
        }
    }

    private fun navigateToCalibration() {
        val calibrationFragment = CalibrateCameraFragment().apply {
            arguments = Bundle().apply {
                putString("escalaName", escalaName)
                putInt("escalaNotes", escalaNotes ?: 0)
                putInt("octaves", octaves ?: 0)
                putInt("bpm", bpm ?: 0)
                putDouble("figure", figure ?: 0.0)
                putString("escala_data", escalaData)
            }
        }

        navigateAndClearStack(calibrationFragment, R.id.fragment_container)
    }

    private fun showError(message: String) {
        if (!isFragmentActive) return
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        if (!isFragmentActive) return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun returnToPracticeFragmentAfterScheduling() {
        safeNavigate {
            (activity as? HomeActivity)?.disableFullscreen()
            (activity as? HomeActivity)?.showBottomNavigation()

            val practiceFragment = PracticeFragment()
            navigateAndClearStack(practiceFragment, R.id.fragment_container)
            (activity as? HomeActivity)?.returnToMainNavigation(practiceFragment)

            Log.d("CheckVideo", "📱 Video kept for upload - will be deleted after successful upload")
        }
    }

    private fun returnToPracticeFragment() {
        safeNavigate {
            (activity as? HomeActivity)?.disableFullscreen()
            (activity as? HomeActivity)?.showBottomNavigation()

            val practiceFragment = PracticeFragment()
            navigateAndClearStack(practiceFragment, R.id.fragment_container)
            (activity as? HomeActivity)?.returnToMainNavigation(practiceFragment)

            Log.d("CheckVideo", "📱 Returned to PracticeFragment")
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

        backPressedCallback?.remove()
        backPressedCallback = null

        if (_binding != null) {
            binding.videoView.stopPlayback()
        }

        Log.d("CheckVideo", "🏠 CheckVideo destroyed")

        _binding = null
    }
}