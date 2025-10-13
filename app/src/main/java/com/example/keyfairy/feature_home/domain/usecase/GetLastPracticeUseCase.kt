package com.example.keyfairy.feature_home.domain.usecase

import android.util.Log
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.feature_reports.domain.repository.ReportsRepository

class GetLastPracticeUseCase (
    private val repository: ReportsRepository
) {

    companion object {
        private const val TAG = "GetLastPracticeUseCase"
    }

    suspend fun execute(
        uid: String,
        lastId: Int? = null,
    ): Result<Practice?> {
        Log.d(TAG, "execute: Starting to get last practice for uid=$uid, lastId=$lastId")

        return try {
            val result = repository.getUserPractices(uid, lastId, 1)
            result.fold(
                onSuccess = { practiceList ->
                    Log.d(TAG, "execute: Successfully retrieved ${practiceList.numPractices} practices")
                    val lastPractice = practiceList.practices.firstOrNull()
                    if (lastPractice != null) {
                        Log.d(TAG, "execute: Found last practice with id=${lastPractice.practiceId}")
                    } else {
                        Log.w(TAG, "execute: No practices found for user")
                    }
                    Result.success(lastPractice)
                },
                onFailure = { exception ->
                    Log.e(TAG, "execute: Failed to retrieve practices", exception)
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "execute: Unexpected error occurred", e)
            Result.failure(e)
        }
    }
}