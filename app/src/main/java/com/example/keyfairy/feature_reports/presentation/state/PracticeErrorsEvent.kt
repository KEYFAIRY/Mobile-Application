package com.example.keyfairy.feature_reports.presentation.state

sealed class PracticeErrorsEvent {
    data class ShowError(val message: String) : PracticeErrorsEvent()
    data class NavigateBack(val practiceId: Int) : PracticeErrorsEvent()
}