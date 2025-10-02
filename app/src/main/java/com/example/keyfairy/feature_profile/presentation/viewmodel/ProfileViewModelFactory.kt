package com.example.keyfairy.feature_profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.feature_auth.domain.usecase.GetProfileUseCase
import com.example.keyfairy.feature_auth.domain.usecase.UpdateProfileUseCase
import com.example.keyfairy.feature_auth.domain.usecase.LogoutUseCase

class ProfileViewModelFactory(
    private val getUserProfileUseCase: GetProfileUseCase,
    private val updateUserProfileUseCase: UpdateProfileUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(
                getUserProfileUseCase,
                updateUserProfileUseCase,
                logoutUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}