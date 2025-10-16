package com.example.keyfairy.feature_auth.domain.usecase

import com.example.keyfairy.feature_auth.domain.model.User
import com.example.keyfairy.feature_auth.domain.repository.UserRepository

/**
 * Get user profile by UID
 */
class GetProfileUseCase (
    private val userRepository: UserRepository
){
    suspend fun execute(uid: String): Result<User> {
        return if (uid.isBlank()) {
            Result.failure(Exception("UID no puede estar vacío"))
        } else {
            userRepository.getUserProfile(uid)
        }
    }
}