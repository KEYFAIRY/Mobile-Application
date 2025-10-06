package com.example.keyfairy.feature_check_video.domain.use_case

import com.example.keyfairy.feature_check_video.domain.model.Practice
import com.example.keyfairy.feature_check_video.domain.repository.PracticeRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Register a practice with video upload
 */
class RegisterPracticeUseCase(
    private val practiceRepository: PracticeRepository
) {

    suspend fun execute(practice: Practice, videoFile: File): Result<Practice> {
        return when {
            practice.uid.isBlank() -> {
                Result.failure(Exception("UID de usuario es requerido"))
            }
            practice.scale.isBlank() -> {
                Result.failure(Exception("Escala musical es requerida"))
            }
            practice.scaleType.isBlank() -> {
                Result.failure(Exception("Tipo de escala es requerido"))
            }
            practice.duration <= 0 -> {
                Result.failure(Exception("Duración debe ser mayor a 0"))
            }
            practice.figure <= 0 -> {
                Result.failure(Exception("Repeticiones deben ser mayor a 0"))
            }
            practice.bpm <= 0 -> {
                Result.failure(Exception("BPM debe ser mayor a 0"))
            }
            practice.octaves <= 0 -> {
                Result.failure(Exception("Octavas deben ser mayor a 0"))
            }
            !videoFile.exists() -> {
                Result.failure(Exception("Archivo de video no encontrado"))
            }
            !isValidVideoFile(videoFile) -> {
                Result.failure(Exception("Formato de video no válido"))
            }
            isVideoTooLarge(videoFile) -> {
                Result.failure(Exception("El archivo de video es muy grande (máximo 100MB)"))
            }
            !isValidDate(practice.date) -> {
                Result.failure(Exception("Formato de fecha inválido (usar YYYY-MM-DD)"))
            }
            !isValidTime(practice.time) -> {
                Result.failure(Exception("Formato de hora inválido (usar HH:MM:SS)"))
            }
            else -> {
                practiceRepository.registerPractice(practice, videoFile)
            }
        }
    }

    private fun isValidVideoFile(file: File): Boolean {
        val validExtensions = listOf("mp4", "avi", "mov", "mkv", "webm")
        val extension = file.extension.lowercase()
        return extension in validExtensions
    }

    private fun isVideoTooLarge(file: File): Boolean {
        val maxSizeBytes = 100 * 1024 * 1024 // 100MB
        return file.length() > maxSizeBytes
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatter.isLenient = false
            formatter.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidTime(time: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            formatter.isLenient = false
            formatter.parse(time)
            true
        } catch (e: Exception) {
            false
        }
    }
}