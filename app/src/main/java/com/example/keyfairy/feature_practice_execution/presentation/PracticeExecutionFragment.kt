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
import androidx.annotation.RequiresPermission
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PracticeExecutionFragment : Fragment() {

    // Variables parametro de entrada, datos que el usuario selecciono antes de iniciar la practica
    private var escalaName: String? = null
    private var escalaNotes: Int? = null
    private var octaves: Int? = null
    private var bpm: Int? = null

    // Variable que contendra la duracion que tendra el video
    private var videoLength: Long = 0

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val STORAGE_PERMISSION_REQUEST = 101
    }

    // Variables reproduccion de audio
    private lateinit var soundPool: SoundPool
    private val soundIds = mutableMapOf<String, Int>()

    // Camera components
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    // Video recording components
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
        // Determina si ya se esta grabando un video
    private var isRecordingScheduled = false

    // Metronome playing variables
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

        // SoundPool encargado de ejecutar sonidos cortos
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        // Preload all sounds after the view is created
        preloadSounds()


        val secondsPerNote = (60/(bpm!!).toDouble())
        msPerTick = (secondsPerNote * 1000).toLong()
        // Multiplicamos la cantidad de notas x2, debido a que es ascendente y descendente
        val numberOfNotes = (((escalaNotes!!-1) * 2) * octaves!!) + 1

        videoLength = ((secondsPerNote * numberOfNotes) * 1000).toLong()
        // Para evitar cortes del video que interrumpan una nota, agregamos la duracion de una nota segun el bpm
        videoLength += (secondsPerNote * 1000).toLong()
        Log.i("VIDEO-LEN", videoLength.toString())

        // Initialize UI components
        previewView = view.findViewById(R.id.previewView)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check both camera and storage permissions
        if (hasRequiredPermissions()) {
            startCamera()
        } else {
            requestRequiredPermissions()
        }
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
                    val deniedPermissions = mutableListOf<String>()
                    permissions.forEachIndexed { index, permission ->
                        if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                            when (permission) {
                                Manifest.permission.CAMERA -> deniedPermissions.add("Camera")
                                Manifest.permission.RECORD_AUDIO -> deniedPermissions.add("Audio")
                            }
                        }
                    }
                    Toast.makeText(requireContext(), "${deniedPermissions.joinToString(" and ")} permission(s) required for video recording", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startCamera() {
        // Check if fragment is still attached
        if (!isAdded || isDetached) {
            Log.d("Camera", "Fragment not properly attached, skipping camera start")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                // Double-check fragment state
                if (!isAdded || isDetached) {
                    Log.d("Camera", "Fragment became inactive during camera setup")
                    return@addListener
                }

                // Get camera provider
                cameraProvider = cameraProviderFuture.get()

                // Build preview use case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        previewView.scaleType = PreviewView.ScaleType.FILL_START
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Build video capture use case
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()

                videoCapture = VideoCapture.withOutput(recorder)

                // Select back camera as default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera (now includes video capture)
                val camera = cameraProvider?.bindToLifecycle(
                    this, // Fragment lifecycle owner
                    cameraSelector,
                    preview,
                    videoCapture
                )

                Log.d("CameraInsana", "Camera successfully bound to lifecycle with video recording: ${camera != null}")
                Handler(Looper.getMainLooper()).postDelayed({
                    playSound("countdown")
                }, 1000) // 1000ms delay to ensure sound is loaded
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isRecordingScheduled) {
                        isRecordingScheduled = true
                        startRecording(videoLength)
                        startMetronome(msPerTick)
                    }
                }, 7000)  // 7000ms delay duracion del audio de cuenta regresiva


            } catch (exc: Exception) {
                Log.e("Camera", "Camera initialization failed", exc)
                Toast.makeText(requireContext(), "Camera initialization failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))

    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(durationMillis: Long = 30000) { // Default 30 seconds
        val videoCapture = this.videoCapture ?: return

        // Create output file
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

        // Start recording
        recording = videoCapture.output
            .prepareRecording(requireContext(), mediaStoreOutputOptions)
            .withAudioEnabled()  // Add this line to enable audio
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Log.d("VideoRecording", "Recording started")
                        Toast.makeText(requireContext(), "Recording started", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Log.d("VideoRecording", "Recording saved: ${recordEvent.outputResults.outputUri}")
                            Toast.makeText(requireContext(), "Video saved successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("VideoRecording", "Recording error: ${recordEvent.error}")
                            Toast.makeText(requireContext(), "Recording failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

        // Auto-stop recording after specified duration
        Handler(Looper.getMainLooper()).postDelayed({
            stopRecording()
        }, durationMillis)
    }

    private fun startMetronome(intervalMs: Long) {
        stopRepeatingSound() // Stop any existing repeating sound

        repeatingSoundHandler = Handler(Looper.getMainLooper())
        playSound("metronome_tick")
        repeatingSoundRunnable = object : Runnable {
            override fun run() {
                if (recording != null) { // Only play while recording
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

    fun stopRecording() {
        recording?.stop()
        recording = null
        Log.d("VideoRecording", "Recording stopped")
    }


    fun playSound(command: String) {
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
    fun loadSound(resId: Int): Int {
        return soundPool.load(requireContext(), resId, 1)
    }

    private fun stopCamera() {
        try {
            stopRecording()
            isRecordingScheduled = false // Reset flag
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
        stopCamera()
        if (this::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    override fun onResume() {
        super.onResume()
        // Restart camera if we have permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }
}