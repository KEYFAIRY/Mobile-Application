package com.example.keyfairy.feature_reports.domain.usecase

import android.util.Log
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.feature_reports.domain.repository.ReportsRepository

class GetPracticeByIdUseCase (
    private val repository: ReportsRepository
) {
    companion object {
        private const val TAG = "GetPracticeByIdUseCase"
    }

    suspend fun execute(uid: String, practiceId: Int): Result<Practice> {
        Log.d(TAG, "Executing GetPracticeById for uid=$uid, practiceId=$practiceId")

        if (uid.isBlank()) {
            Log.e(TAG, "UID is blank")
            return Result.failure(IllegalArgumentException("UID no puede estar vacío"))
        }

        if (practiceId <= 0) {
            Log.e(TAG, "Invalid practiceId: $practiceId")
            return Result.failure(IllegalArgumentException("ID de práctica inválido"))
        }

        return repository.getPracticeById(uid, practiceId)
    }
}