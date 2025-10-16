package com.example.keyfairy.feature_auth.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyfairy.feature_auth.domain.usecase.LoginUseCase
import com.example.keyfairy.feature_auth.presentation.state.LoginState
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            try {
                val result = loginUseCase.execute(email, password)

                _loginState.value = if (result.isSuccess) {
                    LoginState.Success(result.getOrNull()!!)
                } else {
                    LoginState.Error(
                        result.exceptionOrNull()?.message ?: "Login failed"
                    )
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}