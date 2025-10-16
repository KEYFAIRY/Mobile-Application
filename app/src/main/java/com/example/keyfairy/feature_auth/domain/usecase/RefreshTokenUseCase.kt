package com.example.keyfairy.feature_auth.domain.usecase

import com.example.keyfairy.feature_auth.domain.repository.AuthRepository

/**
 * Refresh authentication token
 */
class RefreshTokenUseCase(
    private val repository: AuthRepository
) {

    suspend fun execute(refreshToken: String): Result<Pair<String, String>> {
        return repository.refreshToken(refreshToken)
    }
}