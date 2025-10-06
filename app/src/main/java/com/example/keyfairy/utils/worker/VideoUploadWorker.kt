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

        const val WORK_NAME = "video_upload_work"
        private const val MAX_RETRY_COUNT = 10
        private const val NOTIF_CHANNEL_ID = "video_upload_channel"
        private const val NOTIF_ID = 98765
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val workId = id
        val runAttempt = runAttemptCount

        Log.d("VideoUploadWorker", "🚀 Starting upload attempt $runAttempt for work: $workId")

        try {
            val initialProgress = workDataOf(
                "progress" to 0,
                "message" to "Iniciando...",
                "timestamp" to inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis()),
                KEY_SCALE to (inputData.getString(KEY_SCALE) ?: ""),
                KEY_SCALE_TYPE to (inputData.getString(KEY_SCALE_TYPE) ?: ""),
                KEY_DATE to (inputData.getString(KEY_DATE) ?: ""),
                KEY_TIME to (inputData.getString(KEY_TIME) ?: ""),
                KEY_BPM to inputData.getInt(KEY_BPM, 0),
                KEY_FIGURE to inputData.getDouble(KEY_FIGURE, 0.0),
                KEY_OCTAVES to inputData.getInt(KEY_OCTAVES, 0),
                KEY_DURATION to inputData.getInt(KEY_DURATION, 0),
                KEY_VIDEO_PATH to (inputData.getString(KEY_VIDEO_PATH) ?: "")
            )
            setProgress(initialProgress)
            setForegroundAsync(createForegroundInfo("Subiendo video... (intento $runAttempt)"))
        } catch (e: Exception) {
            Log.w("VideoUploadWorker", "⚠️ Could not set foreground: ${e.message}")
        }

        try {
            if (isStopped) return@withContext Result.failure(createFailureData("Worker was cancelled by system"))

            val videoUriString = inputData.getString(KEY_VIDEO_URI)
            val videoPath = inputData.getString(KEY_VIDEO_PATH)

            if (videoUriString.isNullOrEmpty() || videoPath.isNullOrEmpty()) {
                Log.e("VideoUploadWorker", "❌ Video URI or path missing in inputData")
                return@withContext Result.failure(createFailureData("Missing video URI or path"))
            }

            val videoUri = Uri.parse(videoUriString)
            val videoFile = File(videoPath)

            if (!videoFile.exists()) {
                Log.e("VideoUploadWorker", "❌ Video file not found at: $videoPath")
                return@withContext Result.failure(
                    createFailureData("Video file not found", runAttempt)
                        .toOutputWithVideoInfo(videoUri, videoPath)
                )
            }

            Log.d("VideoUploadWorker", "📤 Uploading from original location:")
            Log.d("VideoUploadWorker", "   📁 URI: $videoUri")
            Log.d("VideoUploadWorker", "   📁 Path: $videoPath")
            Log.d("VideoUploadWorker", "   📊 Size: ${videoFile.length() / 1024}KB")

            // Extraer todos los campos de Practice del inputData
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
            Log.d("VideoUploadWorker", "   ⏱️ Duration: ${duration}s")
            Log.d("VideoUploadWorker", "   🎼 BPM: $bpm, Figure: $figure")
            Log.d("VideoUploadWorker", "   🔢 Octaves: $octaves")

            setProgressSafely(10, "Preparando...", timestamp)

            val repository = PracticeRepositoryImpl()
            val useCase = RegisterPracticeUseCase(repository)

            setProgressSafely(40, "Iniciando upload...", timestamp)
            if (isStopped) return@withContext Result.failure(createFailureData("Worker cancelled"))

            setProgressSafely(60, "Enviando video...", timestamp)

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

            val result = useCase.execute(practice, videoFile)

            if (isStopped) return@withContext Result.failure(createFailureData("Worker cancelled"))

            return@withContext if (result.isSuccess) {
                val practiceResult = result.getOrNull()
                setProgressSafely(100, "Upload completado", timestamp)

                Log.d("VideoUploadWorker", "✅ Upload successful - Practice #${practiceResult?.practiceId}")
                Log.d("VideoUploadWorker", "📹 Video will remain in gallery at: $videoPath")

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
                    "attempts" to runAttempt,
                    "timestamp" to timestamp,
                    KEY_VIDEO_URI to videoUriString,
                    KEY_VIDEO_PATH to videoPath
                )

                Result.success(output)
            } else {
                val err = result.exceptionOrNull()
                handleUploadError(err, err?.message ?: "Unknown error", runAttempt, videoUri, videoPath, timestamp)
            }
        } catch (e: Exception) {
            Log.e("VideoUploadWorker", "💥 Exception in worker: ${e.message}", e)
            val videoUriString = inputData.getString(KEY_VIDEO_URI)
            val videoPath = inputData.getString(KEY_VIDEO_PATH)
            val videoUri = if (videoUriString != null) Uri.parse(videoUriString) else null
            handleUploadError(
                e,
                e.message ?: "Exception",
                runAttempt,
                videoUri,
                videoPath,
                inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
            )
        }
    }

    private suspend fun handleUploadError(
        error: Throwable?,
        errorMessage: String,
        attemptCount: Int,
        videoUri: Uri?,
        videoPath: String?,
        timestamp: Long
    ): Result {
        val isNetwork = isNetworkError(error, errorMessage)
        val shouldRetry = isNetwork && attemptCount < MAX_RETRY_COUNT

        Log.w("VideoUploadWorker", "❌ Upload error (attempt $attemptCount) network=$isNetwork -> $errorMessage")

        return if (shouldRetry) {
            setProgressSafely(0, "Error de conexión. Reintentando...", timestamp)
            Result.retry()
        } else {
            val failureData = createFailureData(errorMessage, attemptCount, timestamp)
            Result.failure(failureData.toOutputWithVideoInfo(videoUri, videoPath))
        }
    }

    private fun isNetworkError(error: Throwable?, errorMessage: String): Boolean {
        when (error) {
            is ConnectException, is SocketTimeoutException, is UnknownHostException -> return true
        }

        val keywords = listOf("network", "connection", "timeout", "unreachable", "Unable to resolve host")
        return keywords.any { errorMessage.contains(it, ignoreCase = true) }
    }

    private suspend fun setProgressSafely(progress: Int, message: String = "", timestamp: Long) {
        try {
            if (!isStopped) {
                val progressData = workDataOf(
                    "progress" to progress,
                    "message" to message,
                    "timestamp" to timestamp,
                    // Datos de práctica para mostrar en UI
                    KEY_SCALE to (inputData.getString(KEY_SCALE) ?: ""),
                    KEY_SCALE_TYPE to (inputData.getString(KEY_SCALE_TYPE) ?: ""),
                    KEY_DATE to (inputData.getString(KEY_DATE) ?: ""),
                    KEY_TIME to (inputData.getString(KEY_TIME) ?: ""),
                    KEY_BPM to inputData.getInt(KEY_BPM, 0),
                    KEY_FIGURE to inputData.getDouble(KEY_FIGURE, 0.0),
                    KEY_OCTAVES to inputData.getInt(KEY_OCTAVES, 0),
                    KEY_DURATION to inputData.getInt(KEY_DURATION, 0),
                    KEY_VIDEO_PATH to (inputData.getString(KEY_VIDEO_PATH) ?: "")
                )
                setProgress(progressData)
                Log.d("VideoUploadWorker", "📊 Progress: $progress% - $message")
            }
        } catch (e: Exception) {
            Log.e("VideoUploadWorker", "Error updating progress: ${e.message}", e)
        }
    }

    private fun createFailureData(
        error: String,
        attempts: Int = runAttemptCount,
        timestamp: Long = System.currentTimeMillis()
    ): Data {
        return workDataOf(
            "success" to false,
            "error" to error,
            "work_id" to id.toString(),
            "attempts" to attempts,
            "timestamp" to timestamp
        )
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