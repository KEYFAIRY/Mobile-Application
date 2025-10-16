package com.example.keyfairy.utils.worker

import androidx.work.WorkInfo
import com.example.keyfairy.feature_home.domain.model.PendingVideo
import com.example.keyfairy.utils.workers.VideoUploadWorker
import android.util.Log

fun WorkInfo.toPendingVideo(): PendingVideo {
    return try {
        Log.d("WorkInfoExtensions", "🔍 Converting WorkInfo ID: $id, state=$state")

        val currentAttempt = progress.getInt("current_attempt", 0)
            .takeIf { it > 0 }
            ?: outputData.getInt("current_attempt", 0)
                .takeIf { it > 0 }
            ?: outputData.getInt("attempts", 0)
                .takeIf { it > 0 }
            ?: runAttemptCount.takeIf { it > 0 }
            ?: 1

        val statusMessage = progress.getString("message")
            ?: outputData.getString("message")
            ?: getDefaultStatusMessage(state)

        val scale = progress.getString(VideoUploadWorker.KEY_SCALE)
            ?: outputData.getString(VideoUploadWorker.KEY_SCALE)
            ?: extractFromTags("scale:")
            ?: "Escala desconocida"

        val scaleType = progress.getString(VideoUploadWorker.KEY_SCALE_TYPE)
            ?: outputData.getString(VideoUploadWorker.KEY_SCALE_TYPE)
            ?: extractFromTags("scaleType:")
            ?: "Mayor"

        val date = progress.getString(VideoUploadWorker.KEY_DATE)
            ?: outputData.getString(VideoUploadWorker.KEY_DATE)
            ?: extractFromTags("date:")
            ?: getCurrentDate()

        val time = progress.getString(VideoUploadWorker.KEY_TIME)
            ?: outputData.getString(VideoUploadWorker.KEY_TIME)
            ?: extractFromTags("time:")
            ?: getCurrentTime()

        val bpm = progress.getInt(VideoUploadWorker.KEY_BPM, 0)
            .takeIf { it > 0 }
            ?: outputData.getInt(VideoUploadWorker.KEY_BPM, 0)
                .takeIf { it > 0 }
            ?: extractIntFromTags("bpm:")
            ?: 120

        val figure = progress.getDouble(VideoUploadWorker.KEY_FIGURE, 0.0)
            .takeIf { it > 0.0 }
            ?: outputData.getDouble(VideoUploadWorker.KEY_FIGURE, 0.0)
                .takeIf { it > 0.0 }
            ?: extractDoubleFromTags("figure:")
            ?: 1.0

        val octaves = progress.getInt(VideoUploadWorker.KEY_OCTAVES, 0)
            .takeIf { it > 0 }
            ?: outputData.getInt(VideoUploadWorker.KEY_OCTAVES, 0)
                .takeIf { it > 0 }
            ?: extractIntFromTags("octaves:")
            ?: 1

        val duration = progress.getInt(VideoUploadWorker.KEY_DURATION, 0)
            .takeIf { it > 0 }
            ?: outputData.getInt(VideoUploadWorker.KEY_DURATION, 0)
                .takeIf { it > 0 }
            ?: extractIntFromTags("duration:")
            ?: 0

        val videoPath = progress.getString(VideoUploadWorker.KEY_VIDEO_PATH)
            ?: outputData.getString(VideoUploadWorker.KEY_VIDEO_PATH)
            ?: ""

        val timestamp = progress.getLong(VideoUploadWorker.KEY_TIMESTAMP, 0L)
            .takeIf { it > 0 }
            ?: outputData.getLong(VideoUploadWorker.KEY_TIMESTAMP, 0L)
                .takeIf { it > 0 }
            ?: extractLongFromTags("timestamp:")
            ?: System.currentTimeMillis()

        // Progreso de subida
        val uploadProgress = progress.getInt("progress", 0)


        Log.d("WorkInfoExtensions", "✅ Parsed: $scale ($scaleType) - State: $state")
        Log.d("WorkInfoExtensions", "   📅 Date: $date, $time")
        Log.d("WorkInfoExtensions", "   🎵 BPM: $bpm, Octaves: $octaves, Figure: $figure")
        Log.d("WorkInfoExtensions", "   📊 Progress: $uploadProgress%, Attempts: $currentAttempt")
        Log.d("WorkInfoExtensions", "   💬 Status message: $statusMessage")

        PendingVideo(
            workId = id,
            scaleName = scale,
            scaleType = scaleType,
            status = state,
            message = statusMessage,
            progress = uploadProgress,
            attempts = currentAttempt,
            timestamp = timestamp,
            date = date,
            time = time,
            bpm = bpm,
            figure = figure,
            octaves = octaves,
            videoPath = videoPath
        )

    } catch (e: Exception) {
        Log.e("WorkInfoExtensions", "❌ Error parsing WorkInfo: ${e.message}", e)
        createFallbackPendingVideo(this)
    }
}

