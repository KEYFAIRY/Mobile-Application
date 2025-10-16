package com.example.keyfairy.feature_profile.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyfairy.feature_auth.domain.usecase.GetProfileUseCase
import com.example.keyfairy.feature_auth.domain.usecase.LogoutUseCase
import com.example.keyfairy.feature_auth.domain.usecase.UpdateProfileUseCase
import com.example.keyfairy.feature_profile.presentation.state.ProfileState
import com.example.keyfairy.feature_profile.presentation.state.UpdateProfileState
import com.example.keyfairy.feature_profile.presentation.state.LogoutState
import com.example.keyfairy.utils.enums.PianoLevel
import com.example.keyfairy.utils.storage.SecureStorage
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getUserProfileUseCase: GetProfileUseCase,
    private val updateUserProfileUseCase: UpdateProfileUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _profileState = MutableLiveData<ProfileState>(ProfileState.Idle)
    val profileState: LiveData<ProfileState> = _profileState

    private val _updateProfileState = MutableLiveData<UpdateProfileState>(UpdateProfileState.Idle)
    val updateProfileState: LiveData<UpdateProfileState> = _updateProfileState

    private val _logoutState = MutableLiveData<LogoutState>(LogoutState.Idle)
    val logoutState: LiveData<LogoutState> = _logoutState

    fun loadUserProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            try {
                val uid = SecureStorage.getUid()
                if (uid.isNullOrEmpty()) {
                    _profileState.value = ProfileState.Error("No se encontró información del usuario")
                    return@launch
                }

                val result = getUserProfileUseCase.execute(uid)

                _profileState.value = if (result.isSuccess) {
                    ProfileState.Success(result.getOrNull()!!)
                } else {
                    ProfileState.Error(
                        result.exceptionOrNull()?.message ?: "Error al cargar perfil"
                    )
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Error inesperado")
            }
        }
    }

    fun updatePianoLevel(pianoLevel: PianoLevel) {
        viewModelScope.launch {
            _updateProfileState.value = UpdateProfileState.Loading

            try {
                val uid = SecureStorage.getUid()
                if (uid.isNullOrEmpty()) {
                    _updateProfileState.value = UpdateProfileState.Error("No se encontró información del usuario")
                    return@launch
                }

                val result = updateUserProfileUseCase.execute(uid, pianoLevel)

                _updateProfileState.value = if (result.isSuccess) {
                    // Recargar perfil después de actualizar
                    loadUserProfile()
                    UpdateProfileState.Success
                } else {
                    UpdateProfileState.Error(
                        result.exceptionOrNull()?.message ?: "Error al actualizar perfil"
                    )
                }
            } catch (e: Exception) {
                _updateProfileState.value = UpdateProfileState.Error(e.message ?: "Error inesperado")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Loading

            try {
                val result = logoutUseCase.execute()

                _logoutState.value = if (result.isSuccess) {
                    LogoutState.Success
                } else {
                    LogoutState.Error(
                        result.exceptionOrNull()?.message ?: "Error al cerrar sesión"
                    )
                }
            } catch (e: Exception) {
                _logoutState.value = LogoutState.Error(e.message ?: "Error inesperado")
            }
        }
    }

    fun resetStates() {
        _profileState.value = ProfileState.Idle
        _updateProfileState.value = UpdateProfileState.Idle
        _logoutState.value = LogoutState.Idle
    }
}