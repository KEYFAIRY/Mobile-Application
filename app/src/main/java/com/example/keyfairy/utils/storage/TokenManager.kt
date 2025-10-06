package com.example.keyfairy.utils.storage

import android.util.Log
import com.example.keyfairy.feature_auth.data.remote.api.AuthApi
import com.example.keyfairy.feature_auth.data.remote.dto.request.RefreshTokenRequest
import com.example.keyfairy.utils.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean


object TokenManager {

    private const val TAG = "TokenManager"
    private val isRefreshing = AtomicBoolean(false) // Evitar múltiples refresh simultáneos

    fun refreshToken(): String? = runBlocking {
        if (isRefreshing.get()) {
            Log.d(TAG, "Refresh already in progress, waiting...")
            while (isRefreshing.get()) {
                Thread.sleep(100)
            }
            return@runBlocking SecureStorage.getIdToken()
        }

        if (!isRefreshing.compareAndSet(false, true)) {
            return@runBlocking SecureStorage.getIdToken()
        }

        try {
            val refreshToken = SecureStorage.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                Log.e(TAG, "No refresh token available")
                // ✅ Limpiar sesión si no hay refresh token
                SecureStorage.clearAll()
                return@runBlocking null
            }

            Log.d(TAG, "🔄 Refreshing token...")

            val authApi = RetrofitClient.createServiceWithoutAuth(AuthApi::class.java)
            val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))

            if (response.isSuccessful && response.body()?.data != null) {
                val tokenData = response.body()!!.data!!

                // Guardar nuevos tokens
                SecureStorage.saveAuthData(
                    uid = SecureStorage.getUid() ?: "",
                    idToken = tokenData.idToken,
                    refreshToken = tokenData.refreshToken,
                    expiresIn = 3600
                )

                Log.d(TAG, "✅ Token refreshed successfully")
                return@runBlocking tokenData.idToken
            } else {
                Log.e(TAG, "❌ Failed to refresh token: ${response.code()}")
                SecureStorage.clearAll()
                return@runBlocking null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error refreshing token: ${e.message}", e)
            SecureStorage.clearAll()
            return@runBlocking null
        } finally {
            isRefreshing.set(false)
        }
    }

    fun getValidToken(): String? {
        return if (SecureStorage.isTokenExpired()) {
            Log.d(TAG, "Token expired, refreshing...")
            refreshToken()
        } else {
            SecureStorage.getIdToken()
        }
    }

    fun hasValidSession(): Boolean {
        val token = getValidToken()
        return !token.isNullOrEmpty() && SecureStorage.hasValidSession()
    }
}