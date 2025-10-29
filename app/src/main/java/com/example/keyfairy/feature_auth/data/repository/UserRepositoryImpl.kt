package com.example.keyfairy.feature_auth.data.repository

import android.util.Log
import com.example.keyfairy.feature_auth.data.mapper.UserMapper
import com.example.keyfairy.feature_auth.data.remote.api.UserApi
import com.example.keyfairy.feature_auth.data.remote.dto.request.CreateUserRequest
import com.example.keyfairy.feature_auth.data.remote.dto.request.UpdateUserRequest
import com.example.keyfairy.feature_auth.domain.model.User
import com.example.keyfairy.feature_auth.domain.repository.UserRepository
import com.example.keyfairy.utils.enums.PianoLevel
import com.example.keyfairy.utils.network.RetrofitClient

/**
 * Implementation of UserRepository
 */
class UserRepositoryImpl: UserRepository {

    private val TAG = "UserRepository"

    // APIs SIN autenticación (para crear usuario)
    private val userApiWithoutAuth = RetrofitClient.createServiceWithoutAuth(UserApi::class.java)

    // APIs CON autenticación (para operaciones que requieren login)
    private val userApi = RetrofitClient.createService(UserApi::class.java)

    override suspend fun createUserProfile(
        uid: String,
        email: String,
        name: String,
        pianoLevel: PianoLevel
    ): Result<User> {
        return try {
            Log.d(TAG, "👤 Creating user profile for UID: $uid")

            // USAR API SIN AUTENTICACIÓN PARA CREAR USUARIO
            val response = userApiWithoutAuth.createUser(
                CreateUserRequest(
                    uid = uid,
                    email = email,
                    name = name,
                    pianoLevel = pianoLevel.label
                )
            )

            if (response.isSuccessful && response.body()?.data != null) {
                val userData = response.body()!!.data!!
                val user = UserMapper.userResponseToDomain(userData)

                Log.d(TAG, "✅ User profile created successfully")
                Result.success(user)
            } else {
                val errorMessage = getErrorMessage(response)
                Log.e(TAG, "❌ User creation failed: $errorMessage (${response.code()})")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating user profile: ${e.message}", e)
            val errorMessage = getNetworkErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun updateUserProfile(uid: String, pianoLevel: PianoLevel): Result<User> {
        return try {
            Log.d(TAG, "📝 Updating user profile for UID: $uid")

            // USAR API CON AUTENTICACIÓN PARA ACTUALIZAR
            val response = userApi.updateUser(
                uid = uid,
                request = UpdateUserRequest(pianoLevel = pianoLevel.label)
            )

            if (response.isSuccessful && response.body()?.data != null) {
                val userData = response.body()!!.data!!
                val user = UserMapper.userResponseToDomain(userData)

                Log.d(TAG, "✅ User profile updated successfully")
                Result.success(user)
            } else {
                val errorMessage = getErrorMessage(response)
                Log.e(TAG, "❌ User update failed: $errorMessage (${response.code()})")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating user profile: ${e.message}", e)
            val errorMessage = getNetworkErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            Log.d(TAG, "📖 Fetching user profile for UID: $uid")

            // USAR API CON AUTENTICACIÓN PARA OBTENER PERFIL
            val response = userApi.getUserById(uid)

            if (response.isSuccessful && response.body()?.data != null) {
                val userData = response.body()!!.data!!
                val user = UserMapper.userResponseToDomain(userData)

                Log.d(TAG, "✅ User profile fetched successfully")
                Result.success(user)
            } else {
                val errorMessage = getErrorMessage(response)
                Log.e(TAG, "❌ Failed to fetch user: $errorMessage (${response.code()})")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching user profile: ${e.message}", e)
            val errorMessage = getNetworkErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    // FUNCIONES AUXILIARES PARA MANEJO DE ERRORES
    private fun <T> getErrorMessage(response: retrofit2.Response<com.example.keyfairy.utils.network.StandardResponse<T>>): String {
        return when {
            // Si el StandardResponse tiene un mensaje específico, usarlo
            response.body()?.message != null -> response.body()!!.message
            // Mensajes por código HTTP específicos para operaciones de usuario
            response.code() == 400 -> "Datos del usuario inválidos"
            response.code() == 401 -> "No autorizado - inicia sesión nuevamente"
            response.code() == 403 -> "No tienes permisos para esta operación"
            response.code() == 404 -> "Usuario no encontrado"
            response.code() == 409 -> "Conflicto - el usuario ya existe"
            response.code() == 422 -> "Nivel de piano inválido. Usa: principiante, intermedio o avanzado"
            response.code() in 500..599 -> "Error del servidor, intenta más tarde"
            else -> "Error desconocido (${response.code()})"
        }
    }

    private fun getNetworkErrorMessage(exception: Exception): String {
        return when (exception) {
            is java.net.UnknownHostException -> "Sin conexión a internet"
            is java.net.SocketTimeoutException -> "Tiempo de espera agotado, intenta de nuevo"
            is java.net.ConnectException -> "No se pudo conectar al servidor"
            is javax.net.ssl.SSLException -> "Error de conexión segura"
            else -> "Error de conexión: ${exception.message ?: "Desconocido"}"
        }
    }
}