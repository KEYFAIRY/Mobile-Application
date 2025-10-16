package com.example.keyfairy.feature_reports.presentation.state

import com.example.keyfairy.feature_reports.domain.model.PosturalError


sealed class PosturalErrorsState {
    object Initial : PosturalErrorsState()
    object Loading : PosturalErrorsState()
    data class Success(val numErrors: Int, val errors: List<PosturalError>) : PosturalErrorsState()
    data class Error(val message: String) : PosturalErrorsState()
}