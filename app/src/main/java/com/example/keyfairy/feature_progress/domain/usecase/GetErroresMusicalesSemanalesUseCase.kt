package com.example.keyfairy.feature_progress.domain.usecase

import com.example.keyfairy.feature_progress.domain.model.ErroresMusicales
import com.example.keyfairy.feature_progress.domain.repository.ProgressRepository


class GetErroresMusicalesSemanalesUseCase (
    private val repository: ProgressRepository
){
    suspend fun execute(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<ErroresMusicales>> {
        return repository.getErroresMusicalesSemanales(idStudent, anio, semana)
    }
}