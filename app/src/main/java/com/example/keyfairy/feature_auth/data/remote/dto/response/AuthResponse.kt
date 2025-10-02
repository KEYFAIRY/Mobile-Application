package com.example.keyfairy.feature_auth.data.remote.dto.response
import com.google.gson.annotations.SerializedName

/**
 * Response from /auth/register endpoint
 */
data class AuthResponse(
    @SerializedName("uid")
    val uid: String,

    @SerializedName("email")
    val email: String
)