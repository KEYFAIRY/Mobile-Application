package com.example.keyfairy.feature_auth.domain.repository

import com.example.keyfairy.feature_auth.domain.model.User
import com.example.keyfairy.utils.enums.PianoLevel

/**
 * Repository interface for user data operations
 */
interface UserRepository {

    /**
     * Create user profile in database
     */
    suspend fun createUserProfile(
        uid: String,
        email: String,
        name: String,
        pianoLevel: PianoLevel
    ): Result<User>

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(uid: String, pianoLevel: PianoLevel): Result<User>

    /**
     * Get user profile by UID
     */
    suspend fun getUserProfile(uid: String): Result<User>
}