package com.example.keyfairy.feature_progress.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class NotasResumenResponse(
    @SerializedName("escala")
    val escala: String,

    @SerializedName("notas_correctas")
    val notasCorrectas: Int,

    @SerializedName("notas_incorrectas")
    val notasIncorrectas: Int
)