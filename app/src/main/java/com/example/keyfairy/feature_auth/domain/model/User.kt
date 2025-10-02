package com.example.keyfairy.feature_auth.domain.model

import com.example.keyfairy.utils.enums.PianoLevel

/**
 * Domain model for complete user information
 */
data class User(
    val uid: String,
    val email: String,
    val name: String,
    val pianoLevel: PianoLevel
)