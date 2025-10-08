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


    fun onUserLoggedIn(uid: String) {
        Log.d(TAG, "👤 User logged in: $uid")
        videoUploadManager?.onUserChanged(uid)

        // Verificar si hay trabajos pendientes del usuario
        val pendingCount = videoUploadManager?.getCurrentUserPendingUploadsCount() ?: 0
        if (pendingCount > 0) {
            Log.d(TAG, "📤 User $uid has $pendingCount pending uploads")
        }
    }


    fun onUserLoggedOut() {
        val currentUid = SecureStorage.getUid()
        Log.d(TAG, "👤 User logged out: $currentUid")
        Log.d(TAG, "🔄 Uploads will continue in background for user: $currentUid")
    }


    fun getVideoUploadManager(): VideoUploadManager? {
        return videoUploadManager
    }
}