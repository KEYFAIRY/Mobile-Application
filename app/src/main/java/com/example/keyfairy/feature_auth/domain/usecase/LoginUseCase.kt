package com.example.keyfairy.feature_auth.domain.usecase

import com.example.keyfairy.feature_auth.domain.model.AuthUser
import com.example.keyfairy.feature_auth.domain.repository.AuthRepository

/**
 * Login user and return auth data
 */
class LoginUseCase(
    private val repository: AuthRepository
) {

    suspend fun execute(email: String, password: String): Result<AuthUser> {
        return repository.login(email, password)
    }
}