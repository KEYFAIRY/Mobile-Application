package com.example.keyfairy.feature_auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.feature_auth.domain.usecase.CreateUserUseCase

class SignUpViewModelFactory(
    private val createUserUseCase: CreateUserUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
            return SignUpViewModel(createUserUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}