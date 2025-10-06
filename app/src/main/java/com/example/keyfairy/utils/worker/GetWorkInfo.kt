package com.example.keyfairy.utils.worker

import androidx.work.WorkInfo
import com.example.keyfairy.feature_home.domain.model.PendingVideo
import com.example.keyfairy.utils.workers.VideoUploadWorker
import android.util.Log

fun WorkInfo.toPendingVideo(): PendingVideo {
    return try {
        Log.d("WorkInfoExtensions", "🔍 Converting WorkInfo ID: $id, state=$state")

        // WorkInfo solo tiene acceso a 'progress' y 'outputData', NO a inputData
        // Los datos están en progress (actualizados por setProgressSafely en el Worker)
        val scale = progress.getString(VideoUploadWorker.KEY_SCALE)
            ?: outputData.getString(VideoUploadWorker.KEY_SCALE)
        val scaleType = progress.getString(VideoUploadWorker.KEY_SCALE_TYPE)
            ?: outputData.getString(VideoUploadWorker.KEY_SCALE_TYPE)
        val date = progress.getString(VideoUploadWorker.KEY_DATE)
            ?: outputData.getString(VideoUploadWorker.KEY_DATE)
        val time = progress.getString(VideoUploadWorker.KEY_TIME)
            ?: outputData.getString(VideoUploadWorker.KEY_TIME)

        val bpm = progress.getInt(VideoUploadWorker.KEY_BPM, 0)
            .takeIf { it > 0 }
            ?: outputData.getInt(VideoUploadWorker.KEY_BPM, 0)

        val figure = progress.getDouble(VideoUploadWorker.KEY_FIGURE, 0.0)
            .takeIf { it > 0.0 }
            ?: outputData.getDouble(VideoUploadWorker.KEY_FIGURE, 0.0)

        val octaves = progress.getInt(VideoUploadWorker.KEY_OCTAVES, 0)
            .takeIf { it > 0 }
            ?: outputData.getInt(VideoUploadWorker.KEY_OCTAVES, 0)

        val videoPath = progress.getString(VideoUploadWorker.KEY_VIDEO_PATH)
            ?: outputData.getString(VideoUploadWorker.KEY_VIDEO_PATH) ?: ""

        val timestamp = progress.getLong(VideoUploadWorker.KEY_TIMESTAMP, 0L)
            .takeIf { it > 0 }
            ?: outputData.getLong(VideoUploadWorker.KEY_TIMESTAMP, System.currentTimeMillis())

        // Validar que los datos críticos existen
        if (scale.isNullOrEmpty() || date.isNullOrEmpty() || time.isNullOrEmpty()) {
            Log.e("WorkInfoExtensions", "❌ Missing critical data for work: $id")
            Log.e("WorkInfoExtensions", "   scale=$scale, date=$date, time=$time")
            return createFallbackPendingVideo(this)
        }

        // Progreso de subida
        val uploadProgress = progress.getInt("progress", 0)

        // Intentos
        val attempts = outputData.getInt("attempts", 0).takeIf { it > 0 } ?: runAttemptCount

        Log.d("WorkInfoExtensions", "✅ Parsed: $scale ($scaleType)")
        Log.d("WorkInfoExtensions", "   📅 Date: $date, $time")
        Log.d("WorkInfoExtensions", "   🎵 BPM: $bpm, Octaves: $octaves, Figure: $figure")
        Log.d("WorkInfoExtensions", "   📊 Progress: $uploadProgress%, Attempts: $attempts")

        PendingVideo(
            workId = id,
            scaleName = scale,
            scaleType = scaleType ?: "",
            status = state,
            progress = uploadProgress,
            attempts = attempts,
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

private fun createFallbackPendingVideo(workInfo: WorkInfo): PendingVideo {
    val attempts = workInfo.outputData.getInt("attempts", 0).takeIf { it > 0 }
        ?: workInfo.runAttemptCount

    Log.e("WorkInfoExtensions", "⚠️⚠️⚠️ Using FALLBACK PendingVideo for work: ${workInfo.id}")
    Log.e("WorkInfoExtensions", "   This indicates missing data - investigate!")

    return PendingVideo(
        workId = workInfo.id,
        scaleName = "⚠️ Datos incompletos",
        scaleType = "",
        status = workInfo.state,
        progress = workInfo.progress.getInt("progress", 0),
        attempts = attempts,
        timestamp = System.currentTimeMillis(),
        date = getCurrentDate(),
        time = getCurrentTime(),
        bpm = 0,
        figure = 0.0,
        octaves = 0,
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