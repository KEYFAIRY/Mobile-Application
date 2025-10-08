package com.example.keyfairy.feature_reports.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.feature_reports.domain.usecase.GetPracticeByIdUseCase
import com.example.keyfairy.feature_reports.domain.usecase.GetUserPracticesUseCase
import com.example.keyfairy.feature_reports.presentation.state.ReportsState
import com.example.keyfairy.feature_reports.presentation.state.ReportsEvent
import com.example.keyfairy.utils.storage.SecureStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ReportsViewModel(
    private val getUserPracticesUseCase: GetUserPracticesUseCase,
    private val getPracticeByIdUseCase: GetPracticeByIdUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "ReportsViewModel"
        private const val PAGE_SIZE = 10
    }

    private val _uiState = MutableStateFlow<ReportsState>(ReportsState.Initial)
    val uiState: StateFlow<ReportsState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<ReportsEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val practicesList = mutableListOf<Practice>()
    private var lastPracticeId: Int? = null
    private var isLoadingMore = false

    fun loadInitialPractices() {
        if (_uiState.value is ReportsState.Loading) return

        viewModelScope.launch {
            _uiState.value = ReportsState.Loading
            practicesList.clear()
            lastPracticeId = null

            loadPractices()
        }
    }

    fun loadMorePractices() {
        if (isLoadingMore) return

        viewModelScope.launch {
            isLoadingMore = true
            _uiState.value = ReportsState.LoadingMore

            loadPractices()

            isLoadingMore = false
        }
    }

    private suspend fun loadPractices() {
        val uid = SecureStorage.getUid()

        if (uid.isNullOrEmpty()) {
            Log.e(TAG, "No UID found in SecureStorage")
            _uiState.value = ReportsState.Error("Usuario no autenticado")
            _uiEvent.send(ReportsEvent.ShowError("Usuario no autenticado"))
            return
        }

        Log.d(TAG, "Loading practices for uid=$uid, lastId=$lastPracticeId, limit=$PAGE_SIZE")

        val result = getUserPracticesUseCase.execute(
            uid = uid,
            lastId = lastPracticeId,
            limit = PAGE_SIZE
        )

        result.fold(
            onSuccess = { practiceList ->
                val newPractices = practiceList.practices

                if (newPractices.isNotEmpty()) {
                    practicesList.addAll(newPractices)
                    lastPracticeId = newPractices.last().practiceId
                }

                val hasMore = newPractices.size == PAGE_SIZE

                Log.d(TAG, "Loaded ${newPractices.size} practices. Total: ${practicesList.size}, HasMore: $hasMore")

                _uiState.value = ReportsState.Success(
                    practices = practicesList.toList(),
                    hasMore = hasMore
                )
            },
            onFailure = { exception ->
                Log.e(TAG, "Error loading practices: ${exception.message}", exception)

                if (practicesList.isEmpty()) {
                    _uiState.value = ReportsState.Error(
                        exception.message ?: "Error al cargar prácticas"
                    )
                } else {
                    // Si ya tenemos prácticas cargadas, mantenerlas y mostrar un mensaje
                    _uiEvent.send(ReportsEvent.ShowError("Error al cargar más prácticas"))
                    _uiState.value = ReportsState.Success(
                        practices = practicesList.toList(),
                        hasMore = false
                    )
                }
            }
        )
    }

    fun getPracticeAndNavigate(practiceId: Int) {
        viewModelScope.launch {
            val uid = SecureStorage.getUid()

            if (uid.isNullOrEmpty()) {
                Log.e(TAG, "No UID found")
                _uiEvent.send(ReportsEvent.ShowError("Usuario no autenticado"))
                return@launch
            }

            Log.d(TAG, "Fetching updated practice $practiceId before navigation")

            val result = getPracticeByIdUseCase.execute(uid, practiceId)

            result.fold(
                onSuccess = { practice ->
                    Log.d(TAG, "Practice $practiceId fetched successfully, navigating...")
                    _uiEvent.send(ReportsEvent.NavigateToDetailsWithData(practice))
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error fetching practice: ${exception.message}", exception)
                    _uiEvent.send(ReportsEvent.ShowError(
                        exception.message ?: "Error al cargar la práctica"
                    ))
                }
            )
        }
    }

    fun onPracticeClicked(practiceId: Int) {
        viewModelScope.launch {
            _uiEvent.send(ReportsEvent.NavigateToDetails(practiceId))
        }
    }

    fun retry() {
        loadInitialPractices()
    }
}