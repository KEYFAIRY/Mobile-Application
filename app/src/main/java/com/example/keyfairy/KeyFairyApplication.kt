package com.example.keyfairy

import android.app.Application
import com.example.keyfairy.utils.common.Config
import com.example.keyfairy.utils.storage.AuthenticationManager
import com.example.keyfairy.utils.storage.SecureStorage
import com.example.keyfairy.utils.storage.TokenManager

class
KeyFairyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize configuration
        Config.init(this)

        // Initialize SecureStorage
        SecureStorage.init(this)

        TokenManager.init(this)

        AuthenticationManager.init(this)
    }
}