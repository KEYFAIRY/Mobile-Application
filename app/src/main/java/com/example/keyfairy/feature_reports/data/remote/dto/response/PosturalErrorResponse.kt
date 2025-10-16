package com.example.keyfairy.feature_reports.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class PosturalErrorResponse (
    @SerializedName("num_errors")
    val numErrors: Int,

    @SerializedName("errors")
    val errors: List<PosturalErrorItem>
)