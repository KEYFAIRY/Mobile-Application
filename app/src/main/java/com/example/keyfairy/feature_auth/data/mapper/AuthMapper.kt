package com.example.keyfairy.feature_auth.data.mapper

import com.example.keyfairy.feature_auth.data.remote.dto.response.LoginResponse
import com.example.keyfairy.feature_auth.domain.model.AuthUser

/**
 * Maps auth DTOs to domain models
 */
object AuthMapper {

    fun loginResponseToDomain(response: LoginResponse): AuthUser {
        return AuthUser(
            uid = response.uid,
            email = response.email,
            idToken = response.idToken,
            refreshToken = response.refreshToken
        )
    }
}