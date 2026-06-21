package com.example.sketchto3view.data.api

import android.content.SharedPreferences

/**
 * Configuration for the image generation API layer.
 * Reads API key and base URL dynamically from SharedPreferences so that
 * changes made in Settings take effect immediately without app restart.
 *
 * @param prefs SharedPreferences instance to read settings from.
 * @param timeoutMs Timeout in milliseconds for HTTP requests (connect, read, write).
 */
class ApiLayerConfig(
    private val prefs: SharedPreferences,
    val timeoutMs: Long = 60_000
) {
    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val DEFAULT_API_KEY = "sk-1o2GpxQKS1pqWE19kOJlwls7Ssd0t5jU35NByNthSJenxK3w"
        private const val DEFAULT_BASE_URL = "https://api.bltcy.ai"
    }

    /**
     * The API key for authenticating with the generation service.
     * Read dynamically from SharedPreferences.
     */
    val apiKey: String
        get() = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY

    /**
     * The base URL of the image generation API endpoint.
     * Read dynamically from SharedPreferences.
     */
    val baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
}
