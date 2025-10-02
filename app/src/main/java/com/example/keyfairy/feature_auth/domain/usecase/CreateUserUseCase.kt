package com.example.keyfairy.feature_auth.domain.usecase

import com.example.keyfairy.feature_auth.domain.model.User
import com.example.keyfairy.feature_auth.domain.repository.AuthRepository
import com.example.keyfairy.feature_auth.domain.repository.UserRepository
import com.example.keyfairy.utils.enums.PianoLevel

/**
 * Complete registration flow:
 * 1. Register auth credentials in Firebase
 * 2. Create user profile in database
 * 3. Login user
 */
class CreateUserUseCase (
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend fun execute(
        email: String,
        password: String,
        name: String,
        pianoLevel: PianoLevel
    ): Result<User> {
        // Paso 1: Register in Firebase Auth
        val registerResult = authRepository.register(email, password)

        if (registerResult.isFailure) {
            return Result.failure(registerResult.exceptionOrNull()!!)
        }

        val uid = registerResult.getOrNull()!!

        // Paso 2: Create user profile in database
        val createProfileResult = userRepository.createUserProfile(
            uid = uid,
            email = email,
            name = name,
            pianoLevel = pianoLevel
        )

        if (createProfileResult.isFailure) {
            return Result.failure(createProfileResult.exceptionOrNull()!!)
        }

        // Paso 3: LOGIN user
        val loginResult = authRepository.login(email, password)

        if (loginResult.isFailure) {
            return Result.failure(
                Exception("Cuenta creada pero error al iniciar sesión: ${loginResult.exceptionOrNull()?.message}")
            )
        }

        return createProfileResult
    }
}