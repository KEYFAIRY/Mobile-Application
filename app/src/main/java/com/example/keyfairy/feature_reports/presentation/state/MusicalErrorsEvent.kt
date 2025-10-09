package com.example.keyfairy.feature_reports.presentation.state

sealed class MusicalErrorsEvent {
    data class ShowError(val message: String) : MusicalErrorsEvent()
    data class NavigateBack(val practiceId: Int) : MusicalErrorsEvent()
}