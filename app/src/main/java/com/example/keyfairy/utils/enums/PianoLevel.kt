package com.example.keyfairy.utils.enums

/**
 * Piano proficiency levels
 */
enum class PianoLevel(val label: String) {
    I("teclado I"),
    II("teclado II"),
    III("teclado III"),
    IV("teclado IV");

    companion object {
        fun fromLabel(label: String): PianoLevel {
            return values().find { it.label.equals(label, ignoreCase = true) } ?: I
        }

        fun labels(): List<String> = values().map { it.label }
    }
}
