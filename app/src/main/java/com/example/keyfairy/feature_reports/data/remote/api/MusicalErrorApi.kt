package com.example.keyfairy.feature_reports.data.remote.api

import com.example.keyfairy.feature_reports.data.remote.dto.response.MusicalErrorResponse
import com.example.keyfairy.utils.network.StandardResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface MusicalErrorApi {
    @GET("musical-errors/{practice_id}")
    suspend fun getMusicalErrors(
        @Path("practice_id") practiceId: Int
    ): Response<StandardResponse<MusicalErrorResponse>>
}