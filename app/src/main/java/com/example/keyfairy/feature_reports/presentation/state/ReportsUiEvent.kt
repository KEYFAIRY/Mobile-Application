package com.example.keyfairy.feature_reports.presentation.state

sealed class ReportsUiEvent {
    data class ShowError(val message: String) : ReportsUiEvent()
    data class NavigateToDetails(val practiceId: Int) : ReportsUiEvent()
}