import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.nio.FloatBuffer
import kotlin.math.exp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class YOLO11Segmentation(private val context: Context) {

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null

    private val modelSize = 608               // model input W (=608)
    private val confThreshold = 0.7f
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
            val modelBytes = context.assets.open("yolo11n-seg-custom.onnx").use { it.readBytes() }
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
    private fun preprocessImage(originalBitmap: Bitmap, pianoAreaPercentage: Float): Pair<FloatBuffer, Pair<Int, Int>> {
        println("Origina image size: ${originalBitmap.width}x${originalBitmap.height}")

        val percentage = pianoAreaPercentage // Porcentaje del piano dinamico
        val cropHeight = (originalBitmap.height * percentage).toInt()
        val bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, cropHeight)

        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        println("Modded image size: ${originalWidth}x${originalHeight}")
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
        println(scaledHeight)
        // Calculate position to center the scaled image
        val left = (modelSize - scaledWidth) / 2f
        val top = 0f  // Center vertically instead of top-aligning

        println("Padding - left: $left, top: $top")

        // Draw the scaled image centered on the black background
        canvas.drawBitmap(scaledBitmap, left, top, null)

        println("Padding Dim: ${canvas.width}x${canvas.height}")

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

        val percentage = 0.3f // Keep top 30%
        val cropHeight = (bitmap.height * percentage).toInt()
        val bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, cropHeight)

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
        val rowLen = 38
        if (predictions.isEmpty() || predictions.size % rowLen != 0) {
            println("Unexpected predictions length ${predictions.size}, expected multiple of $rowLen")
        }
        val numDetections = predictions.size / rowLen
        val detections = mutableListOf<Detection>()

        for (i in 0 until numDetections) {
            val base = i * rowLen
            val x1raw = predictions[base + 0]
            val y1raw = predictions[base + 1]
            val x2raw = predictions[base + 2]
            val y2raw = predictions[base + 3]
            val score = predictions[base + 4]
            // val cls = predictions[base + 5] // not used (single class)

            if (score <= confThreshold) continue

            // Map coords to original image coordinates.
            // Many exported ONNX detectors output normalized coords (0..1),
            // but some return pixel coords relative to the model input width/height (e.g. 0..608).
            fun mapX(raw: Float): Float {
                return if (raw <= 1f) {
                    // normalized -> original pixel
                    (raw * originalWidth.toFloat()).coerceIn(0f, originalWidth.toFloat())
                } else {
                    // absolute relative to modelSize -> scale to original image
                    (raw * (originalWidth.toFloat() / modelSize.toFloat())).coerceIn(0f, originalWidth.toFloat())
                }
            }
            fun mapY(raw: Float): Float {
                return if (raw <= 1f) {
                    (raw * originalHeight.toFloat()).coerceIn(0f, originalHeight.toFloat())
                } else {
                    (raw * (originalHeight.toFloat() / modelSize.toFloat())).coerceIn(0f, originalHeight.toFloat())
                }
            }

            val x1 = mapX(x1raw)
            val y1 = mapY(y1raw)
            val x2 = mapX(x2raw)
            val y2 = mapY(y2raw)

            // Extract 32 mask coefficients (base+6 .. base+37)
            val coeffs = FloatArray(numMasks)
            for (c in 0 until numMasks) {
                val idx = base + 6 + c
                coeffs[c] = if (idx < predictions.size) predictions[idx] else 0f
            }

            detections.add(
                Detection(
                    bbox = floatArrayOf(x1, y1, x2, y2),
                    confidence = score,
                    maskCoeffs = coeffs
                )
            )
        }

        println("Detections before optional filter: ${detections.size}")
        return Pair(detections, maskPrototypes)
    }

    private fun generateMask(
        maskCoeffs: FloatArray,
        maskPrototypes: FloatArray,
        originalWidth: Int,
        originalHeight: Int,
        bbox: FloatArray // x1,y1,x2,y2 in original coords (pixels)
    ): Bitmap? {
        if (maskCoeffs.isEmpty() || maskPrototypes.isEmpty()) return null

        val protoSize = maskH * maskW
        val prob = FloatArray(protoSize)

        // Compute logits then sigmoid per proto pixel
        for (y in 0 until maskH) {
            val rowOffset = y * maskW
            for (x in 0 until maskW) {
                val pxIndex = rowOffset + x
                var sum = 0f
                // maskPrototypes layout NCHW flattened -> batch(=0) then c,h,w
                // offset for channel c: c * protoSize + pxIndex
                for (c in 0 until numMasks) {
                    val protoVal = maskPrototypes[c * protoSize + pxIndex]
                    sum += maskCoeffs[c] * protoVal
                }
                prob[pxIndex] = 1f / (1f + exp(-sum))
            }
        }

        // Make prototype grayscale bitmap
        val protoPixels = IntArray(protoSize)
        for (i in 0 until protoSize) {
            val v = (prob[i] * 255f).toInt().coerceIn(0, 255)
            protoPixels[i] = Color.argb(255, v, v, v)
        }
        val protoBmp = createBitmap(maskW, maskH)
        protoBmp.setPixels(protoPixels, 0, maskW, 0, 0, maskW, maskH)

        // Upscale to original image size (use bilinear to keep edges smoother)
        val up = protoBmp.scale(originalWidth, originalWidth, true)
        protoBmp.recycle()
        return up
//        // Threshold to binary mask (tune threshold if necessary)
//        val finalPixels = IntArray(originalWidth * originalHeight)
//        up.getPixels(finalPixels, 0, originalWidth, 0, 0, originalWidth, originalHeight)
//        val thresh = 0.5f
//        for (i in finalPixels.indices) {
//            val v = Color.red(finalPixels[i])
//            finalPixels[i] = if (v / 255f > thresh) Color.WHITE else Color.BLACK
//        }
//
//        // Enforce bbox strict mask: outside bbox -> background (black)
//        // Clamp bbox coords to integer image bounds:
//        val x1 = bbox[0].toInt().coerceIn(0, originalWidth - 1)
//        val y1 = bbox[1].toInt().coerceIn(0, originalHeight - 1)
//        val x2 = bbox[2].toInt().coerceIn(0, originalWidth - 1)
//        val y2 = bbox[3].toInt().coerceIn(0, originalHeight - 1)
//
//        for (y in 0 until originalHeight) {
//            val rowOff = y * originalWidth
//            val insideY = y in y1..y2
//            for (x in 0 until originalWidth) {
//                if (!insideY || x < x1 || x > x2) {
//                    finalPixels[rowOff + x] = Color.BLACK
//                }
//            }
//        }
//        val maskBitmap = createBitmap(originalWidth, originalHeight)
//        maskBitmap.setPixels(finalPixels, 0, originalWidth, 0, 0, originalWidth, originalHeight)
//        val pythonCornerDetectorExpectedImage = maskBitmap.scale(originalWidth, originalWidth, true)
//        up.recycle()
////        maskBitmap.recycle()
//
//        return maskBitmap
    }

    /**
     * Run full pipeline on a Bitmap and return final mask bitmap (white object on black background).
     */
    fun processImage(bitmap: Bitmap, pianoAreaPercentage: Float): Bitmap {
        if (ortSession == null) {
            println("ONNX session not loaded!")
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        try {
            val (inputBuffer, dims) = preprocessImage(bitmap, pianoAreaPercentage)
            val (origW, origH) = dims

            // Create ONNX tensor (wrap FloatBuffer). Shape [1,3,608,608]
            val shape = longArrayOf(1, 3, modelSize.toLong(), modelSize.toLong())
            val inputTensor = OnnxTensor.createTensor(ortEnv, inputBuffer, shape)

            // Run and ensure outputs/results are closed
            ortSession!!.run(mapOf(ortSession!!.inputNames.first() to inputTensor)).use { results ->
                // results[0] -> predictions tensor ; results[1] -> mask prototypes
                println("Number of outputs: ${results.size()}")
                for (i in 0 until results.size()) {
                    val tensor = results[i] as OnnxTensor
                    println("Output $i shape: ${tensor.info.shape?.contentToString()}")
                    println("Output $i name: ${ortSession!!.outputNames.toList()[i]}")
                }
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

    fun getPianoKeysFromImage(imageBytes: ByteArray?, pianoAreaPercentage: Float): ByteArray? {



        return try {

            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes!!.size)
                ?: throw Exception("Failed to decode image")

            // DESCOMENTAR PARA RERESAR AL OIGINAL
            val resultBitmap = processImage(bitmap, pianoAreaPercentage)
//            val resultBitmap = preprocessImageTest(bitmap)

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
