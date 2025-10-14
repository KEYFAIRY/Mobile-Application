package com.example.keyfairy.feature_home.presentation.state

sealed class LastPracticeEvent {
    data class ShowError(val message: String) : LastPracticeEvent()
    data class NoPractices(val message: String): LastPracticeEvent()
}