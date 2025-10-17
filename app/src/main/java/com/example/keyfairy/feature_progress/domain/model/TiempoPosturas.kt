package com.example.keyfairy.feature_progress.domain.model

data class TiempoPosturas (
    val escala: String,
    val tiempoTotalSegundos: Double,
    val tiempoMalaPosturaSegundos: Double,
    val tiempoBuenaPosturaSegundos: Double
)