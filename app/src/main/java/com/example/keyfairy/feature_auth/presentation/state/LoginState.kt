package com.example.keyfairy.feature_auth.presentation.state
import com.example.keyfairy.feature_auth.domain.model.AuthUser

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: AuthUser) : LoginState()
    data class Error(val message: String) : LoginState()
}