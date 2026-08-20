package com.example.data.ai

import android.content.Context
import android.content.SharedPreferences

object AiPreferencesHelper {

    private const val PREFS_NAME = "konpro_ai_config"
    private const val KEY_ENDPOINT = "ai_endpoint"
    private const val KEY_API_KEY = "ai_api_key"
    private const val KEY_MODEL = "ai_model"
    private const val KEY_CUSTOM_PERSONA = "ai_custom_persona"
    private const val KEY_TEMPERATURE = "ai_temperature"
    private const val KEY_IS_ENABLED = "ai_is_enabled"

    const val DEFAULT_ENDPOINT = "https://ai.drakor.pp.ua/v1"
    const val DEFAULT_API_KEY = "freellmapi-f657ed0085e8e45b7282037af89d6712e8fb0db6860fcf90"
    const val DEFAULT_MODEL = "auto"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAiConfig(context: Context): AiConfig {
        val prefs = getPrefs(context)
        val endpoint = prefs.getString(KEY_ENDPOINT, null)
        val apiKey = prefs.getString(KEY_API_KEY, null)
        val model = prefs.getString(KEY_MODEL, null)

        return AiConfig(
            endpoint = if (endpoint.isNullOrBlank() || endpoint == "https://api.openai.com/v1") DEFAULT_ENDPOINT else endpoint,
            apiKey = if (apiKey.isNullOrBlank()) DEFAULT_API_KEY else apiKey,
            model = if (model.isNullOrBlank() || model == "gpt-4o-mini") DEFAULT_MODEL else model,
            customPersona = prefs.getString(KEY_CUSTOM_PERSONA, "") ?: "",
            temperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f).toDouble(),
            isEnabled = prefs.getBoolean(KEY_IS_ENABLED, true)
        )
    }

    fun saveAiConfig(context: Context, config: AiConfig) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_ENDPOINT, config.endpoint.trim())
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_MODEL, config.model.trim())
            .putString(KEY_CUSTOM_PERSONA, config.customPersona.trim())
            .putFloat(KEY_TEMPERATURE, config.temperature.toFloat())
            .putBoolean(KEY_IS_ENABLED, config.isEnabled)
            .apply()
    }
}
