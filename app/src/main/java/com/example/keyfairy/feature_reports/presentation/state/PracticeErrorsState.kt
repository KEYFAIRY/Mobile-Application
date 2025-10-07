package com.example.keyfairy.feature_reports.presentation.state

import com.example.keyfairy.feature_reports.domain.model.PosturalError


sealed class PracticeErrorsState {
    object Initial : PracticeErrorsState()
    object Loading : PracticeErrorsState()
    data class Success(val numErrors: Int, val errors: List<PosturalError>) : PracticeErrorsState()
    data class Error(val message: String) : PracticeErrorsState()
}