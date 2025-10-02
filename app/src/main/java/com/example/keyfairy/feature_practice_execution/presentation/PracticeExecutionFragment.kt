package com.example.keyfairy.feature_practice_execution.presentation

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.view.Surface // ← AGREGAR

class PracticeExecutionFragment : Fragment() {

    private var escalaName: String? = null
    private var escalaNotes: Int? = null
    private var octaves: Int? = null
    private var bpm: Int? = null
    private var videoLength: Long = 0

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    private lateinit var soundPool: SoundPool
    private val soundIds = mutableMapOf<String, Int>()

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var isRecordingScheduled = false

    private var hasNavigated = false

    private var msPerTick: Long = 0
    private var repeatingSoundHandler: Handler? = null
    private var repeatingSoundRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let { bundle ->
            escalaName = bundle.getString("escalaName")
            escalaNotes = bundle.getInt("escalaNotes")
            octaves = bundle.getInt("octaves")
            bpm = bundle.getInt("bpm")
        }
        return inflater.inflate(R.layout.fragment_practice_execution, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hasNavigated = false
        isRecordingScheduled = false

        setupAudio()
        calculateVideoLength()
        setupCamera()

        if (hasRequiredPermissions()) {
            startCamera()
        } else {
            requestRequiredPermissions()
        }
    }

    private fun setupAudio() {
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        preloadSounds()
    }

    private fun calculateVideoLength() {
        val secondsPerNote = (60 / (bpm ?: 120).toDouble())
        msPerTick = (secondsPerNote * 1000).toLong()
        val numberOfNotes = (((escalaNotes ?: 8) - 1) * 2) * (octaves ?: 1) + 1
        videoLength = ((secondsPerNote * numberOfNotes) * 1000).toLong()
        videoLength += (secondsPerNote * 1000).toLong()
        Log.i("VIDEO-LEN", videoLength.toString())
    }

