package com.example.keyfairy.feature_progress.domain.usecase

import com.example.keyfairy.feature_progress.domain.model.TiempoPosturas
import com.example.keyfairy.feature_progress.domain.repository.ProgressRepository

class GetTiempoPosturasSemanalesUseCase(
    private val repository: ProgressRepository
) {
    suspend fun execute(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<TiempoPosturas>> {
        return repository.getTiempoPosturasSemanales(idStudent, anio, semana)
    }
}