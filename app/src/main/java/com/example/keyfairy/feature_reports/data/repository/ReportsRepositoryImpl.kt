package com.example.keyfairy.feature_reports.data.repository

import android.util.Log
import com.example.keyfairy.feature_reports.data.mapper.PosturalErrorMapper
import com.example.keyfairy.feature_reports.data.mapper.PracticeMapper
import com.example.keyfairy.feature_reports.data.remote.api.ReportsApi
import com.example.keyfairy.feature_reports.domain.model.PosturalErrorList
import com.example.keyfairy.feature_reports.domain.model.PracticeList
import com.example.keyfairy.feature_reports.domain.repository.ReportsRepository
import retrofit2.HttpException
import java.net.HttpURLConnection

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
}