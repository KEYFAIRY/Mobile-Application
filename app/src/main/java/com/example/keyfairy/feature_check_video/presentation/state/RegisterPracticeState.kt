package com.example.keyfairy.feature_check_video.presentation.state

import com.example.keyfairy.feature_check_video.domain.model.Practice


sealed class RegisterPracticeState {
    object Idle : RegisterPracticeState()
    object Loading : RegisterPracticeState()
    data class Success(val practiceResult: Practice) : RegisterPracticeState()
    data class Error(val message: String) : RegisterPracticeState()
}