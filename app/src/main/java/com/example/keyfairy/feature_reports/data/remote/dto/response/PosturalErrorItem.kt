package com.example.keyfairy.feature_reports.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data  class PosturalErrorItem (
    @SerializedName("min_sec_init")
    val minSecInit: String,

    @SerializedName("min_sec_end")
    val minSecEnd: String,

    @SerializedName("explication")
    val explication: String
)