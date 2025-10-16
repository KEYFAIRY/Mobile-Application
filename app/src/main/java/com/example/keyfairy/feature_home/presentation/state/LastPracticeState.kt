package com.example.keyfairy.feature_home.presentation.state

import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.feature_reports.presentation.state.ReportsState

sealed class LastPracticeState {
    object Initial : LastPracticeState()
    object Loading : LastPracticeState()
    data class Success(val practice: Practice) : LastPracticeState()
    data class Error(val message: String) : LastPracticeState()
    data class NoPractices(val message: String) : LastPracticeState()
}