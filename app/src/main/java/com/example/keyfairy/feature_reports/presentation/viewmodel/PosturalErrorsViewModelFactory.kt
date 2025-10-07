package com.example.keyfairy.feature_reports.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.feature_reports.data.remote.api.PosturalErrorApi
import com.example.keyfairy.feature_reports.data.repository.PosturalErrorRepositoryImp
import com.example.keyfairy.feature_reports.domain.usecase.GetPosturalErrorsUseCase
import com.example.keyfairy.utils.network.RetrofitClient

class PosturalErrorsViewModelFactory(
    private val practiceId: Int
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PosturalErrorsViewModel::class.java)) {
            val api = RetrofitClient.createService(PosturalErrorApi::class.java)
            val repository = PosturalErrorRepositoryImp(api)
            val useCase = GetPosturalErrorsUseCase(repository)

            return PosturalErrorsViewModel(useCase, practiceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}