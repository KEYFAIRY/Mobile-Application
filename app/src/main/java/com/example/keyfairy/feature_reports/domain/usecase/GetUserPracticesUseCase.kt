package com.example.keyfairy.feature_reports.domain.usecase

import com.example.keyfairy.feature_reports.domain.model.PracticeList
import com.example.keyfairy.feature_reports.domain.repository.ReportsRepository

class GetUserPracticesUseCase(
    private val repository: ReportsRepository
) {
    suspend fun execute(
        uid: String,
        lastId: Int? = null,
        limit: Int = 10
    ): Result<PracticeList> {
        return repository.getUserPractices(uid, lastId, limit)
    }
}