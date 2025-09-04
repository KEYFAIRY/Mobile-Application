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
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        // Create a new bitmap with target size (608x608) and black background
        val paddedBitmap = Bitmap.createBitmap(modelSize, modelSize, Bitmap.Config.ARGB_8888)
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

        return Pair(inputBuffer, Pair(608, 608))
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

        println("Processing ${numDetections} detections with ${numChannels} channels")

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
        originalHeight: Int
    ): Bitmap? {
        if (maskCoeffs.isEmpty()) return null

        // maskPrototypes shape: [1, 152, 152, 32]
        val prototypeData = maskPrototypes.floatArray
        val shape = maskPrototypes.shape
        val maskSize = shape[1] // 152
        val numMasks = shape[3] // 32

        // Reshape prototypes: [1, H, W, C] -> [H][W][C]
        val prototypes = Array(maskSize) { Array(maskSize) { FloatArray(numMasks) } }
        var index = 0
        for (h in 0 until maskSize) {
            for (w in 0 until maskSize) {
                for (c in 0 until numMasks) {
                    prototypes[h][w][c] = prototypeData[index++]
                }
            }
        }

        // Matrix multiplication: for each [h,w], dot(maskCoeffs, prototypes[h][w])
        val mask = Array(maskSize) { FloatArray(maskSize) { 0f } }
        for (h in 0 until maskSize) {
            for (w in 0 until maskSize) {
                var sum = 0f
                for (c in 0 until numMasks) {
                    sum += maskCoeffs[c] * prototypes[h][w][c]
                }
                mask[h][w] = 1f / (1f + exp(-sum)) // Sigmoid
            }
        }

        // Convert to bitmap: threshold + to ARGB
        val maskBitmap = Bitmap.createBitmap(maskSize, maskSize, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(maskSize * maskSize)
        val threshold = 0.5f
        for (y in 0 until maskSize) {
            for (x in 0 until maskSize) {
                val value = if (mask[y][x] > threshold) 255 else 0
                pixels[y * maskSize + x] = Color.argb(128, value, 0, 0) // semi-transparent red
            }
        }
        maskBitmap.setPixels(pixels, 0, maskSize, 0, 0, maskSize, maskSize)

        // Scale to original image size for overlay
        return Bitmap.createScaledBitmap(maskBitmap, originalWidth, originalHeight, true)
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
                originalHeight
            )

            return mask ?: bitmap.copy(Bitmap.Config.ARGB_8888, true)

        } catch (e: Exception) {
            println("Error during inference: ${e.message}")
            e.printStackTrace()
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    private fun convertToTensorBuffer(array3D: Array<Array<FloatArray>>, shape: IntArray): TensorBuffer {
        val buffer = TensorBuffer.createFixedSize(shape, org.tensorflow.lite.DataType.FLOAT32)
        val flatArray = array3D.flatMap { it.flatMap { it.asIterable() } }.toFloatArray()
        buffer.loadArray(flatArray)
        return buffer
    }

    private fun convertToTensorBuffer(array4D: Array<Array<Array<FloatArray>>>, shape: IntArray): TensorBuffer {
        val buffer = TensorBuffer.createFixedSize(shape, org.tensorflow.lite.DataType.FLOAT32)
        val flatArray = array4D.flatMap { it.flatMap { it.flatMap { it.asIterable() } } }.toFloatArray()
        buffer.loadArray(flatArray)
        return buffer
    }


    fun getPianoKeysFromImage(imageBytes: ByteArray): ByteArray? {
        return try {

            val assetManager = context.assets      // 'context' can be 'this' if inside an Activity
            val inputStream = assetManager.open("test_images/90.jpg")
            val byteArrayMocked = inputStream.readBytes()
            inputStream.close()

            val bitmap = BitmapFactory.decodeByteArray(byteArrayMocked, 0, byteArrayMocked.size)
                ?: throw Exception("Failed to decode image")

            val resultBitmap = processImage(bitmap)

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