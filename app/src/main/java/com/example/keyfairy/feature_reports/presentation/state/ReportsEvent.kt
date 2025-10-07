package com.example.keyfairy.feature_reports.presentation.state

sealed class ReportsEvent {
    data class ShowError(val message: String) : ReportsEvent()
    data class NavigateToDetails(val practiceId: Int) : ReportsEvent()
}