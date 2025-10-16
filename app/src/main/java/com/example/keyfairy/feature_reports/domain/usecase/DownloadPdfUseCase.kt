package com.example.keyfairy.feature_reports.domain.usecase

import com.example.keyfairy.feature_reports.domain.repository.PdfRepository
import java.io.File

class DownloadPdfUseCase(
    private val repository: PdfRepository
) {

    suspend operator fun invoke(
        uid: String,
        practiceId: Int,
        destinationFile: File
    ): File {
        return repository.downloadReport(uid, practiceId, destinationFile)
    }
}