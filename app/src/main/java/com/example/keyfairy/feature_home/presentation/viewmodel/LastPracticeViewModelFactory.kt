package com.example.keyfairy.feature_home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.feature_home.domain.usecase.GetLastPracticeUseCase
import com.example.keyfairy.feature_reports.data.remote.api.ReportsApi
import com.example.keyfairy.feature_reports.data.repository.ReportsRepositoryImpl
import com.example.keyfairy.utils.network.RetrofitClient

class LastPracticeViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LastPracticeViewModel::class.java)) {
            // Get all practices
            val api = RetrofitClient.createService(ReportsApi::class.java)
            val repository = ReportsRepositoryImpl(api)
            val getLastPracticeUseCase = GetLastPracticeUseCase(repository)

            return LastPracticeViewModel(getLastPracticeUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}