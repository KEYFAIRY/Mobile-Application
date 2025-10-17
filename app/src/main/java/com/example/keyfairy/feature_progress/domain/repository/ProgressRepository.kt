package com.example.keyfairy.feature_progress.domain.repository

import com.example.keyfairy.feature_progress.domain.model.*

interface ProgressRepository {
    suspend fun getTopEscalasSemanales(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<TopEscalasSemanales>>

    suspend fun getTiempoPosturasSemanales(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<TiempoPosturas>>

    suspend fun getNotasResumenSemanales(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<NotasResumen>>

    suspend fun getErroresPosturalesSemanales(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<ErroresPosturales>>

    suspend fun getErroresMusicalesSemanales(
        idStudent: String?,
        anio: Int,
        semana: Int
    ): Result<List<ErroresMusicales>>
}