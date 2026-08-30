package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NexusSettings(
    val wakeWordEnabled: Boolean = true,
    val doubleClapEnabled: Boolean = false,
    val backgroundServiceEnabled: Boolean = false,
    val autoSpeakResponses: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val preferredLanguage: String = "auto", // "auto", "en", "hi", "hinglish"
    val hapticsEnabled: Boolean = true,
    val clapSensitivity: Float = 0.7f // 0.1 to 1.0
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nexus_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<NexusSettings> = _settings.asStateFlow()

    private fun loadSettings(): NexusSettings {
        return NexusSettings(
            wakeWordEnabled = prefs.getBoolean(KEY_WAKE_WORD, true),
            doubleClapEnabled = prefs.getBoolean(KEY_DOUBLE_CLAP, false),
            backgroundServiceEnabled = prefs.getBoolean(KEY_BG_SERVICE, false),
            autoSpeakResponses = prefs.getBoolean(KEY_AUTO_SPEAK, true),
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f),
            speechPitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f),
            preferredLanguage = prefs.getString(KEY_PREF_LANG, "auto") ?: "auto",
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
            clapSensitivity = prefs.getFloat(KEY_CLAP_SENSITIVITY, 0.7f)
        )
    }

    fun updateWakeWord(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD, enabled).apply()
        _settings.value = _settings.value.copy(wakeWordEnabled = enabled)
    }

    fun updateDoubleClap(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DOUBLE_CLAP, enabled).apply()
        _settings.value = _settings.value.copy(doubleClapEnabled = enabled)
    }

    fun updateBackgroundService(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BG_SERVICE, enabled).apply()
        _settings.value = _settings.value.copy(backgroundServiceEnabled = enabled)
    }

    fun updateAutoSpeak(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply()
        _settings.value = _settings.value.copy(autoSpeakResponses = enabled)
    }

    fun updateSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
        _settings.value = _settings.value.copy(speechRate = rate)
    }

    fun updateSpeechPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_SPEECH_PITCH, pitch).apply()
        _settings.value = _settings.value.copy(speechPitch = pitch)
    }

    fun updatePreferredLanguage(lang: String) {
        prefs.edit().putString(KEY_PREF_LANG, lang).apply()
        _settings.value = _settings.value.copy(preferredLanguage = lang)
    }

    fun updateHaptics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _settings.value = _settings.value.copy(hapticsEnabled = enabled)
    }

    fun updateClapSensitivity(sensitivity: Float) {
        prefs.edit().putFloat(KEY_CLAP_SENSITIVITY, sensitivity).apply()
        _settings.value = _settings.value.copy(clapSensitivity = sensitivity)
    }

    companion object {
        private const val KEY_WAKE_WORD = "wake_word_enabled"
        private const val KEY_DOUBLE_CLAP = "double_clap_enabled"
        private const val KEY_BG_SERVICE = "background_service_enabled"
        private const val KEY_AUTO_SPEAK = "auto_speak_responses"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_PREF_LANG = "preferred_language"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_CLAP_SENSITIVITY = "clap_sensitivity"
    }
}
