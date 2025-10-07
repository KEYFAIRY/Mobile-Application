package com.example.keyfairy.feature_reports.domain.repository

import com.example.keyfairy.feature_reports.domain.model.PosturalErrorList

interface PosturalErrorRepository {
    suspend fun getPosturalErrors(
        practiceId: Int
    ): Result<PosturalErrorList>
}