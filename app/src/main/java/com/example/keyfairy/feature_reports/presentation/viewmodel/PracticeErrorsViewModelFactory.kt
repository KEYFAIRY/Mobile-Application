package com.example.keyfairy.feature_reports.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.feature_reports.data.remote.api.PdfApi
import com.example.keyfairy.feature_reports.data.remote.api.PosturalErrorApi
import com.example.keyfairy.feature_reports.data.remote.repository.PdfRepositoryImp
import com.example.keyfairy.feature_reports.data.repository.PosturalErrorRepositoryImp
import com.example.keyfairy.feature_reports.domain.usecase.DownloadPdfUseCase
import com.example.keyfairy.feature_reports.domain.usecase.GetPosturalErrorsUseCase
import com.example.keyfairy.utils.network.RetrofitClient

class PracticeErrorsViewModelFactory(
    private val practiceId: Int
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeErrorsViewModel::class.java)) {
            // Postural errors dependencies
            val posturalErrorApi = RetrofitClient.createService(PosturalErrorApi::class.java)
            val posturalErrorRepository = PosturalErrorRepositoryImp(posturalErrorApi)
            val getPosturalErrorsUseCase = GetPosturalErrorsUseCase(posturalErrorRepository)

            // Download report dependencies
            val reportsApi = RetrofitClient.createService(PdfApi::class.java)
            val reportsRepository = PdfRepositoryImp(reportsApi)
            val downloadReportUseCase = DownloadPdfUseCase(reportsRepository)

            return PracticeErrorsViewModel(
                getPosturalErrorsUseCase,
                downloadReportUseCase,
                practiceId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}