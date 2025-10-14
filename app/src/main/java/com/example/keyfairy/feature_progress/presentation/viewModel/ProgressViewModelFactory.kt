package com.example.keyfairy.feature_progress.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.keyfairy.feature_progress.domain.usecase.*

class ProgressViewModelFactory(
    private val getTopEscalasSemanalesUseCase: GetTopEscalasSemanalesUseCase,
    private val getTiempoPosturasSemanalesUseCase: GetTiempoPosturasSemanalesUseCase,
    private val getNotasResumenSemanalesUseCase: GetNotasResumenSemanalesUseCase,
    private val getErroresPosturalesSemanalesUseCase: GetErroresPosturalesSemanalesUseCase,
    private val getErroresMusicalesSemanalesUseCase: GetErroresMusicalesSemanalesUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            return ProgressViewModel(
                getTopEscalasSemanalesUseCase,
                getTiempoPosturasSemanalesUseCase,
                getNotasResumenSemanalesUseCase,
                getErroresPosturalesSemanalesUseCase,
                getErroresMusicalesSemanalesUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}