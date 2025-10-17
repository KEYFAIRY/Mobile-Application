package com.example.keyfairy.feature_progress.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class TopEscalasResponseList (
    @SerializedName("data")
    val data: List<TopEscalasResponse>
)