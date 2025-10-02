package com.example.keyfairy.feature_auth.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyfairy.feature_auth.domain.usecase.CreateUserUseCase
import com.example.keyfairy.feature_auth.presentation.state.SignUpState
import com.example.keyfairy.utils.enums.PianoLevel
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val createUserUseCase: CreateUserUseCase
) : ViewModel() {

    private val _signUpState = MutableLiveData<SignUpState>(SignUpState.Idle)
    val signUpState: LiveData<SignUpState> = _signUpState

    fun signUp(
        name: String,
        email: String,
        password: String,
        pianoLevel: PianoLevel
    ) {
        viewModelScope.launch {
            _signUpState.value = SignUpState.Loading

            try {
                val result = createUserUseCase.execute(email, password, name, pianoLevel)

                _signUpState.value = if (result.isSuccess) {
                    SignUpState.Success(result.getOrNull()!!)
                } else {
                    SignUpState.Error(
                        result.exceptionOrNull()?.message ?: "Error al crear cuenta"
                    )
                }
            } catch (e: Exception) {
                _signUpState.value = SignUpState.Error(e.message ?: "Error inesperado")
            }
        }
    }

    fun resetState() {
        _signUpState.value = SignUpState.Idle
    }
}