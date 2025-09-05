import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class YOLO11Segmentation(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val modelSize = 608 // Your model expects 608x608
    private val confThreshold = 0.7f
    private val iouThreshold = 0.5f

    data class Detection(
        val bbox: FloatArray,
        val confidence: Float,
        val maskCoeffs: FloatArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Detection
            if (confidence != other.confidence) return false
            if (!bbox.contentEquals(other.bbox)) return false
            if (!maskCoeffs.contentEquals(other.maskCoeffs)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = bbox.contentHashCode()
            result = 31 * result + confidence.hashCode()
            result = 31 * result + maskCoeffs.contentHashCode()
            return result
        }
    }

    init {
        loadModel()
    }

    private fun loadModel(): Boolean {
        return try {
            val model = FileUtil.loadMappedFile(context, "yolo11s-seg-keys_float32.tflite")
            val options = Interpreter.Options()
            interpreter = Interpreter(model, options)

            // Print model details
            val inputTensor = interpreter!!.getInputTensor(0)
            val outputTensor0 = interpreter!!.getOutputTensor(0)
            val outputTensor1 = interpreter!!.getOutputTensor(1)

            println("Input shape: ${inputTensor.shape().contentToString()}")
            println("Output 0 shape: ${outputTensor0.shape().contentToString()}")
            println("Output 1 shape: ${outputTensor1.shape().contentToString()}")

            true
        } catch (e: Exception) {
            println("Error loading TFLite model: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun preprocessImage(bitmap: Bitmap): Pair<ByteBuffer, Pair<Int, Int>> {
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
        val scaledBitmap = bitmap.scale(scaledWidth, scaledHeight)

        // Create a new bitmap with target size (608x608) and black background
        val paddedBitmap = createBitmap(modelSize, modelSize)
        val canvas = Canvas(paddedBitmap)

        // Fill with black background
        canvas.drawColor(Color.BLACK)

        // Calculate position to center the scaled image
        val left = (modelSize - scaledWidth) / 2f
        val top = (modelSize - scaledHeight) / 2f

        println("Padding - left: $left, top: $top")

        // Draw the scaled image centered on the black background
        canvas.drawBitmap(scaledBitmap, left, top, null)

        println("Padding Dim: ${canvas.width}x${canvas.height}")

        // Create ByteBuffer for the input tensor
        val inputBuffer = ByteBuffer.allocateDirect(4 * modelSize * modelSize * 3)
        inputBuffer.order(java.nio.ByteOrder.nativeOrder())


        // Convert bitmap to float array and normalize
        val intValues = IntArray(modelSize * modelSize)

        paddedBitmap.getPixels(intValues, 0, modelSize, 0, 0, modelSize, modelSize)


        for (i in intValues.indices) {
            val pixel = intValues[i]
            // Extract RGB values and normalize to [0, 1]
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // Red
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // Green
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)          // Blue
        }

        inputBuffer.rewind()
        println("Created input buffer size: ${inputBuffer.capacity()}")

        // Clean up intermediate bitmaps
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        paddedBitmap.recycle()

        return Pair(inputBuffer, Pair(originalWidth, originalHeight))
    }




    private fun preprocessImageTest(bitmap: Bitmap): Bitmap {
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
        val scaledBitmap = bitmap.scale(scaledWidth, scaledHeight)

        // Create a new bitmap with target size (608x608) and black background
        val paddedBitmap = createBitmap(modelSize, modelSize)
        val canvas = Canvas(paddedBitmap)

        // Fill with black background
        canvas.drawColor(Color.BLACK)

        // Calculate position to center the scaled image
        val left = (modelSize - scaledWidth) / 2f
        val top = (modelSize - scaledHeight) / 2f

        println("Padding - left: $left, top: $top")

        // Draw the scaled image centered on the black background
        canvas.drawBitmap(scaledBitmap, left, top, null)

        println("Padding Dim: ${canvas.width}x${canvas.height}")
        return paddedBitmap
    }





    private fun postprocessSegmentation(
        predictions: TensorBuffer,
        maskPrototypes: TensorBuffer,
        originalWidth: Int,
        originalHeight: Int
    ): Pair<List<Detection>, TensorBuffer> {
        val detections = mutableListOf<Detection>()

        // predictions shape should be [1, 37, 7581]
        val predictionData = predictions.floatArray
        val shape = predictions.shape
        val numDetections = shape[2] // 7581
        val numChannels = shape[1] // 37

        println("Processing $numDetections detections with $numChannels channels")

        for (i in 0 until numDetections) {
            // YOLOv11 format: cx, cy, w, h, confidence, mask_coeffs[32]
            val cx = predictionData[i] // center x
            val cy = predictionData[i + numDetections] // center y
            val width = predictionData[i + numDetections * 2] // width
            val height = predictionData[i + numDetections * 3] // height
            val confidence = predictionData[i + numDetections * 4] // objectness

            if (confidence > confThreshold) {
                // Extract mask coefficients (32 values starting from index 5)
                val maskCoeffs = FloatArray(32)
                for (j in 0 until 32) {
                    maskCoeffs[j] = predictionData[i + numDetections * (5 + j)]
                }

                val detection = Detection(
                    bbox = floatArrayOf(cx, cy, width, height), // Keep normalized for NMS
                    confidence = confidence,
                    maskCoeffs = maskCoeffs
                )
                detections.add(detection)
            }
        }

        // Apply Non-Maximum Suppression
        val filteredDetections = applyNMS(detections)
        println("After NMS: ${filteredDetections.size} detections")

        return Pair(filteredDetections, maskPrototypes)
    }

    private fun applyNMS(detections: List<Detection>): List<Detection> {
        val sortedDetections = detections.sortedByDescending { it.confidence }.toMutableList()
        val selectedDetections = mutableListOf<Detection>()

        while (sortedDetections.isNotEmpty()) {
            val first = sortedDetections.first()
            selectedDetections.add(first)
            sortedDetections.remove(first)

            val iterator = sortedDetections.iterator()
            while (iterator.hasNext()) {
                val nextDetection = iterator.next()
                val iou = calculateIoU(first, nextDetection)
                if (iou >= iouThreshold) {
                    iterator.remove()
                }
            }
        }

        return selectedDetections
    }

    private fun calculateIoU(det1: Detection, det2: Detection): Float {
        val cx1 = det1.bbox[0]
        val cy1 = det1.bbox[1]
        val w1 = det1.bbox[2]
        val h1 = det1.bbox[3]

        val cx2 = det2.bbox[0]
        val cy2 = det2.bbox[1]
        val w2 = det2.bbox[2]
        val h2 = det2.bbox[3]

        val x1 = max(cx1 - w1/2f, cx2 - w2/2f)
        val y1 = max(cy1 - h1/2f, cy2 - h2/2f)
        val x2 = min(cx1 + w1/2f, cx2 + w2/2f)
        val y2 = min(cy1 + h1/2f, cy2 + h2/2f)

        val intersectionArea = max(0f, x2 - x1) * max(0f, y2 - y1)
        val box1Area = w1 * h1
        val box2Area = w2 * h2

        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }
    private fun generateMask(
        maskCoeffs: FloatArray,
        maskPrototypes: TensorBuffer,
        originalWidth: Int,
        originalHeight: Int,
        bbox: FloatArray, // [cx, cy, w, h] normalized to model input
        smooth: Boolean = true,
        negateCoeffs: Boolean = false // Option to negate coefficients
    ): Bitmap? {
        if (maskCoeffs.isEmpty()) return null

        // 1) Read prototype tensor in NHWC
        val proto = maskPrototypes.floatArray
        val shape = maskPrototypes.shape // [1, maskH, maskW, numMasks]
        val maskH = shape[1]
        val maskW = shape[2]
        val numMasks = shape[3]
        val strideNM = numMasks
        val strideW = maskW * strideNM

        // 2) Compose mask logits and apply sigmoid
        val prob = FloatArray(maskH * maskW)
        var p = 0
        var minProb = Float.MAX_VALUE
        var maxProb = Float.MIN_VALUE
        var minLogit = Float.MAX_VALUE
        var maxLogit = Float.MIN_VALUE

        for (y in 0 until maskH) {
            for (x in 0 until maskW) {
                val base = y * strideW + x * strideNM
                var sum = 0f
                for (c in 0 until numMasks) {
                    sum += (if (negateCoeffs) -maskCoeffs[c] else maskCoeffs[c]) * proto[base + c]
                }
                minLogit = minOf(minLogit, sum)
                maxLogit = maxOf(maxLogit, sum)
                prob[p] = 1f / (1f + exp(-sum)) // sigmoid
                minProb = minOf(minProb, prob[p])
                maxProb = maxOf(maxProb, prob[p])
                p++
            }
        }

        // Debug: Log ranges
        println("Logit range: min=$minLogit, max=$maxLogit")
        println("Probability range: min=$minProb, max=$maxProb")
        println("Bbox normalized: cx=${bbox[0]}, cy=${bbox[1]}, w=${bbox[2]}, h=${bbox[3]}")

        // 3) Check average probability in bbox at prototype resolution
        val cxProto = bbox[0] * maskW
        val cyProto = bbox[1] * maskH
        val bwProto = bbox[2] * maskW
        val bhProto = bbox[3] * maskH
        val leftBoxProto = (cxProto - bwProto / 2f).toInt().coerceIn(0, maskW - 1)
        val topBoxProto = (cyProto - bhProto / 2f).toInt().coerceIn(0, maskH - 1)
        val rightBoxProto = (cxProto + bwProto / 2f).toInt().coerceIn(0, maskW - 1)
        val bottomBoxProto = (cyProto + bhProto / 2f).toInt().coerceIn(0, maskH - 1)
        println("Bbox in prototype space: left=$leftBoxProto, top=$topBoxProto, right=$rightBoxProto, bottom=$bottomBoxProto")

        var bboxProbSum = 0f
        var bboxPixelCount = 0
        for (y in topBoxProto..bottomBoxProto) {
            for (x in leftBoxProto..rightBoxProto) {
                val idx = y * maskW + x
                if (idx < prob.size) {
                    bboxProbSum += prob[idx]
                    bboxPixelCount++
                }
            }
        }
        val bboxAvgProb = if (bboxPixelCount > 0) bboxProbSum / bboxPixelCount else 0f
        println("Average probability in bbox: $bboxAvgProb")

//        // 4) Invert probabilities if object has low probabilities
//        if (bboxAvgProb < 0.5f) {
//            println("Inverting probabilities (bbox avg prob = $bboxAvgProb)")
//            for (i in prob.indices) {
//                prob[i] = 1f - prob[i]
//            }
//        }

        // 5) Convert prob[] to grayscale Bitmap at prototype resolution
        val gray = IntArray(maskH * maskW)
        for (i in prob.indices) {
            val v = (prob[i] * 255).toInt().coerceIn(0, 255)
            gray[i] = Color.argb(255, v, v, v) // RGB = probability
        }
        val protoMaskBmp = createBitmap(maskW, maskH)
        protoMaskBmp.setPixels(gray, 0, maskW, 0, 0, maskW, maskH)

        // Save proto mask for debugging
        val protoStream = java.io.ByteArrayOutputStream()
        protoMaskBmp.compress(Bitmap.CompressFormat.PNG, 100, protoStream)


        // 6) Upscale to model size
        val modelMaskBmp = protoMaskBmp.scale(modelSize, modelSize, smooth)
        protoMaskBmp.recycle()

        // 7) Threshold with YOLO library’s default
        val modelPixels = IntArray(modelSize * modelSize)
        modelMaskBmp.getPixels(modelPixels, 0, modelSize, 0, 0, modelSize, modelSize)
        val thresh = 0.5f // Match Ultralytics default
        println("Using threshold: $thresh")
        for (i in modelPixels.indices) {
            val v = Color.red(modelPixels[i])
            modelPixels[i] = if (v / 255f > thresh) {
                Color.argb(255, 255, 255, 255) // White for object
            } else {
                Color.argb(255, 0, 0, 0) // Black for background
            }
        }
        modelMaskBmp.setPixels(modelPixels, 0, modelSize, 0, 0, modelSize, modelSize)

        // 8) Adjust bounding box with padding to avoid over-suppression
        val cxModel = bbox[0] * modelSize
        val cyModel = bbox[1] * modelSize
        val bwModel = bbox[2] * modelSize * 1.1f // Add 10% padding
        val bhModel = bbox[3] * modelSize * 1.1f // Add 10% padding
        val leftBoxModel = (cxModel - bwModel / 2f).toInt().coerceIn(0, modelSize - 1)
        val topBoxModel = (cyModel - bhModel / 2f).toInt().coerceIn(0, modelSize - 1)
        val rightBoxModel = (cxModel + bwModel / 2f).toInt().coerceIn(0, modelSize - 1)
        val bottomBoxModel = (cyModel + bhModel / 2f).toInt().coerceIn(0, modelSize - 1)
        println("Bbox in model space: left=$leftBoxModel, top=$topBoxModel, right=$rightBoxModel, bottom=$bottomBoxModel")

        for (y in 0 until modelSize) {
            val yOff = y * modelSize
            val insideY = y in topBoxModel..bottomBoxModel
            for (x in 0 until modelSize) {
                if (!insideY || x < leftBoxModel || x > rightBoxModel) {
                    modelPixels[yOff + x] = Color.argb(255, 0, 0, 0) // Black outside bbox
                }
            }
        }
        modelMaskBmp.setPixels(modelPixels, 0, modelSize, 0, 0, modelSize, modelSize)

        // Save model mask for debugging
        val modelStream = java.io.ByteArrayOutputStream()
        modelMaskBmp.compress(Bitmap.CompressFormat.PNG, 100, modelStream)


        // 9) Unpad to original size
        val scale = minOf(
            modelSize.toFloat() / originalWidth.toFloat(),
            modelSize.toFloat() / originalHeight.toFloat()
        )
        val scaledW = (originalWidth * scale).toInt()
        val scaledH = (originalHeight * scale).toInt()
        val padLeft = ((modelSize - scaledW) / 2f).toInt()
        val padTop = ((modelSize - scaledH) / 2f).toInt()

        val cropRectW = scaledW.coerceAtLeast(1)
        val cropRectH = scaledH.coerceAtLeast(1)
        val unpadded = Bitmap.createBitmap(modelMaskBmp, padLeft, padTop, cropRectW, cropRectH)
        modelMaskBmp.recycle()

        // 10) Resize to exact original dimensions
        val finalBitmap = unpadded.scale(originalWidth, originalHeight, true)
        unpadded.recycle()

        return finalBitmap
    }


    fun processImage(bitmap: Bitmap): Bitmap {
        if (interpreter == null) {
            println("Interpreter not loaded!")
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        try {
            val (inputBuffer, originalDimensions) = preprocessImage(bitmap)
            val (originalWidth, originalHeight) = originalDimensions

// Prepare output buffers
            val outputShape0 = interpreter!!.getOutputTensor(0).shape() // e.g., [1, 37, 7581]
            val outputShape1 = interpreter!!.getOutputTensor(1).shape() // e.g., [1, 152, 152, 32]

            val predictions = TensorBuffer.createFixedSize(outputShape0, org.tensorflow.lite.DataType.FLOAT32)
            val maskPrototypes = TensorBuffer.createFixedSize(outputShape1, org.tensorflow.lite.DataType.FLOAT32)

            val outputMap = mapOf(
                0 to predictions.buffer,
                1 to maskPrototypes.buffer
            )

// Run inferenceOJO ESTE POTENCIALMENTE FALA
            interpreter!!.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)


            // Post-process
            val (detections, _) = postprocessSegmentation(
                predictions, maskPrototypes, originalWidth, originalHeight
            )

            println("Found ${detections.size} detections after filtering")

            // If no detections, return original image
            if (detections.isEmpty()) {
                return bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }

            // Take the best detection (highest confidence)
            val bestDetection = detections.maxByOrNull { it.confidence }
                ?: return bitmap.copy(Bitmap.Config.ARGB_8888, true)

            println("Best detection confidence: ${bestDetection.confidence}")

            // Generate mask for the best detection
            val mask = generateMask(
                bestDetection.maskCoeffs,
                maskPrototypes,
                originalWidth,
                originalHeight,
                bestDetection.bbox
            )

            return mask ?: bitmap.copy(Bitmap.Config.ARGB_8888, true)

        } catch (e: Exception) {
            println("Error during inference: ${e.message}")
            e.printStackTrace()
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    fun getPianoKeysFromImage(imageBytes: ByteArray): ByteArray? {
        return try {

            val assetManager = context.assets      // 'context' can be 'this' if inside an Activity
            val inputStream = assetManager.open("test_images/98.jpg")
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
        interpreter?.close()
    }
}