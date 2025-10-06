package com.example.keyfairy.feature_auth.data.mapper

import com.example.keyfairy.feature_auth.data.remote.dto.response.UserResponse
import com.example.keyfairy.feature_auth.domain.model.User
import com.example.keyfairy.utils.enums.PianoLevel

/**
 * Maps user DTOs to domain models
 */
object UserMapper {

    fun userResponseToDomain(response: UserResponse): User {
        return User(
            uid = response.uid,
            email = response.email,
            name = response.name,
            pianoLevel = PianoLevel.fromLabel(response.pianoLevel)
        )
    }

    fun userResponseListToDomain(responses: List<UserResponse>): List<User> {
        return responses.map { userResponseToDomain(it) }
    }
}