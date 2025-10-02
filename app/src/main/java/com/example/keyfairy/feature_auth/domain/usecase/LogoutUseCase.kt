package com.example.keyfairy.feature_auth.domain.usecase
import com.example.keyfairy.feature_auth.domain.repository.AuthRepository

/**
 * Logout user and clear session
 */
class LogoutUseCase(
    private val repository: AuthRepository
) {

    suspend fun execute(): Result<Boolean> {
        return repository.logout()
    }
}