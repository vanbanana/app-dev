package com.example.sketchto3view.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        const val PREFS_NAME = "sketch3view_settings"
        const val KEY_API_KEY = "api_key"
        const val KEY_BASE_URL = "base_url"
        const val KEY_FIRST_LAUNCH = "first_launch_done"
        const val DEFAULT_API_KEY = "sk-1o2GpxQKS1pqWE19kOJlwls7Ssd0t5jU35NByNthSJenxK3w"
        const val DEFAULT_BASE_URL = "https://api.bltcy.ai"
    }

    data class SettingsState(
        val apiKey: String = "",
        val baseUrl: String = DEFAULT_BASE_URL,
        val isSaved: Boolean = false
    )

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        loadSettings()
    }

    private fun loadSettings() {
        // On first launch, pre-populate with built-in defaults
        val firstLaunchDone = prefs.getBoolean(KEY_FIRST_LAUNCH, false)
        if (!firstLaunchDone) {
            prefs.edit()
                .putString(KEY_API_KEY, DEFAULT_API_KEY)
                .putString(KEY_BASE_URL, DEFAULT_BASE_URL)
                .putBoolean(KEY_FIRST_LAUNCH, true)
                .apply()
        }

        val apiKey = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        val baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        _state.value = SettingsState(
            apiKey = apiKey,
            baseUrl = baseUrl,
            isSaved = false
        )
    }

    fun updateApiKey(apiKey: String) {
        _state.value = _state.value.copy(apiKey = apiKey, isSaved = false)
    }

    fun updateBaseUrl(baseUrl: String) {
        _state.value = _state.value.copy(baseUrl = baseUrl, isSaved = false)
    }

    fun saveSettings() {
        viewModelScope.launch {
            prefs.edit()
                .putString(KEY_API_KEY, _state.value.apiKey)
                .putString(KEY_BASE_URL, _state.value.baseUrl)
                .apply()
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
