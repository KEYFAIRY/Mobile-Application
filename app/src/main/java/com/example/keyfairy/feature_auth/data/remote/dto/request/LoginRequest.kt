package com.example.keyfairy.feature_auth.data.remote.dto.request
import com.google.gson.annotations.SerializedName

/**
 * Request for /auth/login endpoint
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)