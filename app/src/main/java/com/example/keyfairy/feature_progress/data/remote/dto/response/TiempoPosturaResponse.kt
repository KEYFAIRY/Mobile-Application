package com.example.keyfairy.feature_progress.data.remote.dto.response

import com.google.gson.annotations.SerializedName

class TiempoPosturaResponse (
    @SerializedName("escala")
    val escala: String,

    @SerializedName("tiempo_total_segundos")
    val tiempoTotalSegundos: Double,

    @SerializedName("tiempo_mala_postura_segundos")
    val tiempoMalaPosturaSegundos: Double,

    @SerializedName("tiempo_buena_postura_segundos")
    val tiempoBuenaPosturaSegundos: Double
)