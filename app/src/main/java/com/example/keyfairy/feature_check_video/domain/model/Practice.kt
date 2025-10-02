package com.example.keyfairy.feature_check_video.domain.model

data class Practice(
    val practiceId: Int,
    val date: String,
    val time: String,
    val duration: Int,
    val uid: String,
    val videoLocalRoute: String,
    val scale: String,
    val scaleType: String,
    val reps: Int,
    val bpm: Int
)