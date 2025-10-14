package com.example.keyfairy.feature_progress.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class ErroresMusicalesResponse(
    @SerializedName("escala")
    val escala: String,

    @SerializedName("total_errores_musicales")
    val totalErroresMusicales: Int,

    @SerializedName("dia")
    val dia: String

)