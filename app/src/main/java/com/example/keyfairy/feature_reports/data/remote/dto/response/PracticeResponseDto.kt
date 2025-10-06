package com.example.keyfairy.feature_reports.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class PracticeResponseDto(
    @SerializedName("num_practices")
    val numPractices: Int,

    @SerializedName("practices")
    val practices: List<PracticeItemDto>
)