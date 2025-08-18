package com.example.keyfairy.feature_calibrate.presentation

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.keyfairy.R
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.core.view.doOnLayout
import org.json.JSONObject

class CalibrateCameraFragment : Fragment() {

    private var captureHandler: Handler? = null
    private var captureRunnable: Runnable? = null
    private val CAPTURE_INTERVAL = 3000L // 5 seconds

    private var capturedBitmap: Bitmap? = null
    private var shouldCaptureFrame = false




    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    //   El recuadro de camara de donde se capturan los frames
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var drawingContainer: FrameLayout






    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_calibrate_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(requireContext()))
        }
//        val py = Python.getInstance()
//        val cv_module = py.getModule("calibracion")


        cameraExecutor = Executors.newSingleThreadExecutor()

        // Revisar permiso de camara
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()

//      Una vez se confirma el permiso de la camara se empieza la toma de frames.
            startAutomaticCapture()

        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        }





        val drawingContainer = view.findViewById<FrameLayout>(R.id.drawingContainer)
        val customView = object : View(requireContext()) {
            private val paint = Paint().apply {
                color = Color.RED
                strokeWidth = 16f
                style = Paint.Style.STROKE
            }
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                // Example: draw a diagonal line
                canvas.drawLine(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }
        drawingContainer.addView(customView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))



    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // ImageAnalysis for frame capture
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (shouldCaptureFrame) {
                    val (success, corners) = imageProxyToCalibrationResult(imageProxy) // Now returns Pair<Boolean, List<Pair<Int, Int>>?>
                    view?.post {
                        if (success && corners != null) {
                            Log.i("ATENCION", "CALIBRATION SUCCESS - IMAGE PROCESSED")
                            Log.i("ATENCION", corners.toString())
                            drawCornersOnOverlay(corners) // Draw the corners if you want
                        } else {
                            Log.i("ATENCION", "CALIBRATION FAILED - IMAGE NOT CALIBRATED")
                            drawCornersOnOverlay(emptyList()) // Optionally clear overlay
                        }
                    }
                    shouldCaptureFrame = false
                }
                imageProxy.close()
            }

            try {
                cameraProvider.unbindAll()
                // FIXED: Bind both preview AND imageAnalysis
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e("CameraFragment", "Camera initialization failed", exc)
                Toast.makeText(requireContext(), "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startAutomaticCapture() {
        captureHandler = Handler(Looper.getMainLooper())
        captureRunnable = object : Runnable {
            override fun run() {
                shouldCaptureFrame = true
                captureHandler?.postDelayed(this, CAPTURE_INTERVAL)
            }
        }
        captureHandler?.postDelayed(captureRunnable!!, CAPTURE_INTERVAL)
    }


    private fun imageProxyToCalibrationResult(imageProxy: ImageProxy): Pair<Boolean, List<Pair<Int, Int>>?> {
        val image = imageProxy.image ?: return Pair(false, null)
        try {
            val yBuffer = image.planes[0].buffer // Y
            val vuBuffer = image.planes[2].buffer // VU

            val ySize = yBuffer.remaining()
            val vuSize = vuBuffer.remaining()

            val nv21 = ByteArray(ySize + vuSize)
            yBuffer.get(nv21, 0, ySize)
            vuBuffer.get(nv21, ySize, vuSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            val imageBytes = out.toByteArray()

            val py = Python.getInstance()
            val module = py.getModule("calibracion")
            val rsp = module.callAttr("is_calibrated", imageBytes).toString() // Get JSON string

            val json = JSONObject(rsp)
            val success = json.getBoolean("success")
            val cornersJson = json.optJSONArray("corners")
            val corners = cornersJson?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val point = arr.optJSONArray(i)
                    if (point != null && point.length() == 2) {
                        val x = point.optInt(0)
                        val y = point.optInt(1)
                        Pair(x, y)
                    } else null
                }
            }
            return Pair(success, corners)
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Error processing image: ${e.message}")
            return Pair(false, null)
        }
    }

    private fun drawCornersOnOverlay(corners: List<Pair<Int, Int>>) {
        val overlay = view?.findViewById<FrameLayout>(R.id.drawingContainer) ?: return
        overlay.removeAllViews()
        val customView = object : View(requireContext()) {
            private val paint = Paint().apply {
                color = Color.RED
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

    private fun stopAutomaticCapture() {
        captureRunnable?.let { captureHandler?.removeCallbacks(it) }
        captureHandler = null
        captureRunnable = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::cameraExecutor.isInitialized) {
            stopAutomaticCapture()
            cameraExecutor.shutdown()
        }
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onResume() {
        super.onResume()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    }
}

