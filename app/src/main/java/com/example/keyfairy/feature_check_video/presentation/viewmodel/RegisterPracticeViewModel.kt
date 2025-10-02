package com.example.keyfairy.feature_check_video.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyfairy.feature_check_video.domain.model.Practice
import com.example.keyfairy.feature_check_video.domain.use_case.RegisterPracticeUseCase
import com.example.keyfairy.feature_check_video.presentation.state.RegisterPracticeState
import kotlinx.coroutines.launch
import java.io.File

class RegisterPracticeViewModel(
    private val registerPracticeUseCase: RegisterPracticeUseCase
) : ViewModel() {

    private val _registerPracticeState = MutableLiveData<RegisterPracticeState>(RegisterPracticeState.Idle)
    val registerPracticeState: LiveData<RegisterPracticeState> = _registerPracticeState

    fun registerPractice(practice: Practice, videoFile: File) {
        viewModelScope.launch {
            _registerPracticeState.value = RegisterPracticeState.Loading

            try {
                val result = registerPracticeUseCase.execute(practice, videoFile)

                _registerPracticeState.value = if (result.isSuccess) {
                    RegisterPracticeState.Success(result.getOrNull()!!)
                } else {
                    RegisterPracticeState.Error(
                        result.exceptionOrNull()?.message ?: "Error al registrar práctica"
                    )
                }
            } catch (e: Exception) {
                _registerPracticeState.value = RegisterPracticeState.Error(
                    e.message ?: "Error inesperado"
                )
            }
        }
    }

    fun resetState() {
        _registerPracticeState.value = RegisterPracticeState.Idle
    }
}