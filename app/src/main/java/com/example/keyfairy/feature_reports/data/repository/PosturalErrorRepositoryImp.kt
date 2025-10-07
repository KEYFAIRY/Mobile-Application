package com.example.keyfairy.feature_reports.data.repository

import android.util.Log
import com.example.keyfairy.feature_reports.data.mapper.PosturalErrorMapper
import com.example.keyfairy.feature_reports.data.remote.api.PosturalErrorApi
import com.example.keyfairy.feature_reports.domain.model.PosturalErrorList
import com.example.keyfairy.feature_reports.domain.repository.PosturalErrorRepository
import retrofit2.HttpException
import java.net.HttpURLConnection

class PosturalErrorRepositoryImp(
    private val api: PosturalErrorApi
): PosturalErrorRepository {

    companion object {
        private const val TAG = "PosturalErrorsRepositoryImpl"
    }

    override suspend fun getPosturalErrors(
        practiceId: Int
    ): Result<PosturalErrorList> {
        return try {
            Log.d(TAG, "Fetching postural errors for practice_id=$practiceId")

            val response = api.getPosturalErrors(practiceId)

            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null && body.data != null) {
                        val posturalErrors = PosturalErrorMapper.toDomain(body.data!!)
                        Log.d(TAG, "✅ Successfully fetched ${posturalErrors.numErrors} postural errors")
                        Result.success(posturalErrors)
                    } else {
                        Log.e(TAG, "❌ Response body or data is null")
                        Result.failure(Exception("No se encontraron datos de errores posturales"))
                    }
                }

                response.code() == HttpURLConnection.HTTP_NOT_FOUND -> {
                    Log.i(TAG, "📭 No postural errors found (404) - returning empty list")
                    val emptyErrorList = PosturalErrorList(
                        numErrors = 0,
                        errors = emptyList()
                    )
                    Result.success(emptyErrorList)
                }

                else -> {
                    val errorMsg = "Error ${response.code()}: ${response.message()}"
                    Log.e(TAG, "❌ $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: HttpException) {
            return if (e.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.i(TAG, "📭 No postural errors found (404 HttpException) - returning empty list")
                val emptyErrorList = PosturalErrorList(
                    numErrors = 0,
                    errors = emptyList()
                )
                Result.success(emptyErrorList)
            } else {
                Log.e(TAG, "❌ HTTP Exception fetching postural errors: ${e.code()} - ${e.message()}", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception fetching postural errors: ${e.message}", e)
            Result.failure(e)
        }
    }
}