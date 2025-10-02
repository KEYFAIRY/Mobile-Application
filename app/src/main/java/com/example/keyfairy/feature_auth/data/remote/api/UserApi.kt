package com.example.keyfairy.feature_auth.data.remote.api

import com.example.keyfairy.feature_auth.data.remote.dto.request.CreateUserRequest
import com.example.keyfairy.feature_auth.data.remote.dto.request.UpdateUserRequest
import com.example.keyfairy.feature_auth.data.remote.dto.response.UserResponse
import com.example.keyfairy.utils.network.StandardResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * User API endpoints
 * All endpoints require authentication (except createUser which uses UID from Firebase)
 */
interface UserApi {

    /**
     * Create a new user
     * POST /users/
     */
    @POST("users/")
    suspend fun createUser(
        @Body request: CreateUserRequest
    ): Response<StandardResponse<UserResponse>>

    /**
     * Update user information
     * PUT /users/{uid}
     */
    @PUT("users/{uid}")
    suspend fun updateUser(
        @Path("uid") uid: String,
        @Body request: UpdateUserRequest
    ): Response<StandardResponse<UserResponse>>

    /**
     * Get user by UID
     * GET /users/{uid}
     */
    @GET("users/{uid}")
    suspend fun getUserById(
        @Path("uid") uid: String
    ): Response<StandardResponse<UserResponse>>

}