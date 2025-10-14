package com.example.keyfairy.feature_progress.domain.usecase

import com.example.keyfairy.feature_progress.domain.model.ErroresPosturales
import com.example.keyfairy.feature_progress.domain.repository.ProgressRepository

class GetErroresPosturalesSemanalesUseCase (
    private val repository: ProgressRepository
){
    suspend fun execute(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<ErroresPosturales>> {
        return repository.getErroresPosturalesSemanales(idStudent, anio, semana)
    }
}