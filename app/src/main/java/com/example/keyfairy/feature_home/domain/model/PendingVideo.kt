package com.example.keyfairy.feature_home.domain.model

import androidx.work.WorkInfo
import java.util.*

data class PendingVideo(
    val workId: UUID,
    val date: String,
    val time: String,
    val scaleName: String,
    val scaleType: String,
    val bpm: Int,
    val octaves: Int,
    val duration: Int,
    val videoPath: String,
    val status: WorkInfo.State,
    val progress: Int = 0,
    val attempts: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    fun getStatusText(): String {
        return when (status) {
            WorkInfo.State.ENQUEUED -> "En cola"
            WorkInfo.State.RUNNING -> if (progress > 0) "Subiendo $progress%" else "Subiendo..."
            WorkInfo.State.SUCCEEDED -> "Completado"
            WorkInfo.State.FAILED -> "Error: ${errorMessage ?: "Desconocido"}"
            WorkInfo.State.BLOCKED -> "Esperando conexión"
            WorkInfo.State.CANCELLED -> "Cancelado"
        }
    }

    fun getVideoDetails(): String {
        return "$bpm BPM • $octaves octava${if (octaves > 1) "s" else ""} • ${duration}s"
    }

    fun getVideoDate(): String {
        return "$date , $time"
    }
}