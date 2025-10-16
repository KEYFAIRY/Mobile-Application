package com.example.keyfairy.feature_auth.data.remote.dto.response
import com.google.gson.annotations.SerializedName

/**
 * Response for user data from /users endpoints
 */
data class UserResponse(
    @SerializedName("uid")
    val uid: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("piano_level")
    val pianoLevel: String
)