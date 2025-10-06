package com.example.keyfairy.feature_reports.domain.model

data class PracticeItem(
    val practiceId: Int,
    val scale: String,
    val scaleType: String,
    val duration: Int,
    val bpm: Int,
    val figure: String,
    val octaves: Int,
    val date: String,
    val time: String,
    val state: String,
    val localVideoUrl: String?,
    val pdfUrl: String?
) {
    fun getFormattedDateTime(): String = "$date, $time"

    fun getPracticeInfo(): String = "$bpm BPM - $octaves Octavas - $figure"

    fun getScaleFullName(): String = "Escala $scale $scaleType"
}