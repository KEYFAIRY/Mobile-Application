package com.example.keyfairy.utils.storage

import android.util.Log
import com.example.keyfairy.feature_auth.data.remote.api.AuthApi
import com.example.keyfairy.feature_auth.data.remote.dto.request.RefreshTokenRequest
import com.example.keyfairy.utils.network.RetrofitClient
import kotlinx.coroutines.runBlocking


object TokenManager {

    private const val TAG = "TokenManager"

    fun refreshToken(): String? = runBlocking {
        try {
            val refreshToken = SecureStorage.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                Log.e(TAG, "No refresh token available")
                return@runBlocking null
            }

            Log.d(TAG, "🔄 Refreshing token...")

            val authApi = RetrofitClient.createServiceWithoutAuth(AuthApi::class.java)
            val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))

            if (response.isSuccessful && response.body()?.data != null) {
                val tokenData = response.body()!!.data!!

                // Guardar nuevos tokens
                SecureStorage.saveIdToken(tokenData.idToken)
                SecureStorage.saveRefreshToken(tokenData.refreshToken)
                SecureStorage.saveTokenExpiry(System.currentTimeMillis() + 3600000)

                Log.d(TAG, "✅ Token refreshed successfully")
                return@runBlocking tokenData.idToken
            } else {
                Log.e(TAG, "❌ Failed to refresh token: ${response.code()}")
                return@runBlocking null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error refreshing token: ${e.message}", e)
            return@runBlocking null
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
}