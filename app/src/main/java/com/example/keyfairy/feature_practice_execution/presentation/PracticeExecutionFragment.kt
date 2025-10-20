package com.example.keyfairy.feature_practice_execution.presentation

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.keyfairy.R
import com.example.keyfairy.feature_check_video.presentation.fragment.CheckVideoFragment
import com.example.keyfairy.feature_home.presentation.HomeActivity
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.goBack
import com.example.keyfairy.utils.common.navigateAndClearStack
import kotlinx.coroutines.Runnable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PracticeExecutionFragment : BaseFragment() {

    private var escalaName: String? = null
    private var escalaNotes: Int? = null
    private var octaves: Int? = null
    private var bpm: Int? = null
    private var figure: Double? = null
    private var escalaData: String? = null
    private var videoLength: Long = 0

    private lateinit var soundPool: SoundPool
    private val soundIds = mutableMapOf<String, Int>()

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var isRecordingScheduled = false
    private var hasCompletedRecording = false
    private val scheduleHandler = Handler(Looper.getMainLooper())
    private val scheduledRunnables = mutableListOf<Runnable>()

    private var msPerTick: Long = 0
    private var repeatingSoundHandler: Handler? = null
    private var repeatingSoundRunnable: Runnable? = null
    private var activeStreamId: Int? = null
    private var metronomeBeatCount = 0L
    private var metronomeStartTime = 0L
    private var recordingStartTime = 0L
    private var durationCheckHandler: Handler? = null
    private var durationCheckRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let { bundle ->
            escalaName = bundle.getString("escalaName")
            escalaNotes = bundle.getInt("escalaNotes")
            octaves = bundle.getInt("octaves")
            bpm = bundle.getInt("bpm")
            figure = bundle.getDouble("figure")
            escalaData = bundle.getString("escala_data")
        }
        return inflater.inflate(R.layout.fragment_practice_execution, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFullscreenMode()

        // Verificar permisos antes de continuar
        if (!checkRequiredPermissions()) {
            handleMissingPermissions()
            return
        }

        initializeComponents()
        startCamera()
    }

    private fun setupFullscreenMode() {
        (activity as? HomeActivity)?.enableFullscreen()
        (activity as? HomeActivity)?.hideBottomNavigation()
    }

    private fun checkRequiredPermissions(): Boolean {
        val cameraGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val audioGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        Log.d("PracticeExecutionFragment", "Permissions check - Camera: $cameraGranted, Audio: $audioGranted")

        return cameraGranted && audioGranted
    }

    private fun handleMissingPermissions() {
        Log.w("PracticeExecutionFragment", "Missing required permissions - returning to previous fragment")

        Toast.makeText(
            requireContext(),
            "Se requieren permisos de cámara y audio. Por favor, concédelos desde la pantalla de inicio.",
            Toast.LENGTH_LONG
        ).show()

        // Regresar al fragmento anterior después de 2 segundos
        view?.postDelayed({
            if (isFragmentActive) {
                requireActivity().onBackPressed()
            }
        }, 2000)
    }

    private fun initializeComponents() {
        hasCompletedRecording = false
        isRecordingScheduled = false

        setupAudio()
        calculateVideoLength()
        setupCamera()
    }

    private fun setupAudio() {
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        preloadSounds()
    }

    private fun calculateVideoLength() {
        val secondsPerNote = (60 / (bpm ?: 120).toDouble())

        msPerTick = (secondsPerNote * 1000).toLong()

        val numberOfNotes = (((escalaNotes ?: 8) - 1) * 2) * (octaves ?: 1) + 1
        videoLength = ((secondsPerNote * numberOfNotes * figure!!) * 1000).toLong()

        // Se suma la duracion de un tick adicional para prevenir cortes justo en la nota final
        videoLength += (secondsPerNote * 1000).toLong()
        Log.i("VIDEO-LEN2", videoLength.toString())
        Log.i("NOTES", escalaNotes.toString())
    }

    private fun setupCamera() {
        previewView = requireView().findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        if (!isFragmentActive || hasNavigatedAway || hasCompletedRecording) {
            Log.d("Camera", "Fragment not active or already completed, skipping camera start")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                if (!isFragmentActive || hasNavigatedAway) {
                    Log.d("Camera", "Fragment became inactive during camera setup")
                    return@addListener
                }

                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .setTargetRotation(Surface.ROTATION_90)
                    .build()
                    .also {
                        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()

                videoCapture = VideoCapture.Builder(recorder)
                    .setTargetRotation(Surface.ROTATION_90)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, videoCapture)

                Log.d("Camera", "Camera successfully bound to lifecycle with landscape orientation")

                scheduleRecordingSequence()

            } catch (exc: Exception) {
                Log.e("Camera", "Camera initialization failed", exc)
                if (isFragmentActive) {
                    Toast.makeText(
                        requireContext(),
                        "Error al inicializar cámara: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun scheduleRecordingSequence() {
        if (isRecordingScheduled || hasNavigatedAway || hasCompletedRecording) return
        isRecordingScheduled = true

        val countdownRunnable = Runnable {
            if (isFragmentActive && !hasNavigatedAway) playSound("countdown")
        }
//        val startMetronomeRunnable = Runnable {
//            if (isFragmentActive && !hasNavigatedAway) {
//                startMetronome(msPerTick)
//            }
//        }
        val startRecordingRunnable = Runnable {
            if (isFragmentActive && !hasNavigatedAway && isRecordingScheduled) {
                startRecording(videoLength)
            }
        }
//        scheduledRunnables.add(startMetronomeRunnable)
        scheduledRunnables.add(countdownRunnable)
        scheduledRunnables.add(startRecordingRunnable)

        scheduleHandler.postDelayed(countdownRunnable, 1000)
//        scheduleHandler.postDelayed(startMetronomeRunnable, 7010)
        scheduleHandler.postDelayed(startRecordingRunnable, 6900)
    }
    private fun cancelScheduledTasks() {
        scheduledRunnables.forEach { scheduleHandler.removeCallbacks(it) }
        scheduledRunnables.clear()
    }

    private fun startRecording(durationMillis: Long = 30000) {

        if (!isFragmentActive || hasNavigatedAway || hasCompletedRecording) {
            Log.d("Recording", "Fragment not active, skipping recording")
            return
        }

        if (recording != null) {
            Log.w("Recording", "Recording already active, stopping before starting new one")
            stopRecording()
        }

        val videoCapture = this.videoCapture ?: return

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "video_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/KeyFairy")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(requireContext().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        try {
            recordingStartTime = System.currentTimeMillis()

            recording = videoCapture.output
                .prepareRecording(requireContext(), mediaStoreOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                    handleRecordEvent(recordEvent)
                }

            // Start checking duration with high frequency
            startDurationCheck(durationMillis + 500)

        } catch (e: SecurityException) {
            Log.e("Recording", "SecurityException: ${e.message}")
            if (isFragmentActive) {
                Toast.makeText(
                    requireContext(),
                    "Permiso denegado para grabación",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startDurationCheck(targetDurationMillis: Long) {
        durationCheckHandler = Handler(Looper.getMainLooper())
        durationCheckRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - recordingStartTime

                if (elapsed >= targetDurationMillis) {
                    if (isFragmentActive && !hasNavigatedAway) {
                        stopRecording()
                    }
                } else {
                    // Check every 16ms (~60fps) for precise stopping
                    durationCheckHandler?.postDelayed(this, 16)
                }
            }
        }
        durationCheckHandler?.post(durationCheckRunnable!!)
    }

    private fun handleRecordEvent(recordEvent: VideoRecordEvent) {
        when (recordEvent) {
            is VideoRecordEvent.Start -> {
                requireActivity().runOnUiThread {
                    startMetronome(msPerTick)
                }

                Log.d("VideoRecording", "Recording started")
                if (isFragmentActive) {
                    Toast.makeText(
                        requireContext(),
                        "Grabación iniciada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            is VideoRecordEvent.Finalize -> {
                recording = null
                if (!isFragmentActive || hasNavigatedAway) {
                    Log.d("VideoRecording", "Fragment not active, skipping finalize processing")
                    return
                }

                if (!recordEvent.hasError()) {
                    val videoUri = recordEvent.outputResults.outputUri
                    Log.d("VideoRecording", "Recording saved: $videoUri")

                    hasCompletedRecording = true
                    stopCamera()

                    if (isFragmentActive) {
                        Toast.makeText(
                            requireContext(),
                            "Video guardado exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToCheckVideo(videoUri)
                    }
                } else {
                    Log.e("VideoRecording", "Recording error: ${recordEvent.error}")
                    if (isFragmentActive) {
                        Toast.makeText(
                            requireContext(),
                            "Error en la grabación",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun navigateToCheckVideo(videoUri: Uri) {
        safeNavigate {
            stopCamera()
            stopRepeatingSound()

            val videoDurationSeconds = (videoLength / 1000).toInt()

            val playbackFragment = CheckVideoFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("video_uri", videoUri)
                    putString("escalaName", escalaName ?: "C Major")
                    putInt("escalaNotes", escalaNotes ?: 0)
                    putInt("octaves", octaves ?: 0)
                    putInt("bpm", bpm ?: 0)
                    putInt("videoDuration", videoDurationSeconds)
                    putDouble("figure", figure ?: 0.0)
                    putString("escala_data", escalaData)
                }
            }

            navigateAndClearStack(playbackFragment, R.id.fragment_container)
        }
    }

    private fun startMetronome(intervalMs: Long) {
        stopRepeatingSound()

        repeatingSoundHandler = Handler(Looper.getMainLooper())

        metronomeBeatCount = 0L
        metronomeStartTime = SystemClock.elapsedRealtime()

        playSound("metronome_tick")

        metronomeBeatCount++

        repeatingSoundRunnable = object : Runnable {
            override fun run() {
                if (recording != null && isFragmentActive && !hasNavigatedAway) {
                    // Play the current tick sound
                    playSound("metronome_tick")

                    metronomeBeatCount++

                    val targetTime = metronomeStartTime + (metronomeBeatCount * intervalMs)

                    val currentTime = SystemClock.elapsedRealtime()

                    val actualDelay = (targetTime - currentTime).coerceAtLeast(0)

                    repeatingSoundHandler?.postDelayed(this, actualDelay)
                }
            }
        }
        repeatingSoundHandler?.postDelayed(repeatingSoundRunnable!!, intervalMs)
    }

    private fun stopRepeatingSound() {
        // Cancela el handler
        repeatingSoundRunnable?.let {
            repeatingSoundHandler?.removeCallbacks(it)
        }
        repeatingSoundHandler?.removeCallbacksAndMessages(null)
        repeatingSoundHandler = null
        repeatingSoundRunnable = null

        // Detiene cualquier stream activo
        activeStreamId?.let {
            soundPool.stop(it)
            Log.d("PLAYER", "Stopped active sound stream $it")
            activeStreamId = null
        }
    }

    private fun stopRecording() {

        durationCheckRunnable?.let {
            durationCheckHandler?.removeCallbacks(it)
        }
        durationCheckHandler = null
        durationCheckRunnable = null
        recording?.stop()
        recording = null
        stopRepeatingSound()
        Log.d("VideoRecording", "Recording stopped")
    }

    private fun playSound(command: String) {
        if (!isFragmentActive || hasNavigatedAway) return

        soundIds[command]?.let { soundId ->
            val streamId = soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
            if (streamId != 0) {
                activeStreamId = streamId
            }
            Log.i("PLAYER", "Playing sound for: $command")
        } ?: run {
            Log.w("PLAYER", "Sound not found for command: $command")
        }
    }

    private fun preloadSounds() {
        soundIds["countdown"] = loadSound(R.raw.instruccioncuentaregresivasound)
        soundIds["metronome_tick"] = loadSound(R.raw.metronome_tick)
        Log.i("PLAYER", "All sounds preloaded")
    }

    private fun loadSound(resId: Int): Int {
        return soundPool.load(requireContext(), resId, 1)
    }

    private fun stopCamera() {
        try {
            stopRecording()
            stopRepeatingSound()
            recording = null
            isRecordingScheduled = false
            cameraProvider?.unbindAll()
            cameraProvider = null
            videoCapture = null
            Log.d("Camera", "Camera stopped successfully")
        } catch (e: Exception) {
            Log.e("Camera", "Error stopping camera: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        if (!hasNavigatedAway && !hasCompletedRecording) {
            stopCamera()
            stopRepeatingSound()
            cancelScheduledTasks()

            safeNavigate {
                goBack()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFragmentActive &&
            !hasNavigatedAway &&
            !hasCompletedRecording &&
            checkRequiredPermissions() &&
            !isRecordingScheduled) {
            Log.d("Camera", "Restarting camera in onResume")
            startCamera()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()

        if (this::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }

        if (this::soundPool.isInitialized) {
            soundPool.release()
        }

        hasCompletedRecording = false
        isRecordingScheduled = false
    }
}