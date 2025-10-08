package com.example.keyfairy.feature_reports.domain.repository

import com.example.keyfairy.feature_reports.domain.model.MusicalErrorList

interface MusicalErrorRepository {
    suspend fun getMusicalErrors(
        practiceId: Int
    ): Result<MusicalErrorList>
}