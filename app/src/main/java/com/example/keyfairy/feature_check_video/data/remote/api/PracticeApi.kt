package com.example.keyfairy.feature_check_video.data.remote.api

import com.example.keyfairy.feature_check_video.data.remote.dto.response.PracticeResponse
import com.example.keyfairy.utils.network.StandardResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Practice API endpoints
 */
interface PracticeApi {

    /**
     * Register new practice
     * POST /practice/register
     */
    @Multipart
    @POST("practice/register")
    suspend fun registerPractice(
        @Part video: MultipartBody.Part,
        @Part("practice_data") practiceData: RequestBody
    ): Response<StandardResponse<PracticeResponse>>
}