package com.example.keyfairy.feature_check_video.data.remote.dto.request

data class PracticeRequest(
    val date: String,
    val time: String,
    val scale: String,
    val scale_type: String,
    val duration: Int,
    val bpm: Int,
    val figure: Double,
    val octaves: Int,
    val total_notes_played: Int,
    val uid: String,
    val video_local_route: String,
)