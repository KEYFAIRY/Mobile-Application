package com.example.keyfairy.feature_auth.data.remote.dto.request
import com.google.gson.annotations.SerializedName

/**
 * Request for POST /users/ endpoint
 */
data class CreateUserRequest(
    @SerializedName("uid")
    val uid: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("piano_level")
    val pianoLevel: String
)