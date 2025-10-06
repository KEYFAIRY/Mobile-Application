package com.example.keyfairy.feature_reports.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.feature_reports.data.remote.api.ReportsApi
import com.example.keyfairy.feature_reports.data.repository.ReportsRepositoryImpl
import com.example.keyfairy.feature_reports.domain.usecase.GetUserPracticesUseCase
import com.example.keyfairy.utils.network.RetrofitClient

class ReportsViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            val api = RetrofitClient.createService(ReportsApi::class.java)
            val repository = ReportsRepositoryImpl(api)
            val useCase = GetUserPracticesUseCase(repository)

            return ReportsViewModel(useCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}