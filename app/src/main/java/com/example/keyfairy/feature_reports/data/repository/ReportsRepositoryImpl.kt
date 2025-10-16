package com.example.keyfairy.feature_reports.data.repository

import android.util.Log
import com.example.keyfairy.feature_reports.data.mapper.PracticeMapper
import com.example.keyfairy.feature_reports.data.remote.api.ReportsApi
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.feature_reports.domain.model.PracticeList
import com.example.keyfairy.feature_reports.domain.repository.ReportsRepository


class ReportsRepositoryImpl(
    private val api: ReportsApi
) : ReportsRepository {

    companion object {
        private const val TAG = "ReportsRepositoryImpl"
    }

    override suspend fun getUserPractices(
        uid: String,
        lastId: Int?,
        limit: Int
    ): Result<PracticeList> {
        return try {
            Log.d(TAG, "Fetching practices for uid=$uid, lastId=$lastId, limit=$limit")

            val response = api.getUserPractices(uid, lastId, limit)

            if (response.isSuccessful && response.body()?.data != null) {
                val practiceResponseDto = response.body()!!.data!!
                val practiceList = PracticeMapper.toDomain(practiceResponseDto)

                Log.d(TAG, "Successfully fetched ${practiceList.numPractices} practices")
                Result.success(practiceList)
            } else {
                val errorMsg = "Error fetching practices: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = "Exception fetching practices: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Result.failure(e)
        }
    }

    override suspend fun getPracticeById(
        uid: String,
        practiceId: Int
    ): Result<Practice> {
        return try {
            Log.d(TAG, "Fetching practice id=$practiceId for uid=$uid")

            val response = api.getPracticeById(uid, practiceId)

            if (response.isSuccessful && response.body()?.data != null) {
                val practiceItemDto = response.body()!!.data!!
                val practice = PracticeMapper.toDomain(practiceItemDto)

                Log.d(TAG, "Successfully fetched practice $practiceId with state '${practice.state}'")
                Result.success(practice)
            } else {
                val errorMsg = "Error fetching practice: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = "Exception fetching practice: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Result.failure(e)
        }
    }
}