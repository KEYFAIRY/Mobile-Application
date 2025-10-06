package com.example.keyfairy.utils.storage

import android.util.Log
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.keyfairy.feature_auth.data.remote.api.AuthApi
import com.example.keyfairy.feature_auth.data.remote.dto.request.RefreshTokenRequest
import com.example.keyfairy.utils.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

object TokenManager {

    private const val TAG = "TokenManager"
    private val isRefreshing = AtomicBoolean(false)
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun refreshToken(): String? = runBlocking {
        if (!hasInternetConnection()) {
            Log.d(TAG, "🌐 No internet connection, skipping token refresh")
            return@runBlocking SecureStorage.getIdToken()
        }

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
                return@runBlocking SecureStorage.getIdToken()
            }

            Log.d(TAG, "🔄 Refreshing token...")

            val authApi = RetrofitClient.createServiceWithoutAuth(AuthApi::class.java)
            val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))

            if (response.isSuccessful && response.body()?.data != null) {
                val tokenData = response.body()!!.data!!

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
                // Solo limpiar en casos específicos (401 Unauthorized con refresh token inválido)
                if (response.code() == 401) {
                    Log.e(TAG, "Refresh token invalid, clearing session")
                    SecureStorage.clearAll()
                    return@runBlocking null
                }
                // En otros casos, mantener sesión para uso offline
                return@runBlocking SecureStorage.getIdToken()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error refreshing token: ${e.message}", e)
            // Mantener sesión existente para uso offline
            return@runBlocking SecureStorage.getIdToken()
        } finally {
            isRefreshing.set(false)
        }
    }

    fun getValidToken(): String? {
        val currentToken = SecureStorage.getIdToken()
        val hasInternet = hasInternetConnection()

        // Si no hay token, no hay nada que retornar
        if (currentToken.isNullOrEmpty()) {
            return null
        }

        // Si el token no ha expirado, retornarlo directamente
        if (!SecureStorage.isTokenExpired()) {
            return currentToken
        }

        // Token expirado
        return if (hasInternet) {
            Log.d(TAG, "Token expired, attempting refresh...")
            refreshToken() ?: currentToken // Si falla el refresh, usar el actual
        } else {
            Log.d(TAG, "🌐 Token expired but offline - using current token")
            currentToken // Permitir uso offline con token expirado
        }
    }

    fun hasValidSession(): Boolean {
        val hasBasicSession = !SecureStorage.getUid().isNullOrEmpty() &&
                !SecureStorage.getIdToken().isNullOrEmpty()

        if (!hasBasicSession) {
            return false
        }

        return true
    }

    public fun hasInternetConnection(): Boolean {
        val context = appContext ?: return false

        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking internet connection: ${e.message}")
            false
        }
    }

    fun forceRefreshWhenOnline() {
        if (hasInternetConnection() && SecureStorage.isTokenExpired()) {
            Log.d(TAG, "🌐 Back online - forcing token refresh")
            refreshToken()
        }
    }
}