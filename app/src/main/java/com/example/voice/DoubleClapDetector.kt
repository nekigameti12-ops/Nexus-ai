package com.example.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class DoubleClapDetector(
    private val context: Context,
    private val onDoubleClap: () -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var listenerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun start(sensitivity: Float = 0.7f) {
        if (_isListening.value) return

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            _isListening.value = true

            // Sensitivity threshold: maps 0.1..1.0 to amplitude threshold 12000..3000
            val threshold = (15000 - (sensitivity * 12000)).toInt().coerceIn(2000, 18000)

            listenerJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                var firstClapTime = 0L
                var lastPeakTime = 0L

                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var maxPeak = 0
                        for (i in 0 until read) {
                            val sample = abs(buffer[i].toInt())
                            if (sample > maxPeak) {
                                maxPeak = sample
                            }
                        }

                        val normalized = (maxPeak / 32767f).coerceIn(0f, 1f)
                        _currentAmplitude.value = normalized

                        val currentTime = System.currentTimeMillis()

                        // Detect sharp transient amplitude peak
                        if (maxPeak > threshold && (currentTime - lastPeakTime > 150)) {
                            lastPeakTime = currentTime

                            if (firstClapTime == 0L) {
                                firstClapTime = currentTime
                            } else {
                                val interval = currentTime - firstClapTime
                                if (interval in 200..900) {
                                    // Valid double clap detected!
                                    firstClapTime = 0L
                                    onDoubleClap()
                                } else {
                                    // Reset window
                                    firstClapTime = currentTime
                                }
                            }
                        }

                        // Timeout window for first clap
                        if (firstClapTime != 0L && (currentTime - firstClapTime > 1000)) {
                            firstClapTime = 0L
                        }
                    }
                }
            }
        } catch (e: Exception) {
            stop()
        }
    }

    fun stop() {
        isRecording = false
        _isListening.value = false
        _currentAmplitude.value = 0f
        listenerJob?.cancel()
        listenerJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore release exceptions
        }
        audioRecord = null
    }
}
