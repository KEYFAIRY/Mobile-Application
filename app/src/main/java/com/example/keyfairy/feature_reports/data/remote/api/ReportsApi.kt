package com.example.keyfairy.feature_reports.data.remote.api
import com.example.keyfairy.feature_reports.data.remote.dto.response.PosturalErrorResponse
import com.example.keyfairy.feature_reports.data.remote.dto.response.PracticeItem
import com.example.keyfairy.feature_reports.data.remote.dto.response.PracticeResponse
import com.example.keyfairy.utils.network.StandardResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ReportsApi {

    @GET("practice/{uid}")
    suspend fun getUserPractices(
        @Path("uid") uid: String,
        @Query("last_id") lastId: Int? = null,
        @Query("limit") limit: Int? = 10
    ): Response<StandardResponse<PracticeResponse>>

    @GET("practice/{uid}/{practice_id}")
    suspend fun getPracticeById(
        @Path("uid") uid: String,
        @Path("practice_id") practiceId: Int
    ): Response<StandardResponse<PracticeItem>>
}