package com.example.keyfairy.feature_reports.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyfairy.feature_reports.domain.usecase.DownloadPdfUseCase
import com.example.keyfairy.feature_reports.domain.usecase.GetPosturalErrorsUseCase
import com.example.keyfairy.feature_reports.presentation.state.DownloadReportEvent
import com.example.keyfairy.feature_reports.presentation.state.DownloadReportState
import com.example.keyfairy.feature_reports.presentation.state.PracticeErrorsState
import com.example.keyfairy.feature_reports.presentation.state.PracticeErrorsEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File

class PracticeErrorsViewModel(
    private val getPosturalErrorsUseCase: GetPosturalErrorsUseCase,
    private val downloadReportUseCase: DownloadPdfUseCase,
    private val practiceId: Int
) : ViewModel() {

    companion object {
        private const val TAG = "PracticeErrorsViewModel"
    }

    // Postural errors state
    private val _uiState = MutableStateFlow<PracticeErrorsState>(PracticeErrorsState.Initial)
    val uiState: StateFlow<PracticeErrorsState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<PracticeErrorsEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    // Download report state
    private val _downloadState = MutableStateFlow<DownloadReportState>(DownloadReportState.Idle)
    val downloadState: StateFlow<DownloadReportState> = _downloadState.asStateFlow()

    private val _downloadEvent = Channel<DownloadReportEvent>()
    val downloadEvent = _downloadEvent.receiveAsFlow()

    init {
        loadPosturalErrors()
    }

    fun loadPosturalErrors() {
        if (_uiState.value is PracticeErrorsState.Loading) return

        viewModelScope.launch {
            _uiState.value = PracticeErrorsState.Loading

            Log.d(TAG, "Loading postural errors for practice_id=$practiceId")

            val result = getPosturalErrorsUseCase.execute(practiceId)

            result.fold(
                onSuccess = { posturalErrorResponse ->
                    Log.d(TAG, "Successfully loaded ${posturalErrorResponse.numErrors} postural errors")

                    _uiState.value = PracticeErrorsState.Success(
                        numErrors = posturalErrorResponse.numErrors,
                        errors = posturalErrorResponse.errors
                    )
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error loading postural errors: ${exception.message}", exception)

                    val errorMessage = exception.message ?: "Error al cargar errores posturales"
                    _uiState.value = PracticeErrorsState.Error(errorMessage)
                    _uiEvent.send(PracticeErrorsEvent.ShowError(errorMessage))
                }
            )
        }
    }

    fun downloadReport(uid: String, destinationFile: File) {
        viewModelScope.launch {
            try {
                _downloadState.value = DownloadReportState.Downloading
                Log.d(TAG, "🔽 Starting download for practice $practiceId...")

                val file = downloadReportUseCase(uid, practiceId, destinationFile)

                _downloadState.value = DownloadReportState.Success(file)
                _downloadEvent.send(DownloadReportEvent.OpenPdf(file))
                Log.d(TAG, "✅ Download successful: ${file.absolutePath}")

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Error desconocido al descargar el reporte"
                _downloadState.value = DownloadReportState.Error(errorMessage)
                _downloadEvent.send(DownloadReportEvent.ShowError(errorMessage))
                Log.e(TAG, "❌ Download failed: $errorMessage", e)
            }
        }
    }

    fun retry() {
        loadPosturalErrors()
    }
}