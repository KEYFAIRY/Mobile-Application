package com.example.keyfairy.feature_reports.presentation.state

sealed class PosturalErrorsUiEvent {
    data class ShowError(val message: String) : PosturalErrorsUiEvent()
    data class NavigateBack(val practiceId: Int) : PosturalErrorsUiEvent()
}