package com.example.keyfairy.utils.network

import com.example.keyfairy.utils.storage.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds authentication token to requests
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Obtener token
        val token = SecureStorage.getIdToken()

        // Si no hay token, continuar sin modificar
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Agregar token al header
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}