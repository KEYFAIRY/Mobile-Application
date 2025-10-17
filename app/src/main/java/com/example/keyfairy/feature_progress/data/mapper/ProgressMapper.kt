package com.example.keyfairy.feature_progress.data.mapper

import com.example.keyfairy.feature_progress.data.remote.dto.response.*
import com.example.keyfairy.feature_progress.domain.model.*

object ProgressMapper {

    fun toDomain(response: TopEscalasResponse): TopEscalasSemanales {
        return TopEscalasSemanales(
            escala = response.escala,
            vecesPracticada = response.vecesPracticada
        )
    }

    fun toDomain(response: TiempoPosturaResponse): TiempoPosturas {
        return TiempoPosturas(
            escala = response.escala,
            tiempoTotalSegundos = response.tiempoTotalSegundos,
            tiempoMalaPosturaSegundos = response.tiempoMalaPosturaSegundos,
            tiempoBuenaPosturaSegundos = response.tiempoBuenaPosturaSegundos
        )
    }

    fun toDomain(response: NotasResumenResponse): NotasResumen {
        return NotasResumen(
            escala = response.escala,
            notasCorrectas = response.notasCorrectas,
            notasIncorrectas = response.notasIncorrectas
        )
    }

    fun toDomain(response: ErroresPosturalesResponse): ErroresPosturales {
        return ErroresPosturales(
            escala = response.escala,
            totalErroresPosturales = response.totalErroresPosturales,
            dia = response.dia
        )
    }

    fun toDomain(response: ErroresMusicalesResponse): ErroresMusicales {
        return ErroresMusicales(
            escala = response.escala,
            totalErroresMusicales = response.totalErroresMusicales,
            dia = response.dia
        )
    }
}
