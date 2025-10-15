package com.example.keyfairy.utils.enums

enum class Figure(val value: Double, val label: String) {
    BLANCA(2.0, "Blanca"),
    NEGRA(1.0, "Negra"),
    CORCHEA(0.5, "Corchea");

    companion object {
        fun fromValue(value: Double): Figure? =
            values().find { it.value == value }

        fun fromLabel(label: String): Figure? =
            values().find { it.label == label }
    }

    override fun toString(): String = label
}
