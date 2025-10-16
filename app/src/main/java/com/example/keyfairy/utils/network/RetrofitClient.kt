package com.example.keyfairy.utils.network

import com.example.keyfairy.utils.common.Config
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client singleton
 * Provides configured Retrofit instances for API calls
 */
object RetrofitClient {

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (Config.isLoggingEnabled()) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    // OkHttpClient CON autenticación
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor())  // Agregar token a todas las peticiones
            .authenticator(AuthAuthenticator()) // Refresh automático en 401
            .connectTimeout(Config.getConnectTimeout(), TimeUnit.SECONDS)
            .readTimeout(Config.getReadTimeout(), TimeUnit.SECONDS)
            .writeTimeout(Config.getWriteTimeout(), TimeUnit.SECONDS)
            .build()
    }

    // OkHttpClient SIN autenticación (para login y refresh)
    private val okHttpClientWithoutAuth by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Config.getConnectTimeout(), TimeUnit.SECONDS)
            .readTimeout(Config.getReadTimeout(), TimeUnit.SECONDS)
            .writeTimeout(Config.getWriteTimeout(), TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Config.getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    private val retrofitWithoutAuth by lazy {
        Retrofit.Builder()
            .baseUrl(Config.getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClientWithoutAuth)
            .build()
    }

    /**
     * Create service with authentication
     */
    fun <T> createService(service: Class<T>): T {
        return retrofit.create(service)
    }

    /**
     * Create service WITHOUT authentication (for login/refresh endpoints)
     */
    fun <T> createServiceWithoutAuth(service: Class<T>): T {
        return retrofitWithoutAuth.create(service)
    }
}