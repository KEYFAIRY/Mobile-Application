package com.example.keyfairy.feature_home.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyfairy.feature_home.domain.usecase.GetLastPracticeUseCase
import com.example.keyfairy.feature_home.presentation.state.LastPracticeEvent
import com.example.keyfairy.feature_home.presentation.state.LastPracticeState
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.utils.storage.SecureStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LastPracticeViewModel(
    private val getLastPracticeUseCase: GetLastPracticeUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "LastPracticeViewModel"
    }

    private val _uiState = MutableStateFlow<LastPracticeState>(LastPracticeState.Initial)
    val uiState: StateFlow<LastPracticeState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LastPracticeEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private lateinit var practice: Practice

    fun loadPractice() {
        if (_uiState.value is LastPracticeState.Loading) return

        viewModelScope.launch {
            _uiState.value = LastPracticeState.Loading
            loadPracticeInfo()
        }
    }

    private suspend fun loadPracticeInfo() {
        val uid = SecureStorage.getUid()

        if (uid.isNullOrEmpty()) {
            Log.e(TAG, "No UID found in SecureStorage")
            _uiState.value = LastPracticeState.Error("Usuario no autenticado")
            _uiEvent.send(LastPracticeEvent.ShowError("Usuario no autenticado"))
            return
        }

        Log.d(TAG, "Loading last practice for uid=$uid")

        val result = getLastPracticeUseCase.execute(
            uid = uid
        )

        result.fold(
            onSuccess = { practice ->
                if (practice != null) {
                    Log.d(TAG, "Loaded practice.")
                    _uiState.value = LastPracticeState.Success(
                        practice = practice
                    )
                } else {
                    Log.d(TAG, "No practice found for user")
                    _uiState.value = LastPracticeState.NoPractices("No se encontraron prácticas")
                    _uiEvent.send(LastPracticeEvent.NoPractices("No hay ninguna práctica registrada"))
                }
            },
            onFailure = { exception ->
                Log.e(TAG, "Error loading practices: ${exception.message}", exception)

                _uiState.value = LastPracticeState.Error(
                    exception.message ?: "Error al cargar prácticas"
                )
            }
        )
    }
}