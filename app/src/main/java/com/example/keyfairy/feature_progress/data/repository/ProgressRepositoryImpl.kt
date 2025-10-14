package com.example.keyfairy.feature_progress.data.repository


import android.util.Log
import com.example.keyfairy.feature_progress.data.mapper.ProgressMapper
import com.example.keyfairy.feature_progress.data.remote.api.ProgressApi
import com.example.keyfairy.feature_progress.domain.model.*
import com.example.keyfairy.feature_progress.domain.repository.ProgressRepository
import retrofit2.HttpException
import java.net.HttpURLConnection


class ProgressRepositoryImpl(
    private val api: ProgressApi
) : ProgressRepository {

    companion object {
        private const val TAG = "ProgressRepositoryImpl"
    }

    override suspend fun getTopEscalasSemanales(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<TopEscalasSemanales>> = safeApiCall(
        call = { api.getTopEscalasSemanales(idStudent, anio, semana) },
        mapper = { it.data.map { dto -> ProgressMapper.toDomain(dto) } },
        context = "top scales"
    )

    override suspend fun getTiempoPosturasSemanales(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<TiempoPosturas>> = safeApiCall(
        call = { api.getTiempoPosturasSemanales(idStudent, anio, semana) },
        mapper = { it.data.map { dto -> ProgressMapper.toDomain(dto) } },
        context = "posture times"
    )

    override suspend fun getNotasResumenSemanales(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<NotasResumen>> = safeApiCall(
        call = { api.getNotasResumenSemanales(idStudent, anio, semana) },
        mapper = { it.data.map { dto -> ProgressMapper.toDomain(dto) } },
        context = "note summary"
    )

    override suspend fun getErroresPosturalesSemanales(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<ErroresPosturales>> = safeApiCall(
        call = { api.getErroresPosturalesSemanales(idStudent, anio, semana) },
        mapper = { it.data.map { dto -> ProgressMapper.toDomain(dto) } },
        context = "postural errors"
    )

    override suspend fun getErroresMusicalesSemanales(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<ErroresMusicales>> = safeApiCall(
        call = { api.getErroresMusicalesSemanales(idStudent, anio, semana) },
        mapper = { it.data.map { dto -> ProgressMapper.toDomain(dto) } },
        context = "musical errors"
    )


    private suspend fun <DTO, Domain> safeApiCall(
        call: suspend () -> retrofit2.Response<DTO>,
        mapper: (DTO) -> Domain,
        context: String
    ): Result<Domain> {
        return try {
            val response = call()
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null) {
                        Result.success(mapper(body))
                    } else {
                        Log.e(TAG, "❌ Response body is null for $context")
                        Result.failure(Exception("No se encontraron datos de $context"))
                    }
                }
                response.code() == HttpURLConnection.HTTP_NOT_FOUND -> {
                    Log.i(TAG, "📭 Resource not found (404) for $context - returning empty list")
                    @Suppress("UNCHECKED_CAST")
                    Result.success(emptyList<Any>() as Domain)
                }
                else -> {
                    val errorMsg = "Error ${response.code()}: ${response.message()}"
                    Log.e(TAG, "❌ $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: HttpException) {
            if (e.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.i(TAG, "📭 404 HttpException for $context - returning empty list")
                @Suppress("UNCHECKED_CAST")
                Result.success(emptyList<Any>() as Domain)
            } else {
                Log.e(TAG, "❌ HTTP Exception fetching $context: ${e.message}", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception fetching $context: ${e.message}", e)
            Result.failure(e)
        }
    }
}