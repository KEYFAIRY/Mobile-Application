package com.example.keyfairy.feature_progress.data.remote.api


import com.example.keyfairy.feature_progress.data.remote.dto.response.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


interface ProgressApi {

    @GET("top-escalas-semanales")
    suspend fun getTopEscalasSemanales(
        @Query("idStudent") idStudent: String?,
        @Query("anio") anio: Int,
        @Query("semana") semana: Int
    ): Response<TopEscalasResponseList>

    @GET("tiempo-posturas-semanales")
    suspend fun getTiempoPosturasSemanales(
        @Query("idStudent") idStudent: String?,
        @Query("anio") anio: Int,
        @Query("semana") semana: Int
    ): Response<TiempoPosturaResponseList>

    @GET("notas-resumen-semanales")
    suspend fun getNotasResumenSemanales(
        @Query("idStudent") idStudent: String?,
        @Query("anio") anio: Int,
        @Query("semana") semana: Int
    ): Response<NotasResumenResponseList>

    @GET("errores-posturales-semanales")
    suspend fun getErroresPosturalesSemanales(
        @Query("idStudent") idStudent: String?,
        @Query("anio") anio: Int,
        @Query("semana") semana: Int
    ): Response<ErroresPosturalesResponseList>

    @GET("errores-musicales-semanales")
    suspend fun getErroresMusicalesSemanales(
        @Query("idStudent") idStudent: String?,
        @Query("anio") anio: Int,
        @Query("semana") semana: Int
    ): Response<ErroresMusicalesResponseList>

}