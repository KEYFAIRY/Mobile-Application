package com.example.keyfairy.feature_reports.domain.repository

import com.example.keyfairy.feature_reports.domain.model.PracticeList

interface ReportsRepository {
    suspend fun getUserPractices(
        uid: String,
        lastId: Int?,
        limit: Int
    ): Result<PracticeList>
}