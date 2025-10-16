package com.example.keyfairy.utils.common

import android.content.Context
import java.util.Properties

/**
 * Configuration manager that reads settings from config.properties file
 * Must be initialized before use by calling Config.init(context)
 */
object Config {

    private var properties: Properties? = null
    private var isInitialized = false

    /**
     * Initialize the configuration by loading properties from assets
     * Call this once in Application.onCreate()
     */
    fun init(context: Context) {
        if (isInitialized) return

        try {
            properties = Properties().apply {
                context.assets.open("config.properties").use { inputStream ->
                    load(inputStream)
                }
            }
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to default values if config file is missing
            properties = Properties()
        }
    }

    /**
     * Get base URL for API calls
     */
    fun getBaseUrl(): String {
        checkInitialized()
        return properties?.getProperty("base_url")
            ?: "https://default-api-url.com/"
    }

    /**
     * Get connection timeout in seconds
     */
    fun getConnectTimeout(): Long {
        checkInitialized()
        return properties?.getProperty("connect_timeout")?.toLongOrNull() ?: 30L
    }

    /**
     * Get read timeout in seconds
     */
    fun getReadTimeout(): Long {
        checkInitialized()
        return properties?.getProperty("read_timeout")?.toLongOrNull() ?: 30L
    }

    /**
     * Get write timeout in seconds
     */
    fun getWriteTimeout(): Long {
        checkInitialized()
        return properties?.getProperty("write_timeout")?.toLongOrNull() ?: 30L
    }

    /**
     * Check if logging is enabled
     */
    fun isLoggingEnabled(): Boolean {
        checkInitialized()
        return properties?.getProperty("enable_logging")?.toBoolean() ?: true
    }

    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException(
                "Config not initialized. Call Config.init(context) in Application.onCreate()"
            )
        }
    }
}