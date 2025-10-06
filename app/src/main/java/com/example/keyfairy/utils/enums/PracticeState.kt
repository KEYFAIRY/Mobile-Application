package com.example.keyfairy.utils.enums

enum class PracticeState(val label: String) {
    COMPLETED("COMPLETED"), // Back ha realizado proceso compleo, se puede ver el pdf y los errores
    ANALYZED("ANALYZED"), // Back aun está haciendo el pdf, solo se pueden ver los errores
    IN_PROGRESS("IN_PROGRESS"), // Back no ha terminado de analizar, no se puede ver nada
    FINISHED("FINISHED"), // Back ha terminado de analizar, se puede ver solo el pdf
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromLabel(label: String): PracticeState {
            return PracticeState.values()
                .find { it.label.equals(label, ignoreCase = true) } ?: PracticeState.UNKNOWN
        }

        fun labels(): List<String> = PracticeState.values().map { it.label }
    }
}