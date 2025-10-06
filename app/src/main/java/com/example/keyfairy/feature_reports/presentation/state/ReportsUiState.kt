package com.example.keyfairy.feature_reports.presentation.state

import com.example.keyfairy.feature_reports.domain.model.PracticeItem

sealed class ReportsState {
    object Initial : ReportsState()
    object Loading : ReportsState()
    object LoadingMore : ReportsState()
    data class Success(val practices: List<PracticeItem>, val hasMore: Boolean) : ReportsState()
    data class Error(val message: String) : ReportsState()
}