package com.example.keyfairy.feature_reports.domain.usecase

import android.util.Log
import com.example.keyfairy.feature_reports.domain.model.MusicalErrorList
import com.example.keyfairy.feature_reports.domain.repository.MusicalErrorRepository

class GetMusicalErrorsUseCase (
    private val repository: MusicalErrorRepository
) {

    companion object {
        private const val TAG = "GetMusicalErrorsUseCase"
    }

    suspend fun execute(practiceId: Int): Result<MusicalErrorList> {
        Log.d(TAG, "Executing use case for practice_id=$practiceId")

        if (practiceId <= 0) {
            Log.e(TAG, "Invalid practice ID: $practiceId")
            return Result.failure(Exception("ID de práctica inválido"))
        }
        return repository.getMusicalErrors(practiceId)
    }

}