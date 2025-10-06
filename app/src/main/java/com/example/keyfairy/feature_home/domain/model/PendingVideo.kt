package com.example.keyfairy.feature_home.domain.model

import androidx.work.WorkInfo
import com.example.keyfairy.utils.enums.Figure
import java.text.SimpleDateFormat
import java.util.*

data class PendingVideo(
    val workId: UUID,
    val scaleName: String,
    val scaleType: String,
    val status: WorkInfo.State,
    val progress: Int,
    val attempts: Int,
    val timestamp: Long,
    val date: String,
    val time: String,
    val bpm: Int,
    val figure: Double,
    val octaves: Int,
    val videoPath: String
) {
    fun getStatusText(): String {
        return when (status) {
            WorkInfo.State.ENQUEUED -> "En cola"
            WorkInfo.State.RUNNING -> if (progress > 0) "$progress%" else "Subiendo..."
            WorkInfo.State.BLOCKED -> "Sin conexión"
            WorkInfo.State.FAILED -> "Error (${attempts}/10)"
            WorkInfo.State.SUCCEEDED -> "Completado"
            WorkInfo.State.CANCELLED -> "Cancelado"
        }
    }

    fun getVideoDate(): String {
        return if (date.isNotEmpty() && time.isNotEmpty()) {
            // Formato: "2024-01-15, 14:30"
            val dateParts = date.split("-")
            if (dateParts.size == 3) {
                "${dateParts[2]}/${dateParts[1]}/${dateParts[0]}, ${time.substring(0, 5)}"
            } else {
                "$date, $time"
            }
        } else {
            // Fallback: usar timestamp
            val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }

    fun getVideoDetails(): String {
        return buildString {
            if (bpm > 0) append("$bpm bpm")
            if (octaves > 0) {
                if (isNotEmpty()) append(" • ")
                append("$octaves octava${if (octaves > 1) "s" else ""}")
            }
            if (figure > 0.0) {
                if (isNotEmpty()) append(" • ")
                append(Figure.fromValue(figure))
            }
        }
    }
}