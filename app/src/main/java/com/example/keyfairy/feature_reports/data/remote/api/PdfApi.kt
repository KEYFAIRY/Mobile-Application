package com.example.keyfairy.feature_reports.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface PdfApi {

    @Streaming
    @GET("reports/{uid}/{practice_id}")
    suspend fun downloadReport(
        @Path("uid") uid: String,
        @Path("practice_id") practiceId: Int
    ): Response<ResponseBody>
}