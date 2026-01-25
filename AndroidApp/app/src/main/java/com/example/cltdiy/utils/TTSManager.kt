package com.example.cltdiy.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TTSManager(context: Context) {
    private var tts: TextToSpeech? = null
    var onSpeechCompleted: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE // Or Locale("zh", "CN")
                setupUtteranceListener()
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                onSpeechCompleted?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}
        })
    }

    fun speak(text: String, id: String? = "TTS_MESSAGE", queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        tts?.speak(text, queueMode, null, id)
    }

    fun playSilence(durationInMs: Long, queueMode: Int = TextToSpeech.QUEUE_ADD, id: String? = null) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            tts?.playSilentUtterance(durationInMs, queueMode, id)
        }
    }

    fun updateConfig(rate: Float, pitch: Float) {
        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)
    }

    fun setLanguage(locale: Locale) {
        tts?.language = locale
    }


    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
