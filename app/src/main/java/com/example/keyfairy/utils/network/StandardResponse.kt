package com.example.keyfairy.utils.network
import com.google.gson.annotations.SerializedName

/**
 * Standard response wrapper from API
 * Matches backend StandardResponse schema
 */
data class StandardResponse<T>(
    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: T?
)