package com.example.keyfairy.utils.enums

enum class ScaleType(val displayName: String) {
    MAYOR("Mayor"),
    MENOR("Menor");

    companion object {
        fun fromName(scaleName: String): ScaleType {
            return when {
                scaleName.contains("Mayor", ignoreCase = true) -> MAYOR
                scaleName.contains("Menor", ignoreCase = true) -> MENOR
                else -> MAYOR // fallback por defecto
            }
        }
    }
}
