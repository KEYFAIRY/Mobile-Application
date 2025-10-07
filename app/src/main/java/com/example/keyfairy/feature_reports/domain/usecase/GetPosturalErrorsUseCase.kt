package com.example.keyfairy.feature_reports.domain.usecase

import android.util.Log
import com.example.keyfairy.feature_reports.domain.model.PosturalErrorList
import com.example.keyfairy.feature_reports.domain.repository.PosturalErrorRepository

class GetPosturalErrorsUseCase (
    private val repository: PosturalErrorRepository
) {
    companion object {
        private const val TAG = "GetPosturalErrorsUseCase"
    }

    suspend fun execute(practiceId: Int): Result<PosturalErrorList> {
        Log.d(TAG, "Executing use case for practice_id=$practiceId")

        if (practiceId <= 0) {
            Log.e(TAG, "Invalid practice ID: $practiceId")
            return Result.failure(Exception("ID de práctica inválido"))
        }
        return repository.getPosturalErrors(practiceId)
    }
}