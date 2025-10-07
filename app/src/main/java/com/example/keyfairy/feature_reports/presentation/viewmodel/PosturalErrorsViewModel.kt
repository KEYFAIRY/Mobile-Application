package com.example.keyfairy.feature_reports.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyfairy.feature_reports.domain.usecase.GetPosturalErrorsUseCase
import com.example.keyfairy.feature_reports.presentation.state.PosturalErrorsState
import com.example.keyfairy.feature_reports.presentation.state.PosturalErrorsUiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PosturalErrorsViewModel(
    private val getPosturalErrorsUseCase: GetPosturalErrorsUseCase,
    private val practiceId: Int
) : ViewModel() {

    companion object {
        private const val TAG = "PosturalErrorsViewModel"
    }

    private val _uiState = MutableStateFlow<PosturalErrorsState>(PosturalErrorsState.Initial)
    val uiState: StateFlow<PosturalErrorsState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<PosturalErrorsUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadPosturalErrors()
    }

    fun loadPosturalErrors() {
        if (_uiState.value is PosturalErrorsState.Loading) return

        viewModelScope.launch {
            _uiState.value = PosturalErrorsState.Loading

            Log.d(TAG, "Loading postural errors for practice_id=$practiceId")

            val result = getPosturalErrorsUseCase.execute(practiceId)

            result.fold(
                onSuccess = { posturalErrorResponse ->
                    Log.d(TAG, "Successfully loaded ${posturalErrorResponse.numErrors} postural errors")

                    _uiState.value = PosturalErrorsState.Success(
                        numErrors = posturalErrorResponse.numErrors,
                        errors = posturalErrorResponse.errors
                    )
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error loading postural errors: ${exception.message}", exception)

                    val errorMessage = exception.message ?: "Error al cargar errores posturales"
                    _uiState.value = PosturalErrorsState.Error(errorMessage)
                    _uiEvent.send(PosturalErrorsUiEvent.ShowError(errorMessage))
                }
            )
        }
    }

    fun retry() {
        loadPosturalErrors()
    }
}