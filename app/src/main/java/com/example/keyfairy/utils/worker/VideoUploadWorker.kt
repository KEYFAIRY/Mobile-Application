package com.example.keyfairy.utils.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
        const val KEY_VIDEO_PATH = "video_path"
        const val KEY_UID = "uid"
        const val KEY_ESCALA_NAME = "escala_name"
        const val KEY_SCALE_TYPE = "scale_type"
        const val KEY_OCTAVES = "octaves"
        const val KEY_BPM = "bpm"
        const val KEY_DURATION = "duration"
        const val KEY_DATE = "date"
        const val KEY_TIME = "time"
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
            setForegroundAsync(createForegroundInfo("Subiendo video... (intento $runAttempt)"))
        } catch (e: Exception) {
            Log.w("VideoUploadWorker", "⚠️ Could not set foreground: ${e.message}")
            // Continuar sin foreground si falla
        }

        try {
            if (isStopped) return@withContext Result.failure(createFailureData("Worker was cancelled by system"))

            val videoPath = inputData.getString(KEY_VIDEO_PATH)
            if (videoPath.isNullOrEmpty()) {
                Log.e("VideoUploadWorker", "❌ Video path missing in inputData")
                return@withContext Result.failure(createFailureData("Missing video path"))
            }

            val videoFile = File(videoPath)
            if (!videoFile.exists()) {
                Log.e("VideoUploadWorker", "❌ Video file not found: $videoPath")
                // Return failure and include video path so manager/fragment can detect and cleanup.
                return@withContext Result.failure(createFailureData("Video file not found", runAttempt).toOutputWithPath(videoPath))
            }

            val uid = inputData.getString(KEY_UID).orEmpty()
            val escalaName = inputData.getString(KEY_ESCALA_NAME).orEmpty()
            val date = inputData.getString(KEY_DATE) ?: getCurrentDate()
            val time = inputData.getString(KEY_TIME) ?: getCurrentTime()
            val scaleType = inputData.getString(KEY_SCALE_TYPE) ?: determineScaleType(escalaName)
            val octaves = inputData.getInt(KEY_OCTAVES, 1)
            val bpm = inputData.getInt(KEY_BPM, 120)
            val duration = inputData.getInt(KEY_DURATION, 0)
            val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())

            // Report preparing progress
            setProgressSafely(10, "Preparando...", timestamp)

            // TODO: inject repository (example uses direct instantiation placeholder)
            val repository = PracticeRepositoryImpl()
            val useCase = RegisterPracticeUseCase(repository)

            setProgressSafely(40, "Iniciando upload...", timestamp)
            if (isStopped) return@withContext Result.failure(createFailureData("Worker cancelled"))

            setProgressSafely(60, "Enviando video...", timestamp)

            val practice = Practice(
                practiceId = 0,
                date = date,
                time = time,
                duration = duration,
                uid = uid,
                videoLocalRoute = videoPath,
                scale = escalaName,
                scaleType = scaleType,
                reps = octaves,
                bpm = bpm
            )

            val result = useCase.execute(practice, videoFile)

            if (isStopped) return@withContext Result.failure(createFailureData("Worker cancelled"))

            return@withContext if (result.isSuccess) {
                val practiceResult = result.getOrNull()
                setProgressSafely(100, "Upload completado", timestamp)

                // Build output data (include video path so UI/manager can map)
                val output = workDataOf(
                    "success" to true,
                    "practice_id" to (practiceResult?.practiceId ?: 0),
                    "scale" to (practiceResult?.scale ?: escalaName),
                    "attempts" to runAttempt,
                    "timestamp" to timestamp,
                    KEY_VIDEO_PATH to videoPath
                )

                // Delete local file only AFTER success (manager also does safe delete)
                cleanupVideoFile(videoPath)

                Result.success(output)
            } else {
                val err = result.exceptionOrNull()
                handleUploadError(err, err?.message ?: "Unknown error", runAttempt, videoPath, timestamp)
            }
        } catch (e: Exception) {
            Log.e("VideoUploadWorker", "💥 Exception in worker: ${e.message}", e)
            val videoPath = inputData.getString(KEY_VIDEO_PATH)
            handleUploadError(e, e.message ?: "Exception", runAttempt, videoPath, inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis()))
        }
    }

    private suspend fun handleUploadError(
        error: Throwable?,
        errorMessage: String,
        attemptCount: Int,
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
            // Final failure: include video path in output so manager/fragment can cancel/cleanup
            val failureData = createFailureData(errorMessage, attemptCount, timestamp)
            Result.failure(failureData.toOutputWithPath(videoPath))
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
                    "timestamp" to timestamp
                )
                setProgress(progressData) // ✅ ya no marca error
                Log.d("VideoUploadWorker", "📊 Progress: $progress - $message")
            }
        } catch (e: Exception) {
            Log.e("VideoUploadWorker", "Error updating progress: ${e.message}", e)
        }
    }


    private fun createFailureData(error: String, attempts: Int = runAttemptCount, timestamp: Long = System.currentTimeMillis()): androidx.work.Data {
        return workDataOf(
            "success" to false,
            "error" to error,
            "work_id" to id.toString(),
            "attempts" to attempts,
            "timestamp" to timestamp
        )
    }

    private fun androidx.work.Data.toOutputWithPath(videoPath: String?): androidx.work.Data {
        return if (!videoPath.isNullOrEmpty()) {
            androidx.work.workDataOf(*(this.keyValueMap.entries.map { it.key to it.value }.toTypedArray()), KEY_VIDEO_PATH to videoPath)
        } else this
    }

    private fun cleanupVideoFile(videoPath: String?) {
        if (videoPath.isNullOrEmpty()) return
        try {
            val f = File(videoPath)
            when {
                !f.exists() -> Log.d("VideoUploadWorker", "📂 File already deleted: ${f.name}")
                f.delete() -> Log.d("VideoUploadWorker", "🗑️ Video deleted by worker: ${f.name}")
                else -> Log.w("VideoUploadWorker", "⚠️ Could not delete file: ${f.name}")
            }
        } catch (e: Exception) {
            Log.e("VideoUploadWorker", "❌ Error cleaning file: ${e.message}", e)
        }
    }

    private fun determineScaleType(scaleName: String): String {
        return when {
            scaleName.contains("Major", ignoreCase = true) -> "Major"
            scaleName.contains("Minor", ignoreCase = true) -> "Minor"
            else -> "Major"
        }
    }

    private fun getCurrentDate(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

    private fun getCurrentTime(): String =
        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

    private fun createForegroundInfo(contentText: String): ForegroundInfo {
        val channelId = NOTIF_CHANNEL_ID
        val channelName = "Subida de videos"

        // ✅ Crear canal de notificación si es necesario
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

        // ✅ Especificar el tipo de servicio correcto
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