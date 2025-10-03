package com.example.keyfairy.utils.enums

enum class VideoState () {
    ENQUEUED,   // En cola, esperando
    RUNNING,    // Ejecutándose
    SUCCEEDED,  // Completado exitosamente
    FAILED,     // Falló
    BLOCKED,    // Bloqueado (esperando dependencias)
    CANCELLED   // Cancelado por el usuario/sistema
}