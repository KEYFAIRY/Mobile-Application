package com.example.keyfairy.utils.enums

/**
 * Piano proficiency levels
 */
enum class PianoLevel(val value: String) {
    BEGINNER("principiante"),
    INTERMEDIATE("intermedio"),
    ADVANCED("avanzado");

    companion object {
        fun fromString(value: String): PianoLevel {
            return values().find { it.value.equals(value, ignoreCase = true) }
                ?: BEGINNER
        }
    }
}