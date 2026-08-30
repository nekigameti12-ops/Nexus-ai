package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SpeechState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

class SpeechRecognizerManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onErrorMsg: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechState = MutableStateFlow(SpeechState.IDLE)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String = "auto") {
        if (!isAvailable()) {
            onErrorMsg("Speech Recognition service not available on this device.")
            _speechState.value = SpeechState.ERROR
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                val selectedLocale = when (languageCode) {
                    "hi" -> "hi-IN"
                    "en" -> "en-IN"
                    else -> Locale.getDefault().toString()
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, selectedLocale)
            }

            _partialText.value = ""
            _speechState.value = SpeechState.LISTENING
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _speechState.value = SpeechState.ERROR
            onErrorMsg("Failed to start voice recognition: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore destroy errors
        } finally {
            speechRecognizer = null
            _speechState.value = SpeechState.IDLE
            _soundLevel.value = 0f
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _speechState.value = SpeechState.LISTENING
            }

            override fun onBeginningOfSpeech() {
                _speechState.value = SpeechState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize rmsdB (-2 to 10 typical) to 0.0 .. 1.0
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _soundLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _speechState.value = SpeechState.PROCESSING
                _soundLevel.value = 0f
            }

            override fun onError(error: Int) {
                _speechState.value = SpeechState.ERROR
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Voice recognition error ($error)"
                }
                if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                    onErrorMsg(message)
                }
                _speechState.value = SpeechState.IDLE
            }

            override fun onResults(results: Bundle?) {
                _speechState.value = SpeechState.IDLE
                _soundLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim()
                if (!recognizedText.isNullOrBlank()) {
                    _partialText.value = recognizedText
                    onResult(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim()
                if (!text.isNullOrBlank()) {
                    _partialText.value = text
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
