package com.example.keyfairy.feature_auth.data.remote.dto.response
import com.google.gson.annotations.SerializedName

/**
 * Response from /auth/login endpoint
 */
data class LoginResponse(
    @SerializedName("uid")
    val uid: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("id_token")
    val idToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String
)