package com.example.keyfairy.feature_reports.data.remote.repository

import android.util.Log
import com.example.keyfairy.feature_reports.data.remote.api.PdfApi
import com.example.keyfairy.feature_reports.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRepositoryImp(
    private val pdfApi: PdfApi
) : PdfRepository {

    companion object {
        private const val TAG = "ReportsRepositoryImpl"
    }

    override suspend fun downloadReport(
        uid: String,
        practiceId: Int,
        destinationFile: File
    ): File = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔽 Downloading report for practice $practiceId...")

            val response = pdfApi.downloadReport(uid, practiceId)

            if (!response.isSuccessful) {
                throw Exception("Error downloading report: HTTP ${response.code()}")
            }

            val body = response.body()
                ?: throw Exception("Response body is empty")

            // Guardar el PDF en el archivo destino
            body.byteStream().use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Log.d(TAG, "✅ Report downloaded successfully to ${destinationFile.absolutePath}")
            destinationFile

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error downloading report: ${e.message}", e)
            throw e
        }
    }
}