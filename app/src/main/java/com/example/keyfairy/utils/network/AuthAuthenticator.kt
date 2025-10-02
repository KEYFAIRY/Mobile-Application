package com.example.keyfairy.utils.network

import android.util.Log
import com.example.keyfairy.utils.storage.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Authenticator that automatically refreshes token on 401 responses
 */
class AuthAuthenticator : Authenticator {

    private val TAG = "AuthAuthenticator"

    override fun authenticate(route: Route?, response: Response): Request? {
        // Si ya intentamos refrescar, no lo hagas de nuevo (evitar loop infinito)
        if (response.request.header("Authorization") != null &&
            response.priorResponse?.code == 401) {
            Log.e(TAG, "Token refresh already attempted, giving up")
            return null
        }

        Log.d(TAG, "🔄 Received 401, attempting token refresh...")

        // Intentar refrescar el token
        val newToken = TokenManager.refreshToken()

        return if (newToken != null) {
            Log.d(TAG, "✅ Token refreshed, retrying request")
            // Reintentar la petición con el nuevo token
            response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        } else {
            Log.e(TAG, "❌ Token refresh failed")
            // TODO: Redirigir a login
            null
        }
    }
}