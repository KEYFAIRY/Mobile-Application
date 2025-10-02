package com.example.keyfairy.feature_calibrate.presentation

import YOLO11Segmentation
import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.keyfairy.R
import com.example.keyfairy.feature_home.presentation.HomeActivity
import com.example.keyfairy.feature_practice_execution.presentation.PracticeExecutionFragment
import com.example.keyfairy.utils.common.BaseFragment
import com.example.keyfairy.utils.common.navigateAndClearStack
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.json.JSONObject

class CalibrateCameraFragment : BaseFragment() {

    // Variables parametro de entrada
    private var escalaName: String? = null
    private var escalaNotes: Int? = null
    private var octaves: Int? = null
    private var bpm: Int? = null
    private var noteType: String? = null
    private var escalaData: String? = null

    private var captureHandler: Handler? = null
    private var captureRunnable: Runnable? = null
    private val CAPTURE_INTERVAL = 1000L
    private var shouldCaptureFrame = false

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    // Variables reproduccion de audio
    private lateinit var soundPool: SoundPool
    private val soundIds = mutableMapOf<String, Int>()

    // Instancia para la segmentacion
    private var segmentation: YOLO11Segmentation? = null
    private var calibratedCounts: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let { bundle ->
            escalaName = bundle.getString("escalaName")
            escalaNotes = bundle.getInt("escalaNotes")
            octaves = bundle.getInt("octaves")
            bpm = bundle.getInt("bpm")
            noteType = bundle.getString("noteType")
            escalaData = bundle.getString("escala_data")
        }
        return inflater.inflate(R.layout.fragment_calibrate_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFullscreenMode()
        initializeComponents(view)
        setupCamera()
    }

    private fun setupFullscreenMode() {
        (activity as? HomeActivity)?.enableFullscreen()
        (activity as? HomeActivity)?.hideBottomNavigation()
    }

    private fun initializeComponents(view: View) {
        segmentation = YOLO11Segmentation(requireContext())
        previewView = view.findViewById(R.id.previewView)

        // SoundPool setup
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        preloadSounds()

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(requireContext()))
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
            startAutomaticCapture()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                    startAutomaticCapture()
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                previewView.scaleType = PreviewView.ScaleType.FILL_START
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis?.setAnalyzer(cameraExecutor) { imageProxy ->
                if (shouldCaptureFrame && isFragmentActive) {
                    processFrame(imageProxy)
                }
                imageProxy.close()
            }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e("CameraFragment", "Camera initialization failed", exc)
                Toast.makeText(requireContext(), "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        val (command, corners) = imageProxyToCalibrationResult(imageProxy)
        view?.post {
            if (corners != null && isFragmentActive) {
                Log.i("PLAYER", command)
                if (command == "calibrado") {
                    Log.i("ATENCION", "CALIBRATION SUCCESS - IMAGE PROCESSED")
                    drawCornersOnOverlay(corners, true)
                    calibratedCounts++
                    Log.i("CUENTA", calibratedCounts.toString())

                    if (calibratedCounts == 4) {
                        handleCalibrationComplete()
                    }
                } else {
                    calibratedCounts = 0
                    drawCornersOnOverlay(corners, false)
                }

                // Play sound feedback
                playSound(command)
            }
        }
        shouldCaptureFrame = false
    }

    private fun handleCalibrationComplete() {
        safeNavigate {
            stopAutomaticCapture()
            stopCamera()
            navigateToPracticeExecution()
        }
    }

    private fun navigateToPracticeExecution() {
        val fragment = PracticeExecutionFragment().apply {
            arguments = Bundle().apply {
                putString("escalaName", escalaName)
                putInt("escalaNotes", escalaNotes ?: 0)
                putInt("octaves", octaves ?: 1)
                putInt("bpm", bpm ?: 120)
                putString("noteType", noteType)
                putString("escala_data", escalaData)
            }
        }

        // Navegación lineal: reemplaza sin back stack
        navigateAndClearStack(fragment, R.id.fragment_container)
    }

    private fun startAutomaticCapture() {
        captureHandler = Handler(Looper.getMainLooper())
        captureRunnable = object : Runnable {
            override fun run() {
                if (isFragmentActive) {
                    shouldCaptureFrame = true
                    captureHandler?.postDelayed(this, CAPTURE_INTERVAL)
                }
            }
        }
        captureHandler?.postDelayed(captureRunnable!!, CAPTURE_INTERVAL)
    }

    private fun stopAutomaticCapture() {
        captureRunnable?.let { captureHandler?.removeCallbacks(it) }
        captureHandler = null
        captureRunnable = null
        shouldCaptureFrame = false
    }

