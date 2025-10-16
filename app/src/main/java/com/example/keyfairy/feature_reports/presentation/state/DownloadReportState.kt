package com.example.keyfairy.feature_reports.presentation.state

import java.io.File

sealed class DownloadReportState {
    object Idle: DownloadReportState()
    object Downloading: DownloadReportState()
    data class Success(val file: File) : DownloadReportState()
    data class Error(val message: String) : DownloadReportState()
}