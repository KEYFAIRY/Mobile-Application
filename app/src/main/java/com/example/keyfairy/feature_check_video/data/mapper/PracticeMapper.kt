package com.example.keyfairy.feature_check_video.data.mapper

import com.example.keyfairy.feature_check_video.data.remote.dto.request.PracticeRequest
import com.example.keyfairy.feature_check_video.data.remote.dto.response.PracticeResponse
import com.example.keyfairy.feature_check_video.domain.model.Practice

object PracticeMapper {

    fun domainToRequest(practice: Practice): PracticeRequest {
        return PracticeRequest(
            date = practice.date,
            time = practice.time,
            scale = practice.scale,
            scale_type = practice.scaleType,
            duration = practice.duration,
            bpm = practice.bpm,
            figure = practice.figure,
            octaves = practice.octaves,
            uid = practice.uid,
            video_local_route = practice.videoLocalRoute,
        )
    }
}