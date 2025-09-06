import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.nio.FloatBuffer
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class YOLO11Segmentation(private val context: Context) {

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null

    private val modelSize = 608               // model input W (=608)
    private val confThreshold = 0.7f
    private val iouThreshold = 0.5f
    private val maskH = 152                   // prototype H
    private val maskW = 152                   // prototype W
    private val numMasks = 32                 // prototype channels

    data class Detection(
        val bbox: FloatArray,    // [x1, y1, x2, y2] in original image coordinates
        val confidence: Float,
        val maskCoeffs: FloatArray
    )

    init {
        loadModel()
    }

    private fun loadModel(): Boolean {
        return try {
            val modelBytes = context.assets.open("yolo11s-seg-keys.onnx").use { it.readBytes() }
            ortSession = ortEnv.createSession(modelBytes, OrtSession.SessionOptions())
            println("ONNX model loaded successfully")
            true
        } catch (e: Exception) {
            println("Error loading ONNX model: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Preprocess: letterbox-like: scale image to width=modelSize, keep aspect, pad bottom if needed (same as Python).
     * Returns FloatBuffer (NCHW) and pair(originalWidth, originalHeight).
     */
    private fun preprocessImage(bitmap: Bitmap): Pair<FloatBuffer, Pair<Int, Int>> {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        println("Original image size: ${originalWidth}x${originalHeight}")
        println("Target model size: ${modelSize}x${modelSize}")

        // Calculate scaling factor to fit image within target size while maintaining aspect ratio
        val scale = minOf(
            modelSize.toFloat() / originalWidth,
            modelSize.toFloat() / originalHeight
        )

        // Calculate new dimensions after scaling
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()

        println("Scaled dimensions: ${scaledWidth}x${scaledHeight}")

        // First, resize the bitmap maintaining aspect ratio
        val scaledBitmap = bitmap.scale(scaledWidth, scaledHeight, true)

        // Create a new bitmap with target size (modelSize x modelSize) and black background
        val paddedBitmap = createBitmap(modelSize, modelSize)
        val canvas = android.graphics.Canvas(paddedBitmap)

        // Fill with black background
        canvas.drawColor(Color.BLACK)

        // Calculate position to center the scaled image
        val left = (modelSize - scaledWidth) / 2f
        val top = (modelSize - scaledHeight) / 2f

        println("Padding - left: $left, top: $top")

        // Draw the scaled image centered on the black background
        canvas.drawBitmap(scaledBitmap, left, top, null)

        println("Padding Dim: ${canvas.width}x${canvas.height}")

        // Convert to NCHW float array normalized to [0,1]
        val total = 1 * 3 * modelSize * modelSize
        val floatArr = FloatArray(total)
        val pixels = IntArray(modelSize * modelSize)
        paddedBitmap.getPixels(pixels, 0, modelSize, 0, 0, modelSize, modelSize)

        // fill channel-first: for c in {R,G,B} for y,x
        val HW = modelSize * modelSize
        for (y in 0 until modelSize) {
            for (x in 0 until modelSize) {
                val p = pixels[y * modelSize + x]
                val r = ((p shr 16) and 0xFF) / 255f
                val g = ((p shr 8) and 0xFF) / 255f
                val b = (p and 0xFF) / 255f
                floatArr[0 * HW + y * modelSize + x] = r
                floatArr[1 * HW + y * modelSize + x] = g
                floatArr[2 * HW + y * modelSize + x] = b
            }
        }

        scaledBitmap.recycle()
        paddedBitmap.recycle()

        val fb = FloatBuffer.wrap(floatArr)
        return Pair(fb, Pair(originalWidth, originalHeight))
    }

    private fun preprocessImageTest(bitmap: Bitmap): Bitmap {

        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // scale factor to make width == modelSize
        val scale = modelSize.toFloat() / originalWidth.toFloat()
        val newHeight = (originalHeight * scale).toInt()
        val scaledHeight = newHeight.coerceAtMost(modelSize)

        // resize to (modelSize, scaledHeight)
        val resized = bitmap.scale(modelSize, scaledHeight, true)

        // create padded canvas (modelSize x modelSize), place resized at top-left, pad bottom
        val padded = createBitmap(modelSize, modelSize)
        val canvas = android.graphics.Canvas(padded)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(resized, 0f, 0f, null) // image at top, pad bottom if any

        return padded

    }

    /**
     * Postprocess predictions and mask prototypes (flat arrays).
     * predictions flat layout assumed: shape [C, N] flattened as (C * N) with C=37
     * We'll infer numDetections = preds.size / 37.
     * Returns list of Detection and the maskPrototypes flat array (unchanged).
     */
    private fun postprocessSegmentation(
        predictions: FloatArray,
        maskPrototypes: FloatArray,
        originalWidth: Int,
        originalHeight: Int
    ): Pair<List<Detection>, FloatArray> {
        val channels = 37
        val numDetections = predictions.size / channels
        val detections = mutableListOf<Detection>()

        for (i in 0 until numDetections) {
            val xCenter = predictions[0 * numDetections + i]
            val yCenter = predictions[1 * numDetections + i]
            val width = predictions[2 * numDetections + i]
            val height = predictions[3 * numDetections + i]
            val confidence = predictions[4 * numDetections + i]

            if (confidence > confThreshold) {
                // convert normalized model coords -> original image coords
                val x1 = ((xCenter - width / 2f) * originalWidth / modelSize).toInt().coerceIn(0, originalWidth)
                val y1 = ((yCenter - height / 2f) * originalHeight / modelSize).toInt().coerceIn(0, originalHeight)
                val x2 = ((xCenter + width / 2f) * originalWidth / modelSize).toInt().coerceIn(0, originalWidth)
                val y2 = ((yCenter + height / 2f) * originalHeight / modelSize).toInt().coerceIn(0, originalHeight)

                val coeffs = FloatArray(numMasks)
                for (c in 0 until numMasks) {
                    // mask coeffs are channels 5..36
                    coeffs[c] = predictions[(5 + c) * numDetections + i]
                }

                detections.add(Detection(floatArrayOf(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat()), confidence, coeffs))
            }
        }

        // NMS (same as before)
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<Detection>()
        while (sorted.isNotEmpty()) {
            val first = sorted.removeAt(0)
            kept.add(first)
            val it = sorted.iterator()
            while (it.hasNext()) {
                val other = it.next()
                val iou = calculateIoU(first, other)
                if (iou >= iouThreshold) it.remove()
            }
        }

        println("After NMS: ${kept.size} detections")
        return Pair(kept, maskPrototypes)
    }

    private fun calculateIoU(det1: Detection, det2: Detection): Float {
        val x1 = det1.bbox[0]
        val y1 = det1.bbox[1]
        val x2 = det1.bbox[2]
        val y2 = det1.bbox[3]
        val xa1 = det2.bbox[0]
        val ya1 = det2.bbox[1]
        val xa2 = det2.bbox[2]
        val ya2 = det2.bbox[3]
        val xi1 = max(x1, xa1)
        val yi1 = max(y1, ya1)
        val xi2 = min(x2, xa2)
        val yi2 = min(y2, ya2)
        val inter = max(0f, xi2 - xi1) * max(0f, yi2 - yi1)
        val area1 = (x2 - x1) * (y2 - y1)
        val area2 = (xa2 - xa1) * (ya2 - ya1)
        return if (area1 + area2 - inter <= 0f) 0f else inter / (area1 + area2 - inter)
    }

    /** sigmoid helper */
    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))

    /**
     * Generate final mask bitmap (binary white object / black background).
     * maskPrototypes layout assumed CHANNELS-FIRST: [numMasks, maskH, maskW]
     */
    private fun generateMask(
        maskCoeffs: FloatArray,
        maskPrototypes: FloatArray,
        originalWidth: Int,
        originalHeight: Int,
        bbox: FloatArray // x1,y1,x2,y2 in original coords
    ): Bitmap? {
        if (maskCoeffs.isEmpty()) return null

        // Compute proto logits -> probs (maskH x maskW)
        val protoSize = maskH * maskW
        val prob = FloatArray(protoSize)

        for (y in 0 until maskH) {
            for (x in 0 until maskW) {
                var sum = 0f
                val pxIndex = y * maskW + x
                // maskPrototypes expected layout: c * (maskH*maskW) + y*maskW + x
                for (c in 0 until numMasks) {
                    val protoVal = maskPrototypes[c * protoSize + pxIndex]
                    sum += maskCoeffs[c] * protoVal
                }
                prob[pxIndex] = sigmoid(sum)
            }
        }

        // Build prototype grayscale bitmap (0..255) and upscale
        val protoPixels = IntArray(protoSize)
        for (i in prob.indices) {
            val v = (prob[i] * 255f).toInt().coerceIn(0, 255)
            protoPixels[i] = Color.argb(255, v, v, v)
        }
        val protoBmp = createBitmap(maskW, maskH)
        protoBmp.setPixels(protoPixels, 0, maskW, 0, 0, maskW, maskH)

        // Upscale prototype to original image size (bilinear)
        val up = protoBmp.scale(originalWidth, originalHeight)
        protoBmp.recycle()

        // Threshold (adaptive simple choice) -> binary mask
        val finalPixels = IntArray(originalWidth * originalHeight)
        up.getPixels(finalPixels, 0, originalWidth, 0, 0, originalWidth, originalHeight)

        // use threshold 0.5 (you can tune or compute Otsu per-case)
        val thresh = 0.5f
        for (i in finalPixels.indices) {
            val v = Color.red(finalPixels[i]) // grayscale stored in R/G/B
            val p = v / 255f
            finalPixels[i] = if (p > thresh) Color.WHITE else Color.BLACK
        }

        // Enforce bbox strict mask: outside bbox -> background
        val x1 = bbox[0].toInt().coerceIn(0, originalWidth - 1)
        val y1 = bbox[1].toInt().coerceIn(0, originalHeight - 1)
        val x2 = bbox[2].toInt().coerceIn(0, originalWidth - 1)
        val y2 = bbox[3].toInt().coerceIn(0, originalHeight - 1)

        for (y in 0 until originalHeight) {
            val yOff = y * originalWidth
            val insideY = y in y1..y2
            for (x in 0 until originalWidth) {
                if (!insideY || x < x1 || x > x2) {
                    finalPixels[yOff + x] = Color.BLACK
                }
            }
        }

        val maskBitmap = createBitmap(originalWidth, originalHeight)
        maskBitmap.setPixels(finalPixels, 0, originalWidth, 0, 0, originalWidth, originalHeight)
        up.recycle()

        return maskBitmap
    }

    /**
     * Run full pipeline on a Bitmap and return final mask bitmap (white object on black background).
     */
    fun processImage(bitmap: Bitmap): Bitmap {
        if (ortSession == null) {
            println("ONNX session not loaded!")
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        try {
            val (inputBuffer, dims) = preprocessImage(bitmap)
            val (origW, origH) = dims

            // Create ONNX tensor (wrap FloatBuffer). Shape [1,3,608,608]
            val shape = longArrayOf(1, 3, modelSize.toLong(), modelSize.toLong())
            val inputTensor = OnnxTensor.createTensor(ortEnv, inputBuffer, shape)

            // Run and ensure outputs/results are closed
            ortSession!!.run(mapOf(ortSession!!.inputNames.first() to inputTensor)).use { results ->
                // results[0] -> predictions tensor ; results[1] -> mask prototypes
                val predTensor = results[0] as OnnxTensor
                val protoTensor = results[1] as OnnxTensor
                val shape = protoTensor.info.shape
                println("Mask prototype output shape: ${shape?.contentToString()}")

                // Read predictions into float array
                val predBuf = predTensor.floatBuffer
                predBuf.rewind()
                val preds = FloatArray(predBuf.remaining())
                predBuf.get(preds)

                // Read prototypes into float array
                val protoBuf = protoTensor.floatBuffer
                protoBuf.rewind()
                val protos = FloatArray(protoBuf.remaining())
                protoBuf.get(protos)

                // Close tensors (they will be closed when results is closed, but safe to close explicitly)
                predTensor.close()
                protoTensor.close()
                inputTensor.close()

                // Postprocess
                val (detections, maskProtos) = postprocessSegmentation(preds, protos, origW, origH)
                if (detections.isEmpty()) {
                    return bitmap.copy(Bitmap.Config.ARGB_8888, true)
                }

                // choose best detection
                val best = detections.maxByOrNull { it.confidence } ?: return bitmap.copy(Bitmap.Config.ARGB_8888, true)
                println("Best detection confidence: ${best.confidence}")

                // Generate mask
                val mask = generateMask(best.maskCoeffs, maskProtos, origW, origH, best.bbox)
                return mask ?: bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }
        } catch (e: Exception) {
            println("Error during inference: ${e.message}")
            e.printStackTrace()
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * Keep your upload/test helper — reads asset test image and processes it.
     * If you want to use the provided imageBytes instead, just decode them (commented).
     */
    fun getPianoKeysFromImage(imageBytes: ByteArray?): ByteArray? {

        return try {

            val assetManager = context.assets      // 'context' can be 'this' if inside an Activity
            val inputStream = assetManager.open("test_images/60.jpg")
            val byteArrayMocked = inputStream.readBytes()
            inputStream.close()

            val bitmap = BitmapFactory.decodeByteArray(byteArrayMocked, 0, byteArrayMocked.size)
                ?: throw Exception("Failed to decode image")

            // DESCOMENTAR PARA RERESAR AL OIGINAL
            val resultBitmap = processImage(bitmap)
            //val resultBitmap = preprocessImageTest(bitmap)

            val stream = java.io.ByteArrayOutputStream()
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            println("Error processing image: ${e.message}")
            e.printStackTrace()
            null
        }

    }

    fun close() {
        ortSession?.close()
    }
}
