package com.example.keyfairy.feature_reports.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyfairy.feature_reports.domain.usecase.DownloadPdfUseCase
import com.example.keyfairy.feature_reports.domain.usecase.GetMusicalErrorsUseCase
import com.example.keyfairy.feature_reports.domain.usecase.GetPosturalErrorsUseCase
import com.example.keyfairy.feature_reports.presentation.state.DownloadReportEvent
import com.example.keyfairy.feature_reports.presentation.state.DownloadReportState
import com.example.keyfairy.feature_reports.presentation.state.MusicalErrorsEvent
import com.example.keyfairy.feature_reports.presentation.state.MusicalErrorsState
import com.example.keyfairy.feature_reports.presentation.state.PosturalErrorsState
import com.example.keyfairy.feature_reports.presentation.state.PosturalErrorsEvent
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
    private val getMusicalErrorsUseCase: GetMusicalErrorsUseCase,
    private val practiceId: Int
) : ViewModel() {

    companion object {
        private const val TAG = "PracticeErrorsViewModel"
    }

    // Postural errors state
    private val _posturalErrorsState = MutableStateFlow<PosturalErrorsState>(PosturalErrorsState.Initial)
    val posturalErrorsState: StateFlow<PosturalErrorsState> = _posturalErrorsState.asStateFlow()

    private val _posturalErrorsEvent = Channel<PosturalErrorsEvent>()
    val posturalErrorsEvent = _posturalErrorsEvent.receiveAsFlow()

    // Musical errors state
    private val _musicalErrorsState = MutableStateFlow<MusicalErrorsState>(MusicalErrorsState.Initial)
    val musicalErrorsState: StateFlow<MusicalErrorsState> = _musicalErrorsState.asStateFlow()

    private val _musicalErrorsEvent = Channel<MusicalErrorsEvent>()
    val musicalErrorsEvent = _musicalErrorsEvent.receiveAsFlow()

    // Download report state
    private val _downloadState = MutableStateFlow<DownloadReportState>(DownloadReportState.Idle)
    val downloadState: StateFlow<DownloadReportState> = _downloadState.asStateFlow()

    private val _downloadEvent = Channel<DownloadReportEvent>()
    val downloadEvent = _downloadEvent.receiveAsFlow()

    init {
        loadPosturalErrors()
        loadMusicalErrors()
    }

    fun loadPosturalErrors() {
        if (_posturalErrorsState.value is PosturalErrorsState.Loading) return

        viewModelScope.launch {
            _posturalErrorsState.value = PosturalErrorsState.Loading

            Log.d(TAG, "Loading postural errors for practice_id=$practiceId")

            val result = getPosturalErrorsUseCase.execute(practiceId)

            result.fold(
                onSuccess = { posturalErrorResponse ->
                    Log.d(TAG, "Successfully loaded ${posturalErrorResponse.numErrors} postural errors")

                    _posturalErrorsState.value = PosturalErrorsState.Success(
                        numErrors = posturalErrorResponse.numErrors,
                        errors = posturalErrorResponse.errors
                    )
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error loading postural errors: ${exception.message}", exception)

                    val errorMessage = exception.message ?: "Error al cargar errores posturales"
                    _posturalErrorsState.value = PosturalErrorsState.Error(errorMessage)
                    _posturalErrorsEvent.send(PosturalErrorsEvent.ShowError(errorMessage))
                }
            )
        }
    }

    fun loadMusicalErrors() {
        if (_musicalErrorsState.value is MusicalErrorsState.Loading) return

        viewModelScope.launch {
            _musicalErrorsState.value = MusicalErrorsState.Loading

            Log.d(TAG, "Loading musical errors for practice_id=$practiceId")

            val result = getMusicalErrorsUseCase.execute(practiceId)

            result.fold(
                onSuccess = { musicalErrorResponse ->
                    Log.d(TAG, "Successfully loaded ${musicalErrorResponse.numErrors} postural errors")

                    _musicalErrorsState.value = MusicalErrorsState.Success(
                        numErrors = musicalErrorResponse.numErrors,
                        errors = musicalErrorResponse.errors
                    )
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error loading musical errors: ${exception.message}", exception)

                    val errorMessage = exception.message ?: "Error al cargar errores posturales"
                    _musicalErrorsState.value = MusicalErrorsState.Error(errorMessage)
                    _musicalErrorsEvent.send(MusicalErrorsEvent.ShowError(errorMessage))
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