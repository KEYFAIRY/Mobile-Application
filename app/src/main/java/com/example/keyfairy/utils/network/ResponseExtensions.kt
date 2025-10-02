package com.example.keyfairy.utils.network

import retrofit2.Response

fun <T> Response<StandardResponse<T>>.getErrorMessage(): String {
    return when {
        this.body()?.message != null -> this.body()!!.message
        this.code() == 400 -> "Datos inválidos"
        this.code() == 401 -> "Credenciales inválidas"
        this.code() == 403 -> "Acceso denegado"
        this.code() == 404 -> "Recurso no encontrado"
        this.code() == 409 -> "El recurso ya existe"
        this.code() == 422 -> "Error de validación"
        this.code() >= 500 -> "Error del servidor, intenta más tarde"
        else -> "Error desconocido (${this.code()})"
    }
}