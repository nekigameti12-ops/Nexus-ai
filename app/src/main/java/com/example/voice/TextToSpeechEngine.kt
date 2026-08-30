package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechEngine(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.ENGLISH
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun speak(text: String, pitch: Float = 1.0f, rate: Float = 1.0f, language: String = "auto") {
        if (!isInitialized || tts == null) return

        stop()

        // Clean markdown symbols from spoken voice
        val cleanText = text
            .replace(Regex("""[*#_`>~]"""), "")
            .replace(Regex("""\[.*?\]\(.*?\)"""), "")
            .trim()

        if (cleanText.isEmpty()) return

        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))

        when (language) {
            "hi" -> tts?.language = Locale("hi", "IN")
            "en" -> tts?.language = Locale.ENGLISH
            else -> {
                // Auto-detect Hindi characters
                val hasHindi = cleanText.any { it.code in 0x0900..0x097F }
                tts?.language = if (hasHindi) Locale("hi", "IN") else Locale.ENGLISH
            }
        }

        val utteranceId = "NEXUS_TTS_${System.currentTimeMillis()}"
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isInitialized && tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignore shutdown errors
        }
        tts = null
        isInitialized = false
        _isSpeaking.value = false
    }
}
