package com.example.keyfairy.feature_reports.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.feature_reports.data.remote.api.ReportsApi
import com.example.keyfairy.feature_reports.data.repository.ReportsRepositoryImpl
import com.example.keyfairy.feature_reports.domain.usecase.GetPracticeByIdUseCase
import com.example.keyfairy.feature_reports.domain.usecase.GetUserPracticesUseCase
import com.example.keyfairy.utils.network.RetrofitClient

class ReportsViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            // Get all practices
            val api = RetrofitClient.createService(ReportsApi::class.java)
            val repository = ReportsRepositoryImpl(api)
            val getAllUseCase = GetUserPracticesUseCase(repository)
            val getByIdUseCase = GetPracticeByIdUseCase(repository)

            return ReportsViewModel(getAllUseCase, getByIdUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}