package com.example.keyfairy.feature_check_video.data.repository

import android.util.Log
import com.example.keyfairy.feature_check_video.data.mapper.PracticeMapper
import com.example.keyfairy.feature_check_video.data.remote.api.PracticeApi
import com.example.keyfairy.feature_check_video.domain.model.Practice
import com.example.keyfairy.feature_check_video.domain.model.PracticeResult
import com.example.keyfairy.feature_check_video.domain.repository.PracticeRepository
import com.example.keyfairy.utils.network.RetrofitClient
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PracticeRepositoryImpl : PracticeRepository {

    private val TAG = "PracticeRepository"
    private val practiceApi = RetrofitClient.createService(PracticeApi::class.java)
    private val gson = Gson()

    override suspend fun registerPractice(practice: Practice, videoFile: File): Result<PracticeResult> {
        return try {
            Log.d(TAG, "🎹 Registering practice for UID: ${practice.uid}")

            // Preparar los datos de la práctica como JSON
            val practiceRequest = PracticeMapper.domainToRequest(practice)
            val practiceJson = gson.toJson(practiceRequest)
            val practiceDataBody = practiceJson.toRequestBody("text/plain".toMediaType())

            // Preparar el archivo de video
            val videoRequestBody = videoFile.asRequestBody("video/*".toMediaType())
            val videoPart = MultipartBody.Part.createFormData(
                "video",
                videoFile.name,
                videoRequestBody
            )

            // Hacer la petición
            val response = practiceApi.registerPractice(videoPart, practiceDataBody)

            if (response.isSuccessful && response.body()?.data != null) {
                val practiceResponse = response.body()!!.data!!
                val practiceResult = PracticeMapper.responseToResult(practiceResponse)

                Log.d(TAG, "✅ Practice registered successfully - ID: ${practiceResult.practiceId}")
                Result.success(practiceResult)
            } else {
                val errorMessage = getErrorMessage(response)
                Log.e(TAG, "❌ Practice registration failed: $errorMessage (${response.code()})")
                Result.failure(Exception(errorMessage))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error registering practice: ${e.message}", e)
            val errorMessage = getNetworkErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    private fun <T> getErrorMessage(response: retrofit2.Response<com.example.keyfairy.utils.network.StandardResponse<T>>): String {
        return when {
            response.body()?.message != null -> response.body()!!.message
            response.code() == 400 -> "Datos de práctica inválidos"
            response.code() == 401 -> "No autorizado - inicia sesión nuevamente"
            response.code() == 413 -> "El archivo de video es muy grande"
            response.code() == 415 -> "Formato de video no soportado"
            response.code() == 422 -> "Error en los datos de la práctica"
            response.code() in 500..599 -> "Error del servidor, intenta más tarde"
            else -> "Error desconocido (${response.code()})"
        }
    }

    private fun getNetworkErrorMessage(exception: Exception): String {
        return when (exception) {
            is java.net.UnknownHostException -> "Sin conexión a internet"
            is java.net.SocketTimeoutException -> "Tiempo de espera agotado, el video puede ser muy grande"
            is java.net.ConnectException -> "No se pudo conectar al servidor"
            is javax.net.ssl.SSLException -> "Error de conexión segura"
            else -> "Error de conexión: ${exception.message ?: "Desconocido"}"
        }
    }
}