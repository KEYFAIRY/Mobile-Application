package com.example.keyfairy.feature_check_video.domain.model

data class Practice(
    val uid: String,
    var practiceId: Int,
    val date: String,
    val time: String,
    val scale: String,
    val scaleType: String,
    val duration: Int,
    val bpm: Int,
    val figure: Double,
    val octaves: Int,
    val total_notes_played: Int,
    val videoLocalRoute: String,
)