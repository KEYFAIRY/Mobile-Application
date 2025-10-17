package com.example.keyfairy.feature_progress.domain.usecase

import com.example.keyfairy.feature_progress.domain.model.NotasResumen
import com.example.keyfairy.feature_progress.domain.repository.ProgressRepository

class GetNotasResumenSemanalesUseCase (
    private val repository: ProgressRepository
){
    suspend fun execute(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<NotasResumen>> {
        return repository.getNotasResumenSemanales(idStudent, anio, semana)
    }
}