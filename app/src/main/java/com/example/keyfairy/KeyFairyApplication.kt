package com.example.keyfairy

import android.app.Application
import com.example.keyfairy.utils.common.Config

class KeyFairyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize configuration
        Config.init(this)

        // TODO: Initialize other app-wide components
    }
}