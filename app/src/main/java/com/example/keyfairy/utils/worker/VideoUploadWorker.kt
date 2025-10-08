package com.example.keyfairy.utils.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.keyfairy.feature_check_video.data.repository.PracticeRepositoryImpl
import com.example.keyfairy.feature_check_video.domain.model.Practice
import com.example.keyfairy.feature_check_video.domain.use_case.RegisterPracticeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException
import javax.net.ssl.SSLException

class VideoUploadWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        const val KEY_VIDEO_URI = "video_uri"
        const val KEY_VIDEO_PATH = "video_path"
        const val KEY_UID = "uid"
        const val KEY_PRACTICE_ID = "practice_id"
        const val KEY_DATE = "date"
        const val KEY_TIME = "time"
        const val KEY_SCALE = "scale"
        const val KEY_SCALE_TYPE = "scale_type"
        const val KEY_DURATION = "duration"
        const val KEY_BPM = "bpm"
        const val KEY_FIGURE = "figure"
        const val KEY_OCTAVES = "octaves"
        const val KEY_VIDEO_LOCAL_ROUTE = "video_local_route"
        const val KEY_TIMESTAMP = "timestamp"

        // Mensajes de estado
        const val STATUS_WAITING_INTERNET = "Esperando internet"
        const val STATUS_WAITING_SERVER = "Esperando servidor"
        const val STATUS_UPLOADING = "Enviando"
        const val STATUS_NO_VIDEO = "Video borrado"
        const val WORKER_STOPPED = "Worker detenido"

        const val WORK_NAME = "video_upload_work"
        private const val MAX_RETRY_COUNT = 10
        private const val NOTIF_CHANNEL_ID = "video_upload_channel"
        private const val NOTIF_ID = 98765
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val workId = id
        val attemptNumber = runAttemptCount + 1

        Log.d("VideoUploadWorker", "🚀 Starting upload attempt $attemptNumber/$MAX_RETRY_COUNT for work: $workId")

        // Verificar conectividad al inicio
        val hasNetwork = isNetworkAvailable()
        Log.d("VideoUploadWorker", "📡 Initial network check: $hasNetwork")

        if (!hasNetwork) {
            Log.w("VideoUploadWorker", "📡 No network available - setting '$STATUS_WAITING_INTERNET' status")
            setWaitingStatus(STATUS_WAITING_INTERNET, attemptNumber)
            return@withContext Result.retry()
        }

        try {
            val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
            val scale = inputData.getString(KEY_SCALE) ?: ""

            // Establecer estado inicial como UPLOADING ya que hay internet
            setProgressWithStatus(0, "Iniciando...", timestamp, attemptNumber, STATUS_UPLOADING)
            setForegroundAsync(createForegroundInfo("Subiendo $scale... (intento $attemptNumber/$MAX_RETRY_COUNT)"))
        } catch (e: Exception) {
            Log.w("VideoUploadWorker", "⚠️ Could not set foreground: ${e.message}")
        }

        try {
            if (isStopped) {
                Log.w("VideoUploadWorker", "🛑 Worker was stopped")
                return@withContext Result.failure(createFailureData("Worker was cancelled by system", attemptNumber, WORKER_STOPPED))
            }

            val videoUriString = inputData.getString(KEY_VIDEO_URI)
            val videoPath = inputData.getString(KEY_VIDEO_PATH)

            if (videoUriString.isNullOrEmpty() || videoPath.isNullOrEmpty()) {
                Log.e("VideoUploadWorker", "❌ Video URI or path missing in inputData")
                return@withContext Result.failure(createFailureData("Missing video URI or path", attemptNumber, STATUS_NO_VIDEO))
            }

            val videoUri = Uri.parse(videoUriString)
            val videoFile = File(videoPath)

            if (!videoFile.exists()) {
                Log.e("VideoUploadWorker", "❌ Video file not found at: $videoPath")
                return@withContext Result.failure(
                    createFailureData("Video file not found", attemptNumber, STATUS_NO_VIDEO)
                        .toOutputWithVideoInfo(videoUri, videoPath)
                )
            }

            // Extraer todos los campos
            val uid = inputData.getString(KEY_UID).orEmpty()
            val practiceId = inputData.getInt(KEY_PRACTICE_ID, 0)
            val date = inputData.getString(KEY_DATE).orEmpty()
            val time = inputData.getString(KEY_TIME).orEmpty()
            val scale = inputData.getString(KEY_SCALE).orEmpty()
            val scaleType = inputData.getString(KEY_SCALE_TYPE).orEmpty()
            val duration = inputData.getInt(KEY_DURATION, 0)
            val bpm = inputData.getInt(KEY_BPM, 120)
            val figure = inputData.getDouble(KEY_FIGURE, 1.0)
            val octaves = inputData.getInt(KEY_OCTAVES, 1)
            val videoLocalRoute = inputData.getString(KEY_VIDEO_LOCAL_ROUTE) ?: videoPath
            val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())

            Log.d("VideoUploadWorker", "📋 Practice details:")
            Log.d("VideoUploadWorker", "   🎵 Scale: $scale ($scaleType)")
            Log.d("VideoUploadWorker", "   📊 File size: ${videoFile.length() / 1024}KB")
            Log.d("VideoUploadWorker", "   🔄 Attempt: $attemptNumber/$MAX_RETRY_COUNT")

            setProgressWithStatus(10, "Preparando upload...", timestamp, attemptNumber, STATUS_UPLOADING)

            val repository = PracticeRepositoryImpl()
            val useCase = RegisterPracticeUseCase(repository)

            setProgressWithStatus(30, "Conectando al servidor...", timestamp, attemptNumber, STATUS_UPLOADING)
            if (isStopped) return@withContext Result.failure(createFailureData("Worker cancelled", attemptNumber, WORKER_STOPPED))

            setProgressWithStatus(50, "Enviando video...", timestamp, attemptNumber, STATUS_UPLOADING)

            val practice = Practice(
                uid = uid,
                practiceId = practiceId,
                date = date,
                time = time,
                scale = scale,
                scaleType = scaleType,
                duration = duration,
                bpm = bpm,
                figure = figure,
                octaves = octaves,
                videoLocalRoute = videoLocalRoute
            )

            Log.d("VideoUploadWorker", "📤 Executing upload for $scale...")
            val result = useCase.execute(practice, videoFile)

            if (isStopped) return@withContext Result.failure(createFailureData("Worker cancelled", attemptNumber, WORKER_STOPPED))

            return@withContext if (result.isSuccess) {
                val practiceResult = result.getOrNull()
                setProgressWithStatus(100, "Upload completado ✅", timestamp, attemptNumber, STATUS_UPLOADING)

                Log.d("VideoUploadWorker", "✅ Upload successful - Practice #${practiceResult?.practiceId} after $attemptNumber attempts")

                val output = workDataOf(
                    "success" to true,
                    "practice_id" to (practiceResult?.practiceId ?: 0),
                    "uid" to uid,
                    "date" to date,
                    "time" to time,
                    "scale" to scale,
                    "scale_type" to scaleType,
                    "bpm" to bpm,
                    "figure" to figure,
                    "octaves" to octaves,
                    "duration" to duration,
                    "attempts" to attemptNumber,
                    "timestamp" to timestamp,
                    "message" to STATUS_UPLOADING,
                    KEY_VIDEO_URI to videoUriString,
                    KEY_VIDEO_PATH to videoPath,
                )

                Result.success(output)
            } else {
                val error = result.exceptionOrNull()
                val errorMessage = error?.message ?: "Unknown error"
                handleUploadError(error, errorMessage, attemptNumber, timestamp)
            }

        } catch (e: Exception) {
            Log.e("VideoUploadWorker", "💥 Exception in worker: ${e.message}", e)
            val videoUriString = inputData.getString(KEY_VIDEO_URI)
            val videoPath = inputData.getString(KEY_VIDEO_PATH)
            val videoUri = if (videoUriString != null) Uri.parse(videoUriString) else null
            handleUploadError(
                e,
                e.message ?: "Exception",
                attemptNumber,
                inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
            )
        }
    }

    private suspend fun handleUploadError(
        error: Throwable?,
        errorMessage: String,
        attemptNumber: Int,
        timestamp: Long
    ): Result {
        Log.w("VideoUploadWorker", "❌ Upload error on attempt $attemptNumber/$MAX_RETRY_COUNT")
        Log.w("VideoUploadWorker", "   🔍 Error type: ${error?.javaClass?.simpleName}")
        Log.w("VideoUploadWorker", "   💬 Message: $errorMessage")

        val isPermanentError = isPermanentError(error, errorMessage)
        val shouldRetry = !isPermanentError && attemptNumber < MAX_RETRY_COUNT

        Log.w("VideoUploadWorker", "   🔄 Permanent error: $isPermanentError")
        Log.w("VideoUploadWorker", "   🔄 Will retry: $shouldRetry")

        return if (shouldRetry) {
            val nextAttempt = attemptNumber + 1

            // Determinar si  hay internet
            val waitingStatus = if (!isNetworkAvailable()) {
                Log.d("VideoUploadWorker", "📡 Network error detected.")
                STATUS_WAITING_INTERNET
            } else {
                STATUS_WAITING_SERVER
            }

            Log.d("VideoUploadWorker", "🔄 Final status decision: $waitingStatus for retry $nextAttempt")
            setWaitingStatus(waitingStatus, nextAttempt)
            Result.retry()
        } else {
            if (isPermanentError) {
                Log.e("VideoUploadWorker", "❌ Permanent error detected, failing without retries")
            } else {
                Log.e("VideoUploadWorker", "❌ Max retries reached ($MAX_RETRY_COUNT), failing permanently")
            }

            val failureData = createFailureData(errorMessage, attemptNumber, STATUS_UPLOADING, timestamp)
                .toOutputWithAllInfo()
            Result.failure(failureData)
        }
    }

    private suspend fun setWaitingStatus(statusMessage: String, attemptNumber: Int) {
        try {
            val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
            val waitingProgress = workDataOf(
                "progress" to 0,
                "message" to statusMessage,
                "timestamp" to timestamp,
                "attempts" to attemptNumber,
                KEY_SCALE to (inputData.getString(KEY_SCALE) ?: ""),
                KEY_SCALE_TYPE to (inputData.getString(KEY_SCALE_TYPE) ?: ""),
                KEY_DATE to (inputData.getString(KEY_DATE) ?: ""),
                KEY_TIME to (inputData.getString(KEY_TIME) ?: ""),
                KEY_BPM to inputData.getInt(KEY_BPM, 0),
                KEY_FIGURE to inputData.getDouble(KEY_FIGURE, 0.0),
                KEY_OCTAVES to inputData.getInt(KEY_OCTAVES, 0),
                KEY_DURATION to inputData.getInt(KEY_DURATION, 0),
                KEY_VIDEO_PATH to (inputData.getString(KEY_VIDEO_PATH) ?: ""),
            )
            setProgress(waitingProgress)
            Log.d("VideoUploadWorker", "📊 Set waiting status: $statusMessage (attempt $attemptNumber)")
        } catch (e: Exception) {
            Log.e("VideoUploadWorker", "Error setting waiting status: ${e.message}", e)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager =
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            val hasInternetCapability =
                networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val isValidated =
                networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

            // Validación rápida de acceso real a internet
            val hasRealInternet = if (hasInternetCapability) {
                try {
                    val url = java.net.URL("https://clients3.google.com/generate_204")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.setRequestProperty("User-Agent", "Android")
                    connection.setRequestProperty("Connection", "close")
                    connection.connectTimeout = 1500
                    connection.readTimeout = 1500
                    connection.connect()
                    connection.responseCode == 204
                } catch (e: Exception) {
                    Log.w("VideoUploadWorker", "⚠️ Active internet check failed: ${e.message}")
                    false
                }
            } else false

            Log.d(
                "VideoUploadWorker",
                "🌐 Network check: hasInternetCap=$hasInternetCapability, validated=$isValidated, realInternet=$hasRealInternet"
            )

            hasInternetCapability && (isValidated || hasRealInternet)
        } catch (e: Exception) {
            Log.w("VideoUploadWorker", "⚠️ Error checking network: ${e.message}")
            false
        }
    }

    private fun isPermanentError(error: Throwable?, errorMessage: String): Boolean {
        val permanentErrors = listOf(
            "file not found", "permission denied", "authentication failed",
            "unauthorized", "invalid credentials", "missing video uri",
            "missing video path", "400", "401", "403"
        )
        return permanentErrors.any { errorMessage.contains(it, ignoreCase = true) }
    }


    private suspend fun setProgressWithStatus(
        progress: Int,
        message: String,
        timestamp: Long,
        attemptNumber: Int,
        status: String
    ) {
        try {
            if (!isStopped) {
                val progressData = workDataOf(
                    "progress" to progress,
                    "message" to status,
                    "timestamp" to timestamp,
                    "attempts" to attemptNumber,
                    KEY_SCALE to (inputData.getString(KEY_SCALE) ?: ""),
                    KEY_SCALE_TYPE to (inputData.getString(KEY_SCALE_TYPE) ?: ""),
                    KEY_DATE to (inputData.getString(KEY_DATE) ?: ""),
                    KEY_TIME to (inputData.getString(KEY_TIME) ?: ""),
                    KEY_BPM to inputData.getInt(KEY_BPM, 0),
                    KEY_FIGURE to inputData.getDouble(KEY_FIGURE, 0.0),
                    KEY_OCTAVES to inputData.getInt(KEY_OCTAVES, 0),
                    KEY_DURATION to inputData.getInt(KEY_DURATION, 0),
                    KEY_VIDEO_PATH to (inputData.getString(KEY_VIDEO_PATH) ?: ""),
                )
                setProgress(progressData)
                Log.d("VideoUploadWorker", "📊 Progress: $progress% - $message (status: $status)")
            }
        } catch (e: Exception) {
            Log.e("VideoUploadWorker", "Error updating progress: ${e.message}", e)
        }
    }

    private fun createFailureData(
        error: String,
        attempts: Int,
        status: String = STATUS_UPLOADING,
        timestamp: Long = System.currentTimeMillis()
    ): Data {
        return workDataOf(
            "success" to false,
            "error" to error,
            "work_id" to id.toString(),
            "attempts" to attempts,
            "timestamp" to timestamp,
            "message" to status,
            KEY_SCALE to (inputData.getString(KEY_SCALE) ?: ""),
            KEY_SCALE_TYPE to (inputData.getString(KEY_SCALE_TYPE) ?: ""),
            KEY_DATE to (inputData.getString(KEY_DATE) ?: ""),
            KEY_TIME to (inputData.getString(KEY_TIME) ?: ""),
            KEY_BPM to inputData.getInt(KEY_BPM, 0),
            KEY_FIGURE to inputData.getDouble(KEY_FIGURE, 0.0),
            KEY_OCTAVES to inputData.getInt(KEY_OCTAVES, 0),
            KEY_DURATION to inputData.getInt(KEY_DURATION, 0),
            KEY_VIDEO_PATH to (inputData.getString(KEY_VIDEO_PATH) ?: ""),
            KEY_VIDEO_URI to (inputData.getString(KEY_VIDEO_URI) ?: ""),
            KEY_TIMESTAMP to inputData.getLong(KEY_TIMESTAMP, timestamp),
        )
    }

    private fun Data.toOutputWithAllInfo(): Data {
        val entries = this.keyValueMap.entries.map { it.key to it.value }.toMutableList()

        val inputKeys = listOf(
            KEY_VIDEO_URI, KEY_VIDEO_PATH, KEY_UID, KEY_PRACTICE_ID,
            KEY_DATE, KEY_TIME, KEY_SCALE, KEY_SCALE_TYPE,
            KEY_DURATION, KEY_BPM, KEY_FIGURE, KEY_OCTAVES,
            KEY_VIDEO_LOCAL_ROUTE, KEY_TIMESTAMP
        )

        inputKeys.forEach { key ->
            if (!this.keyValueMap.containsKey(key)) {
                when (key) {
                    KEY_BPM, KEY_PRACTICE_ID, KEY_DURATION, KEY_OCTAVES -> {
                        entries.add(key to inputData.getInt(key, 0))
                    }
                    KEY_FIGURE -> {
                        entries.add(key to inputData.getDouble(key, 0.0))
                    }
                    KEY_TIMESTAMP -> {
                        entries.add(key to inputData.getLong(key, System.currentTimeMillis()))
                    }
                    else -> {
                        inputData.getString(key)?.let { value ->
                            entries.add(key to value)
                        }
                    }
                }
            }
        }

        return workDataOf(*entries.toTypedArray())
    }

    private fun Data.toOutputWithVideoInfo(videoUri: Uri?, videoPath: String?): Data {
        val entries = this.keyValueMap.entries.map { it.key to it.value }.toMutableList()

        if (videoUri != null) {
            entries.add(KEY_VIDEO_URI to videoUri.toString())
        }
        if (!videoPath.isNullOrEmpty()) {
            entries.add(KEY_VIDEO_PATH to videoPath)
        }

        return workDataOf(*entries.toTypedArray())
    }

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        val channelId = NOTIF_CHANNEL_ID
        val channelName = "Subida de videos"

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones para subida de videos en segundo plano"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val notif: Notification = NotificationCompat.Builder(ctx, channelId)
            .setContentTitle("KeyFairy - Subida de video")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIF_ID,
                notif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIF_ID, notif)
        }
    }
}