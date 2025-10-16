package com.example.keyfairy.feature_auth.presentation.state

import com.example.keyfairy.feature_auth.domain.model.User

sealed class SignUpState {
    object Idle : SignUpState()
    object Loading : SignUpState()
    data class Success(val user: User) : SignUpState()
    data class Error(val message: String) : SignUpState()
}