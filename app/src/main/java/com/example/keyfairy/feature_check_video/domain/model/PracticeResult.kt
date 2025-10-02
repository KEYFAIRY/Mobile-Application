package com.example.keyfairy.feature_check_video.domain.model

data class PracticeResult(
    val practiceId: Int,
    val date: String,
    val time: String,
    val duration: Int,
    val scale: String,
    val scaleType: String
)