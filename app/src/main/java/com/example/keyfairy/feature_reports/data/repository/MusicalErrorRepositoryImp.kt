package com.example.keyfairy.feature_reports.data.repository

import android.util.Log
import com.example.keyfairy.feature_reports.data.mapper.MusicalErrorMapper
import com.example.keyfairy.feature_reports.data.remote.api.MusicalErrorApi
import com.example.keyfairy.feature_reports.domain.model.MusicalErrorList
import com.example.keyfairy.feature_reports.domain.repository.MusicalErrorRepository
import retrofit2.HttpException
import java.net.HttpURLConnection

class MusicalErrorRepositoryImp (
    private val api: MusicalErrorApi
): MusicalErrorRepository {
    companion object {
        private const val TAG = "MusicalErrorRepositoryImp"
    }
    override suspend fun getMusicalErrors(practiceId: Int): Result<MusicalErrorList> {
        return try {
            Log.d(TAG, "Fetching musical errors for practice_id=$practiceId")

            val response = api.getMusicalErrors(practiceId)

            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null && body.data != null) {
                        val musicalErrors = MusicalErrorMapper.toDomain(body.data!!)
                        Log.d(TAG, "✅ Successfully fetched ${musicalErrors.numErrors} musical errors")
                        Result.success(musicalErrors)
                    } else {
                        Log.e(TAG, "❌ Response body or data is null")
                        Result.failure(Exception("No se encontraron datos de errores musicales"))
                    }
                }

                response.code() == HttpURLConnection.HTTP_NOT_FOUND -> {
                    Log.i(TAG, "📭 No musical errors found (404) - returning empty list")
                    val emptyErrorList = MusicalErrorList(
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
                Log.i(TAG, "📭 No musical errors found (404 HttpException) - returning empty list")
                val emptyErrorList = MusicalErrorList(
                    numErrors = 0,
                    errors = emptyList()
                )
                Result.success(emptyErrorList)
            } else {
                Log.e(TAG, "❌ HTTP Exception fetching musical errors: ${e.code()} - ${e.message()}", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception fetching musical errors: ${e.message}", e)
            Result.failure(e)
        }
    }
}