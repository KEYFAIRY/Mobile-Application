package com.example.keyfairy.feature_check_video.domain.repository

import com.example.keyfairy.feature_check_video.domain.model.Practice
import com.example.keyfairy.feature_check_video.domain.model.PracticeResult
import java.io.File

interface PracticeRepository {
    suspend fun registerPractice(practice: Practice, videoFile: File): Result<PracticeResult>
}