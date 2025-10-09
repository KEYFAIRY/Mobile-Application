package com.example.keyfairy.feature_reports.presentation.state

import com.example.keyfairy.feature_reports.domain.model.MusicalError

sealed class MusicalErrorsState {
    object Initial : MusicalErrorsState()
    object Loading : MusicalErrorsState()
    data class Success(val numErrors: Int, val errors: List<MusicalError>) : MusicalErrorsState()
    data class Error(val message: String) : MusicalErrorsState()
}