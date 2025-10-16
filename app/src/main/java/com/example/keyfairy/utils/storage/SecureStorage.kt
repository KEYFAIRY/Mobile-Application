package com.example.keyfairy.utils.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage using EncryptedSharedPreferences
 * Stores sensitive data like tokens encrypted
 */
object SecureStorage {

    private const val FILE_NAME = "keyfairy_secure_prefs"
    private const val KEY_UID = "user_uid"
    private const val KEY_ID_TOKEN = "id_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_TOKEN_EXPIRY = "token_expiry_time"

    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences == null) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    // UID
    fun saveUid(uid: String) {
        sharedPreferences?.edit()?.putString(KEY_UID, uid)?.apply()
    }

    fun getUid(): String? {
        return sharedPreferences?.getString(KEY_UID, null)
    }

    // ID Token
    fun saveIdToken(token: String) {
        sharedPreferences?.edit()?.putString(KEY_ID_TOKEN, token)?.apply()
    }

    fun getIdToken(): String? {
        return sharedPreferences?.getString(KEY_ID_TOKEN, null)
    }

    // Refresh Token
    fun saveRefreshToken(token: String) {
        sharedPreferences?.edit()?.putString(KEY_REFRESH_TOKEN, token)?.apply()
    }

    fun getRefreshToken(): String? {
        return sharedPreferences?.getString(KEY_REFRESH_TOKEN, null)
    }

    // Token Expiry (timestamp en milisegundos)
    fun saveTokenExpiry(expiryTime: Long) {
        sharedPreferences?.edit()?.putLong(KEY_TOKEN_EXPIRY, expiryTime)?.apply()
    }

    fun getTokenExpiry(): Long {
        return sharedPreferences?.getLong(KEY_TOKEN_EXPIRY, 0L) ?: 0L
    }

    fun isTokenExpired(): Boolean {
        val expiryTime = getTokenExpiry()
        val currentTime = System.currentTimeMillis()
        // Considerar expirado 5 minutos antes para tener margen
        return currentTime >= (expiryTime - 5 * 60 * 1000)
    }

    fun saveAuthData(uid: String, idToken: String, refreshToken: String, expiresIn: Long = 3600) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
        sharedPreferences?.edit()?.apply {
            putString(KEY_UID, uid)
            putString(KEY_ID_TOKEN, idToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_TOKEN_EXPIRY, expiryTime)
            apply()
        }
    }

    fun clearAll() {
        sharedPreferences?.edit()?.clear()?.apply()
    }

    fun hasValidSession(): Boolean {
        return !getIdToken().isNullOrEmpty() && !isTokenExpired()
    }
}