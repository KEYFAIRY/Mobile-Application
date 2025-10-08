package com.example.keyfairy.utils.storage

import android.content.Context
import android.util.Log
import com.example.keyfairy.utils.workers.VideoUploadManager

object AuthenticationManager {
    private const val TAG = "AuthenticationManager"
    private var videoUploadManager: VideoUploadManager? = null

    fun init(context: Context) {
        videoUploadManager = VideoUploadManager(context)
    }

    /**
     * Llamar cuando el usuario hace login exitosamente
     */
    fun onUserLoggedIn(uid: String) {
        Log.d(TAG, "👤 User logged in: $uid")
        videoUploadManager?.onUserChanged(uid)

        // Verificar si hay trabajos pendientes del usuario
        val pendingCount = videoUploadManager?.getCurrentUserPendingUploadsCount() ?: 0
        if (pendingCount > 0) {
            Log.d(TAG, "📤 User $uid has $pendingCount pending uploads")
        }
    }

    /**
     * Llamar cuando el usuario hace logout
     */
    fun onUserLoggedOut() {
        val currentUid = SecureStorage.getUid()
        Log.d(TAG, "👤 User logged out: $currentUid")

        // DECISIÓN: ¿Cancelar uploads del usuario al cerrar sesión?
        // Opción 1: Mantener uploads (recomendado)
        // - Los uploads continuarán en background
        // - Si el usuario vuelve a hacer login, los verá completados

        // Opción 2: Cancelar uploads (más agresivo)
        // currentUid?.let { uid ->
        //     videoUploadManager?.cleanupUserWork(uid)
        // }

        Log.d(TAG, "🔄 Uploads will continue in background for user: $currentUid")
    }

    /**
     * Obtener el manager de uploads para el usuario actual
     */
    fun getVideoUploadManager(): VideoUploadManager? {
        return videoUploadManager
    }
}