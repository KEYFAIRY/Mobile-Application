package com.example.keyfairy.feature_progress.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class TiempoPosturaResponseList (
    @SerializedName("data")
    val data: List<TiempoPosturaResponse>
)