/**
 * Obtiene un mensaje de estado por defecto solo si el Worker no proporcionó uno
 */
private fun getDefaultStatusMessage(state: WorkInfo.State): String {
    return when (state) {
        WorkInfo.State.ENQUEUED -> "En cola"
        WorkInfo.State.RUNNING -> VideoUploadWorker.STATUS_UPLOADING
        WorkInfo.State.BLOCKED -> VideoUploadWorker.STATUS_WAITING_INTERNET
        WorkInfo.State.SUCCEEDED -> "Completado"
        WorkInfo.State.FAILED -> "Falló"
        WorkInfo.State.CANCELLED -> "Cancelado"
    }
}

private fun WorkInfo.extractFromTags(prefix: String): String? {
    return tags.find { it.startsWith(prefix) }?.substringAfter(prefix)
}

private fun WorkInfo.extractIntFromTags(prefix: String): Int? {
    return extractFromTags(prefix)?.toIntOrNull()
}

private fun WorkInfo.extractDoubleFromTags(prefix: String): Double? {
    return extractFromTags(prefix)?.toDoubleOrNull()
}

private fun WorkInfo.extractLongFromTags(prefix: String): Long? {
    return extractFromTags(prefix)?.toLongOrNull()
}

private fun createFallbackPendingVideo(workInfo: WorkInfo): PendingVideo {
    val attempts = workInfo.outputData.getInt("attempts", 0).takeIf { it > 0 }
        ?: workInfo.runAttemptCount

    // Intentar extraer al menos la escala de los tags como último recurso
    val scaleFromTags = workInfo.extractFromTags("scale:") ?: "Datos incompletos"

    Log.w("WorkInfoExtensions", "⚠️ Using FALLBACK PendingVideo for work: ${workInfo.id}")
    Log.w("WorkInfoExtensions", "   Scale from tags: $scaleFromTags")

    return PendingVideo(
        workId = workInfo.id,
        scaleName = scaleFromTags,
        scaleType = workInfo.extractFromTags("scaleType:") ?: "",
        status = workInfo.state,
        message = "Desconocido",
        progress = workInfo.progress.getInt("progress", 0),
        attempts = attempts,
        timestamp = workInfo.extractLongFromTags("timestamp:") ?: System.currentTimeMillis(),
        date = workInfo.extractFromTags("date:") ?: getCurrentDate(),
        time = workInfo.extractFromTags("time:") ?: getCurrentTime(),
        bpm = workInfo.extractIntFromTags("bpm:") ?: 120,
        figure = workInfo.extractDoubleFromTags("figure:") ?: 1.0,
        octaves = workInfo.extractIntFromTags("octaves:") ?: 1,
        videoPath = ""
    )
}

private fun getCurrentDate(): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date())
}

private fun getCurrentTime(): String {
    return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        .format(java.util.Date())
}