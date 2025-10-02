package com.example.keyfairy.feature_check_video.data.remote.dto.request

data class PracticeRequest(
    val date: String,
    val time: String,
    val duration: Int,
    val uid: String,
    val video_local_route: String,
    val scale: String,
    val scale_type: String,
    val reps: Int,
    val bpm: Int
)