package com.example.keyfairy.feature_auth.data.remote.api
import com.example.keyfairy.feature_auth.data.remote.dto.request.LoginRequest
import com.example.keyfairy.feature_auth.data.remote.dto.request.RefreshTokenRequest
import com.example.keyfairy.feature_auth.data.remote.dto.request.RegisterAuthRequest
import com.example.keyfairy.feature_auth.data.remote.dto.response.AuthResponse
import com.example.keyfairy.feature_auth.data.remote.dto.response.LoginResponse
import com.example.keyfairy.feature_auth.data.remote.dto.response.TokenResponse
import com.example.keyfairy.utils.network.StandardResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Auth API endpoints
 * All endpoints are public (don't require authentication)
 */
interface AuthApi {

    /**
     * Register new user credentials in Firebase
     * POST /auth/register
     */
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterAuthRequest
    ): Response<StandardResponse<AuthResponse>>

    /**
     * Login user and get tokens
     * POST /auth/login
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<StandardResponse<LoginResponse>>

    /**
     * Refresh access token
     * POST /auth/refresh-token
     */
    @POST("auth/refresh-token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<StandardResponse<TokenResponse>>
}