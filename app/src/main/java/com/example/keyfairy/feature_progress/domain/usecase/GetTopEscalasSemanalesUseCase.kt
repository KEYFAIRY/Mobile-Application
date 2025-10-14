package com.example.keyfairy.feature_progress.domain.usecase


import com.example.keyfairy.feature_progress.domain.model.TopEscalasSemanales
import com.example.keyfairy.feature_progress.domain.repository.ProgressRepository


/**
 * Use case to retrieve the top practiced scales for a specific week.
 */

class GetTopEscalasSemanalesUseCase (
    private val repository: ProgressRepository
) {
    suspend fun execute(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<TopEscalasSemanales>> {
        return repository.getTopEscalasSemanales(idStudent, anio, semana)
    }
}
