package com.quantumproperty.qcai.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class SpeechManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    fun startRecording(languageTag: String = "zh-CN", onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Voice recognition not available on this device")
            return
        }

        // Clean up previous instance
        speechRecognizer?.destroy()
        _transcript.value = ""
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _isRecording.value = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _isRecording.value = false }
            override fun onError(error: Int) {
                _isRecording.value = false
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found (Error 7).\n\nSimulator Tips:\n1. Ensure 'Microphone' is enabled in Emulator settings (three dots -> Microphone).\n2. Note: Simulators often miss Google Speech Services. Try the new 'Text Input' field below!"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Speech recognition error: $error"
                }
                onError(message)
            }
            override fun onResults(results: Bundle?) {
                 val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                 if (!matches.isNullOrEmpty()) {
                     _transcript.value = matches[0]
                 }
                 _isRecording.value = false
            }
            override fun onPartialResults(partialResults: Bundle?) {
                 val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                 if (!matches.isNullOrEmpty()) {
                     _transcript.value = matches[0]
                 }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopRecording() {
        speechRecognizer?.stopListening()
        _isRecording.value = false
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }
}