    private fun stopCamera() {
        try {
            imageAnalysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
            imageAnalysis = null
            shouldCaptureFrame = false

            if (this::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) {
                cameraExecutor.shutdownNow()
            }

            Log.d("Camera", "Camera stopped and executor shut down")
        } catch (e: Exception) {
            Log.e("Camera", "Error stopping camera: ${e.message}")
        }
    }

    // Rest of the methods remain the same...
    private fun imageProxyToCalibrationResult(imageProxy: ImageProxy): Pair<String, List<Pair<Int, Int>>?> {
        val image = imageProxy.image ?: return Pair("notCalibrated", null)
        try {
            val pianoAreaSection = requireView().findViewById<FrameLayout>(R.id.drawingContainer)
            val yBuffer = image.planes[0].buffer
            val vuBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val vuSize = vuBuffer.remaining()

            val nv21 = ByteArray(ySize + vuSize)
            yBuffer.get(nv21, 0, ySize)
            vuBuffer.get(nv21, ySize, vuSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val heightToWidthRatio = image.height/image.width.toFloat()
            val scalingRatio = previewView.width / 608f
            val phonePreviewTotalHeight = (previewView.width * image.height) / image.width.toFloat()
            val frameCapturedPianoAreaPercentage = pianoAreaSection.height / phonePreviewTotalHeight

            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
            val imageBytes = out.toByteArray()

            val py = Python.getInstance()
            val module = py.getModule("calibracion")

            val resultBitmap = segmentation!!.getPianoKeysFromImage(imageBytes, frameCapturedPianoAreaPercentage)
            val rsp = module.callAttr("is_calibrated", resultBitmap, frameCapturedPianoAreaPercentage, heightToWidthRatio, context).toString()

            val json = JSONObject(rsp)
            val command = json.getString("command")
            val cornersJson = json.optJSONArray("corners")
            val corners = cornersJson?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val point = arr.optJSONArray(i)
                    if (point != null && point.length() == 2) {
                        var x = point.optInt(0)
                        var y = point.optInt(1)
                        x = (x * scalingRatio).toInt()
                        y = (y * scalingRatio).toInt()
                        Pair(x, y)
                    } else null
                }
            }
            return Pair(command, corners)
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Error processing image: ${e.message}")
            return Pair("notCalibrated", null)
        }
    }

    private fun drawCornersOnOverlay(corners: List<Pair<Int, Int>>, isCalibrated: Boolean) {
        if (!isFragmentActive) return

        val overlay = view?.findViewById<FrameLayout>(R.id.drawingContainer) ?: return
        overlay.removeAllViews()
        val customView = object : View(requireContext()) {
            private val paint = Paint().apply {
                color = if (isCalibrated) Color.GREEN else Color.RED
                style = Paint.Style.FILL
                strokeWidth = 16f
            }
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                for ((x, y) in corners) {
                    canvas.drawCircle(x.toFloat(), y.toFloat(), 20f, paint)
                }
            }
        }
        overlay.addView(customView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
    }

    private fun loadSound(resId: Int): Int {
        return soundPool.load(requireContext(), resId, 1)
    }

    private fun playSound(command: String) {
        if (!isFragmentActive) return

        soundIds[command]?.let { soundId ->
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
            Log.i("PLAYER", "Playing sound for: $command")
        } ?: run {
            Log.w("PLAYER", "Sound not found for command: $command")
        }
    }

    private fun preloadSounds() {
        soundIds["arriba"] = loadSound(R.raw.arribacalibrationsound)
        soundIds["izquierda"] = loadSound(R.raw.izquierdacalibrationsound)
        soundIds["derecha"] = loadSound(R.raw.derechacalibrationsound)
        soundIds["adelante"] = loadSound(R.raw.adelantecalibrationsound)
        soundIds["atras"] = loadSound(R.raw.atrascalibrationsound)
        soundIds["r_derecha"] = loadSound(R.raw.rotaderechacalibrationsound)
        soundIds["r_izquierda"] = loadSound(R.raw.rotaizquierdacalibrationsound)
        soundIds["calibrado"] = loadSound(R.raw.calibradocalibrationsound)

        Log.i("PLAYER", "All calibration sounds preloaded")
    }

    override fun onResume() {
        super.onResume()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun onPause() {
        super.onPause()
        stopAutomaticCapture()
        stopCamera()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onStop() {
        super.onStop()
        stopAutomaticCapture()
        stopCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
        stopAutomaticCapture()

        if (::soundPool.isInitialized) {
            soundPool.release()
        }

        if (this::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }

        cameraProvider = null
        imageAnalysis = null
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}