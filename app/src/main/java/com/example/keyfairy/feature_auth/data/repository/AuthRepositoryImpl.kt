package com.example.keyfairy.feature_auth.data.repository

import android.util.Log
import com.example.keyfairy.feature_auth.data.mapper.AuthMapper
import com.example.keyfairy.feature_auth.data.remote.api.AuthApi
import com.example.keyfairy.feature_auth.data.remote.dto.request.*
import com.example.keyfairy.feature_auth.domain.model.AuthUser
import com.example.keyfairy.feature_auth.domain.repository.AuthRepository
import com.example.keyfairy.utils.network.RetrofitClient
import com.example.keyfairy.utils.storage.SecureStorage

/**
 * Implementation of AuthRepository
 */
class AuthRepositoryImpl : AuthRepository {

    private val TAG = "AuthRepository"

    // APIs sin autenticación (para login, register, refresh)
    private val authApi = RetrofitClient.createServiceWithoutAuth(AuthApi::class.java)

    override suspend fun register(email: String, password: String): Result<String> {
        return try {
            Log.d(TAG, "🔐 Registering user: $email")

            val response = authApi.register(
                RegisterAuthRequest(email = email, password = password)
            )

            if (response.isSuccessful && response.body()?.data != null) {
                val uid = response.body()!!.data!!.uid
                Log.d(TAG, "✅ User registered successfully - UID: $uid")
                Result.success(uid)
            } else {
                val errorMessage = getErrorMessage(response)
                Log.e(TAG, "❌ Registration failed: $errorMessage (${response.code()})")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error registering user: ${e.message}", e)
            val errorMessage = getNetworkErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        return try {
            Log.d(TAG, "🔐 Logging in user: $email")

            val response = authApi.login(
                LoginRequest(email = email, password = password)
            )

            if (response.isSuccessful && response.body()?.data != null) {
                val loginData = response.body()!!.data!!

                // Guardar tokens de forma segura
                SecureStorage.saveAuthData(
                    uid = loginData.uid,
                    idToken = loginData.idToken,
                    refreshToken = loginData.refreshToken,
                    expiresIn = 3600 // 1 hora por defecto
                )

                val authUser = AuthMapper.loginResponseToDomain(loginData)

                Log.d(TAG, "✅ Login successful - UID: ${authUser.uid}")
                Result.success(authUser)
            } else {
                val errorMessage = getErrorMessage(response)
                Log.e(TAG, "❌ Login failed: $errorMessage (${response.code()})")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error logging in: ${e.message}", e)
            val errorMessage = getNetworkErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun refreshToken(refreshToken: String): Result<Pair<String, String>> {
        return try {
            Log.d(TAG, "🔄 Refreshing token")

            val response = authApi.refreshToken(
                RefreshTokenRequest(refreshToken = refreshToken)
            )

            if (response.isSuccessful && response.body()?.data != null) {
                val tokenData = response.body()!!.data!!

                // Actualizar tokens en storage
                SecureStorage.saveIdToken(tokenData.idToken)
                SecureStorage.saveRefreshToken(tokenData.refreshToken)
                SecureStorage.saveTokenExpiry(System.currentTimeMillis() + 3600000) // +1 hora

                Log.d(TAG, "✅ Token refreshed successfully")
                Result.success(Pair(tokenData.idToken, tokenData.refreshToken))
            } else {
                val errorMessage = getErrorMessage(response)
                Log.e(TAG, "❌ Token refresh failed: $errorMessage (${response.code()})")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error refreshing token: ${e.message}", e)
            val errorMessage = getNetworkErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun logout(): Result<Boolean> {
        return try {
            Log.d(TAG, "🚪 Logging out user")

            // Limpiar tokens locales
            SecureStorage.clearAll()

            Log.d(TAG, "✅ Logout successful")
            Result.success(true)
        } catch (e: Exception) {
            // Aunque falle, limpiar localmente
            SecureStorage.clearAll()
            Log.w(TAG, "⚠️ Logout with errors but cleared local data", e)
            Result.success(true)
        }
    }

    override fun isLoggedIn(): Boolean {
        return SecureStorage.hasValidSession()
    }

    // FUNCIONES AUXILIARES PARA MANEJO DE ERRORES
    private fun <T> getErrorMessage(response: retrofit2.Response<com.example.keyfairy.utils.network.StandardResponse<T>>): String {
        return when {
            // Si el StandardResponse tiene un mensaje específico, usarlo
            response.body()?.message != null -> response.body()!!.message
            // Mensajes por código HTTP
            response.code() == 400 -> "Datos inválidos"
            response.code() == 401 -> "Credenciales inválidas"
            response.code() == 403 -> "Acceso denegado"
            response.code() == 404 -> "Usuario no encontrado"
            response.code() == 409 -> "El usuario ya existe"
            response.code() == 422 -> "Error de validación en los datos"
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