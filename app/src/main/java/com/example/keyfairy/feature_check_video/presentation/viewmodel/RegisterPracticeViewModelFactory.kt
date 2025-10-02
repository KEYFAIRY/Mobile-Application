package com.example.keyfairy.feature_check_video.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.feature_check_video.domain.use_case.RegisterPracticeUseCase

class RegisterPracticeViewModelFactory(
    private val registerPracticeUseCase: RegisterPracticeUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterPracticeViewModel::class.java)) {
            return RegisterPracticeViewModel(registerPracticeUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}