    private fun setupCamera() {
        previewView = requireView().findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun hasRequiredPermissions(): Boolean {
        val cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        return cameraGranted && audioGranted
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), CAMERA_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (allGranted) {
                    startCamera()
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Permisos de cámara y audio requeridos", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun startCamera() {
        if (!isAdded || isDetached || hasNavigated) {
            Log.d("Camera", "Fragment not properly attached or already navigated, skipping camera start")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                if (!isAdded || isDetached || hasNavigated) {
                    Log.d("Camera", "Fragment became inactive during camera setup")
                    return@addListener
                }

                cameraProvider = cameraProviderFuture.get()

                // ✅ SOLUCIÓN: Configurar preview con orientación landscape
                val preview = Preview.Builder()
                    .setTargetRotation(Surface.ROTATION_90) // ← Forzar landscape
                    .build()
                    .also {
                        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER // ← Cambiar a FILL_CENTER
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // ✅ SOLUCIÓN: Configurar recorder con orientación landscape
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()

                videoCapture = VideoCapture.Builder(recorder)
                    .setTargetRotation(Surface.ROTATION_90) // ← Forzar landscape en grabación
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, videoCapture)

                Log.d("Camera", "Camera successfully bound to lifecycle with landscape orientation")

                scheduleRecordingSequence()

            } catch (exc: Exception) {
                Log.e("Camera", "Camera initialization failed", exc)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error al inicializar cámara: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun scheduleRecordingSequence() {
        if (isRecordingScheduled || hasNavigated) {
            Log.d("Recording", "Recording already scheduled or navigated away")
            return
        }

        isRecordingScheduled = true

        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded && !hasNavigated) {
                playSound("countdown")
            }
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded && !hasNavigated && isRecordingScheduled) {
                startRecording(videoLength)
                startMetronome(msPerTick)
            }
        }, 7000)
    }

    private fun startRecording(durationMillis: Long = 30000) {
        if (!isAdded || hasNavigated) {
            Log.d("Recording", "Fragment not attached or already navigated, skipping recording")
            return
        }

        if (!hasRequiredPermissions()) {
            Log.e("Permissions", "Cannot start recording - missing permissions")
            if (isAdded) {
                Toast.makeText(requireContext(), "Permisos de cámara y audio requeridos", Toast.LENGTH_SHORT).show()
            }
            return
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
            recording = videoCapture.output
                .prepareRecording(requireContext(), mediaStoreOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                    handleRecordEvent(recordEvent)
                }
        } catch (e: SecurityException) {
            Log.e("Recording", "SecurityException: ${e.message}")
            if (isAdded) {
                Toast.makeText(requireContext(), "Permiso denegado para grabación", Toast.LENGTH_SHORT).show()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            stopRecording()
        }, durationMillis)
    }

    private fun handleRecordEvent(recordEvent: VideoRecordEvent) {
        when (recordEvent) {
            is VideoRecordEvent.Start -> {
                Log.d("VideoRecording", "Recording started")
                if (isAdded) {
                    Toast.makeText(requireContext(), "Grabación iniciada", Toast.LENGTH_SHORT).show()
                }
            }

            is VideoRecordEvent.Finalize -> {
                if (!isAdded || hasNavigated) {
                    Log.d("VideoRecording", "Fragment not attached, skipping finalize processing")
                    return
                }

                if (!recordEvent.hasError()) {
                    val videoUri = recordEvent.outputResults.outputUri
                    Log.d("VideoRecording", "Recording saved: $videoUri")

                    // ✅ SOLUCIÓN: Marcar que ya navegamos ANTES de navegar
                    hasNavigated = true

                    // ✅ SOLUCIÓN: Detener TODO antes de navegar
                    stopCamera()

                    if (isAdded) {
                        Toast.makeText(requireContext(), "Video guardado exitosamente", Toast.LENGTH_SHORT).show()
                        navigateToCheckVideo(videoUri)
                    }
                } else {
                    Log.e("VideoRecording", "Recording error: ${recordEvent.error}")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error en la grabación", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun navigateToCheckVideo(videoUri: android.net.Uri) {
        try {
            val videoDurationSeconds = (videoLength / 1000).toInt()

            val playbackFragment = CheckVideoFragment.newInstance(
                videoUri = videoUri,
                escalaName = escalaName ?: "C Major",
                escalaNotes = escalaNotes ?: 8,
                octaves = octaves ?: 1,
                bpm = bpm ?: 120,
                videoDurationSeconds = videoDurationSeconds
            )

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, playbackFragment)
                .addToBackStack(null)
                .commit()

        } catch (e: Exception) {
            Log.e("Navigation", "Error navigating to CheckVideoFragment: ${e.message}")
        }
    }

    private fun startMetronome(intervalMs: Long) {
        stopRepeatingSound()

        repeatingSoundHandler = Handler(Looper.getMainLooper())
        playSound("metronome_tick")
        repeatingSoundRunnable = object : Runnable {
            override fun run() {
                if (recording != null && isAdded && !hasNavigated) {
                    playSound("metronome_tick")
                    repeatingSoundHandler?.postDelayed(this, intervalMs)
                }
            }
        }
        repeatingSoundHandler?.postDelayed(repeatingSoundRunnable!!, intervalMs)
    }

    private fun stopRepeatingSound() {
        repeatingSoundRunnable?.let {
            repeatingSoundHandler?.removeCallbacks(it)
        }
        repeatingSoundHandler?.removeCallbacksAndMessages(null)
        repeatingSoundHandler = null
        repeatingSoundRunnable = null
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        stopRepeatingSound() // ← Detener metrónomo también
        Log.d("VideoRecording", "Recording stopped")
    }

    private fun playSound(command: String) {
        if (!isAdded || hasNavigated) return

        soundIds[command]?.let { soundId ->
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
            Log.i("PLAYER", "Playing sound for: $command")
        } ?: run {
            Log.w("PLAYER", "Sound not found for command: $command")
        }
    }

    private fun preloadSounds() {
        soundIds["countdown"] = loadSound(R.raw.instruccioncuentaregresivasound)
        soundIds["metronome_tick"] = loadSound(R.raw.metronome_tick)
        Log.i("PLAYER", "All calibration sounds preloaded")
    }

    private fun loadSound(resId: Int): Int {
        return soundPool.load(requireContext(), resId, 1)
    }

    private fun stopCamera() {
        try {
            stopRecording()
            stopRepeatingSound()
            isRecordingScheduled = false
            cameraProvider?.unbindAll()
            cameraProvider = null
            videoCapture = null
            Log.d("Camera", "Camera stopped successfully")
        } catch (e: Exception) {
            Log.e("Camera", "Error stopping camera: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hasNavigated = true
        stopCamera()
        if (this::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        if (this::soundPool.isInitialized) {
            soundPool.release()
        }
    }

    override fun onPause() {
        super.onPause()
        // ✅ SOLUCIÓN: Solo detener si NO hemos navegado (evita re-iniciar en onResume)
        if (!hasNavigated) {
            stopCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        // ✅ SOLUCIÓN: Solo reiniciar si NO hemos navegado
        if (!hasNavigated && hasRequiredPermissions()) {
            startCamera()
        }
    }
}