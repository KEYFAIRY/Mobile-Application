package com.example.keyfairy.feature_reports.data.remote.api

import com.example.keyfairy.feature_reports.data.remote.dto.response.PosturalErrorResponse
import com.example.keyfairy.utils.network.StandardResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PosturalErrorApi {
    @GET("postural-errors/{practice_id}")
    suspend fun getPosturalErrors(
        @Path("practice_id") practiceId: Int
    ): Response<StandardResponse<PosturalErrorResponse>>
}