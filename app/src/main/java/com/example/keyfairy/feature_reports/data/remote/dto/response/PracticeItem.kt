package com.example.keyfairy.feature_reports.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class PracticeItem(
    @SerializedName("practice_id")
    val practiceId: Int,

    @SerializedName("scale")
    val scale: String,

    @SerializedName("scale_type")
    val scaleType: String,

    @SerializedName("duration")
    val duration: Int,

    @SerializedName("bpm")
    val bpm: Int,

    @SerializedName("figure")
    val figure: String,

    @SerializedName("octaves")
    val octaves: Int,

    @SerializedName("date")
    val date: String,

    @SerializedName("time")
    val time: String,

    @SerializedName("state")
    val state: String,

    @SerializedName("local_video_url")
    val localVideoUrl: String?,

    @SerializedName("pdf_url")
    val pdfUrl: String?
)