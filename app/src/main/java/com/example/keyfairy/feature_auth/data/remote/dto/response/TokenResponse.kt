package com.example.keyfairy.feature_auth.data.remote.dto.response
import com.google.gson.annotations.SerializedName

/**
 * Response from /auth/refresh-token endpoint
 */
data class TokenResponse(
    @SerializedName("id_token")
    val idToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String
)