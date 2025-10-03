package com.example.keyfairy.utils.worker

import androidx.work.WorkInfo
import com.example.keyfairy.feature_home.domain.model.PendingVideo
import android.util.Log

fun WorkInfo.toPendingVideo(): PendingVideo {
    return try {
        Log.d("WorkInfoExtensions", "🔍 Converting WorkInfo ID: ${this.id}, state=${this.state}")

        val dataTag = this.tags.firstOrNull { it.startsWith("DATA:") }
        if (dataTag != null) {
            val parts = dataTag.removePrefix("DATA:").split("|")
            if (parts.size >= 8) {
                val scaleName = parts[0]
                val scaleType = parts[1]
                val bpm = parts[2].toIntOrNull() ?: 120
                val octaves = parts[3].toIntOrNull() ?: 1
                val duration = parts[4].toIntOrNull() ?: 30
                val date = parts[5]
                val time = parts[6]
                val timestamp = parts[7].toLongOrNull() ?: System.currentTimeMillis()

                val attempts = if (this.outputData.keyValueMap.isNotEmpty()) {
                    this.outputData.getInt("attempts", this.runAttemptCount)
                } else runAttemptCount

                val progress = this.progress.getInt("progress", 0)
                val errorMessage = this.outputData.getString("error")

                return PendingVideo(
                    workId = this.id,
                    date = date,
                    time = time,
                    scaleName = scaleName,
                    scaleType = scaleType,
                    bpm = bpm,
                    octaves = octaves,
                    duration = duration,
                    videoPath = "", // 🔒 oculto
                    status = this.state,
                    progress = progress,
                    attempts = attempts,
                    timestamp = timestamp,
                    errorMessage = errorMessage
                )
            }
        }

        Log.w("WorkInfoExtensions", "⚠️ No DATA tag found or malformed, fallback")
        createFallbackPendingVideo(this)

    } catch (e: Exception) {
        Log.e("WorkInfoExtensions", "❌ Error parsing WorkInfo: ${e.message}", e)
        createFallbackPendingVideo(this)
    }
}

private fun createFallbackPendingVideo(workInfo: WorkInfo): PendingVideo {
    val attempts = if (workInfo.outputData.keyValueMap.isNotEmpty()) {
        workInfo.outputData.getInt("attempts", workInfo.runAttemptCount)
    } else workInfo.runAttemptCount

    return PendingVideo(
        workId = workInfo.id,
        date = getCurrentDate(),
        time = getCurrentTime(),
        scaleName = "Práctica musical",
        scaleType = "Major",
        bpm = 120,
        octaves = 1,
        duration = 30,
        videoPath = "",
        status = workInfo.state,
        progress = workInfo.progress.getInt("progress", 0),
        attempts = attempts,
        timestamp = System.currentTimeMillis(),
        errorMessage = workInfo.outputData.getString("error")
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
