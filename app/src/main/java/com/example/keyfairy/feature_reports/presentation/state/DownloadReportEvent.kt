package com.example.keyfairy.feature_reports.presentation.state

import java.io.File

sealed class DownloadReportEvent {
    data class ShowError(val message: String) : DownloadReportEvent()
    data class OpenPdf(val file: File) : DownloadReportEvent()
}