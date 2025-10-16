package com.example.keyfairy.feature_auth.domain.model

/**
 * Domain model for authenticated user (from auth endpoints)
 */
data class AuthUser(
    val uid: String,
    val email: String,
    val idToken: String,
    val refreshToken: String
)