package com.example.keyfairy.feature_auth.domain.usecase

import com.example.keyfairy.feature_auth.domain.model.User
import com.example.keyfairy.feature_auth.domain.repository.UserRepository
import com.example.keyfairy.utils.enums.PianoLevel

class UpdateProfileUseCase (
    private val userRepository: UserRepository
){
    suspend fun execute(uid: String, pianoLevel: PianoLevel): Result<User> {
        return when {
            uid.isBlank() -> {
                Result.failure(Exception("UID no puede estar vacío"))
            }
            else -> {
                userRepository.updateUserProfile(uid, pianoLevel)
            }
        }
    }
}