package com.example.keyfairy.feature_reports.presentation.state

sealed class PosturalErrorsEvent {
    data class ShowError(val message: String) : PosturalErrorsEvent()
    data class NavigateBack(val practiceId: Int) : PosturalErrorsEvent()
}