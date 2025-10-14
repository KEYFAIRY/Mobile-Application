package com.example.keyfairy.feature_reports.presentation.state

import com.example.keyfairy.feature_reports.domain.model.Practice

sealed class ReportsEvent {
    data class ShowError(val message: String) : ReportsEvent()
    data class GettingDetails(val practiceId: Int) : ReportsEvent()
    data class NavigateToDetailsWithData(val practice: Practice) : ReportsEvent()
}