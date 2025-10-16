package com.example.keyfairy.feature_reports.domain.repository

import java.io.File

interface PdfRepository {

    suspend fun downloadReport(
        uid: String,
        practiceId: Int,
        destinationFile: File
    ): File
}