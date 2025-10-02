package com.example.keyfairy.feature_auth.data.remote.dto.request
import com.google.gson.annotations.SerializedName

/**
 * Request for PUT /users/{uid} endpoint
 */
data class UpdateUserRequest(
    @SerializedName("piano_level")
    val pianoLevel: String?
)