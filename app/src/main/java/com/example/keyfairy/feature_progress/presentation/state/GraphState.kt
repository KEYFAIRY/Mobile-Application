package com.example.keyfairy.feature_progress.presentation.state

import android.graphics.Bitmap

sealed class GraphState {
    object Idle : GraphState()
    object Loading : GraphState()
    data class Success(val bitmap: Bitmap) : GraphState()
    data class Error(val message: String) : GraphState()
}