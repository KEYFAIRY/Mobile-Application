package com.example.keyfairy.feature_check_video.data.mapper

import com.example.keyfairy.feature_check_video.data.remote.dto.request.PracticeRequest
import com.example.keyfairy.feature_check_video.data.remote.dto.response.PracticeResponse
import com.example.keyfairy.feature_check_video.domain.model.Practice
import com.example.keyfairy.feature_check_video.domain.model.PracticeResult

object PracticeMapper {

    fun domainToRequest(practice: Practice): PracticeRequest {
        return PracticeRequest(
            date = practice.date,
            time = practice.time,
            duration = practice.duration,
            uid = practice.uid,
            video_local_route = practice.videoLocalRoute,
            scale = practice.scale,
            scale_type = practice.scaleType,
            reps = practice.reps,
            bpm = practice.bpm
        )
    }

    fun responseToResult(response: PracticeResponse): PracticeResult {
        return PracticeResult(
            practiceId = response.practice_id,
            date = response.date,
            time = response.time,
            duration = response.duration,
            scale = response.scale,
            scaleType = response.scale_type
        )
    }
}