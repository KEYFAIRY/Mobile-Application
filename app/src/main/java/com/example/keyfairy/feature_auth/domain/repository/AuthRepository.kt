package com.example.keyfairy.feature_auth.domain.repository

import com.example.keyfairy.feature_auth.domain.model.AuthUser

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {

    /**
     * Register new user credentials in Firebase
     */
    suspend fun register(email: String, password: String): Result<String>

    /**
     * Login user and return auth data with tokens
     */
    suspend fun login(email: String, password: String): Result<AuthUser>

    /**
     * Refresh access token
     */
    suspend fun refreshToken(refreshToken: String): Result<Pair<String, String>>

    /**
     * Logout user
     */
    suspend fun logout(): Result<Boolean>

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean
}