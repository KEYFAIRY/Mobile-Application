package com.example.keyfairy.feature_progress.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class TopEscalasResponse (
    @SerializedName("escala")
    val escala: String,

    @SerializedName("veces_practicada")
    val vecesPracticada: Int
)
