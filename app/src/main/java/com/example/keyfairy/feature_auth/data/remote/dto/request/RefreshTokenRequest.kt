package com.example.keyfairy.feature_auth.data.remote.dto.request
import com.google.gson.annotations.SerializedName

/**
 * Request for /auth/refresh-token endpoint
 */
data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)