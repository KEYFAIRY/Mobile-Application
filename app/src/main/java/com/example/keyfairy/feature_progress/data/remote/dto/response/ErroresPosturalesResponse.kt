package com.example.keyfairy.feature_progress.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class ErroresPosturalesResponse(
    @SerializedName("escala")
    val escala: String,

    @SerializedName("total_errores_posturales")
    val totalErroresPosturales: Int,

    @SerializedName("dia")
    val dia: String

)