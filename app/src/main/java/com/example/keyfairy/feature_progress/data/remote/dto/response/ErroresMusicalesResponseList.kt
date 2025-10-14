package com.example.keyfairy.feature_progress.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class ErroresMusicalesResponseList (
    @SerializedName("data")
    val data: List<ErroresMusicalesResponse>
)