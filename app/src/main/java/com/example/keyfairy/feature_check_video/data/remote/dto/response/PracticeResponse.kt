package com.example.keyfairy.feature_check_video.data.remote.dto.response

data class PracticeResponse(
    val practice_id: Int,
    val date: String,
    val time: String,
    val duration: Int,
    val scale: String,
    val scale_type: String
)