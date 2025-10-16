package com.example.keyfairy.feature_reports.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class MusicalErrorItem (
    @SerializedName("min_sec")
    val min_sec: String,

    @SerializedName("note_played")
    val note_played: String,

    @SerializedName("note_correct")
    val note_correct: String
)