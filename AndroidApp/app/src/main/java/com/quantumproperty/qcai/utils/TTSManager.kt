package com.quantumproperty.qcai.utils

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSManager(
    private val context: Context,
    private val onInitSuccess: (() -> Unit)? = null,
    private val onInitFailure: ((String) -> Unit)? = null
) {
    private var tts: TextToSpeech? = null
    var onSpeechCompleted: (() -> Unit)? = null
    
    private var isInitialized = false
    private val TAG = "TTSManager"
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    init {
        try {
            Log.d(TAG, "Starting TTS initialization...")
            
            // Check audio setup
            audioManager?.let { am ->
                val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                Log.d(TAG, "Audio - Current volume: $currentVolume/$maxVolume")
                
                if (currentVolume == 0) {
                    Log.w(TAG, "⚠️ Media volume is MUTED! User won't hear TTS.")
                }
            }
            
            tts = TextToSpeech(context) { status ->
                Log.d(TAG, "TTS initialization callback received with status: $status")
                
                when (status) {
                    TextToSpeech.SUCCESS -> {
                        Log.d(TAG, "TTS initialized successfully")
                        // Default to US English instead of Chinese to avoid failure on standard emulators/phones
                        // The ViewModel will override this with the correct user preference immediately after.
                        val result = tts?.setLanguage(Locale.US)
                        
                        when (result) {
                            TextToSpeech.LANG_MISSING_DATA -> {
                                val errorMsg = "Chinese language data is missing. TTS may not work properly."
                                Log.e(TAG, errorMsg)
                                onInitFailure?.invoke(errorMsg)
                                isInitialized = false
                            }
                            TextToSpeech.LANG_NOT_SUPPORTED -> {
                                val errorMsg = "Chinese language is not supported by TTS engine."
                                Log.e(TAG, errorMsg)
                                onInitFailure?.invoke(errorMsg)
                                isInitialized = false
                            }
                            else -> {
                                isInitialized = true
                                setupUtteranceListener()
                                
                                // Set audio attributes for TTS playback
                                tts?.let { engine ->
                                    try {
                                        // Request audio focus
                                        val audioAttributes = android.media.AudioAttributes.Builder()
                                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                            .build()
                                        
                                        // This ensures TTS uses the media stream
                                        Log.d(TAG, "Audio attributes configured for TTS")
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Could not set audio attributes: ${e.message}")
                                    }
                                }
                                
                                Log.d(TAG, "✅ TTS ready! Language: Chinese")
                                onInitSuccess?.invoke()
                            }
                        }
                    }
                    TextToSpeech.ERROR -> {
                        val errorMsg = "TTS initialization failed. Audio playback unavailable."
                        Log.e(TAG, errorMsg)
                        isInitialized = false
                        onInitFailure?.invoke(errorMsg)
                    }
                    else -> {
                        val errorMsg = "TTS initialization returned unknown status: $status"
                        Log.e(TAG, errorMsg)
                        isInitialized = false
                        onInitFailure?.invoke(errorMsg)
                    }
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Exception initializing TTS: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onInitFailure?.invoke(errorMsg)
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "🔊 Speech started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "✅ Speech completed: $utteranceId")
                onSpeechCompleted?.invoke()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "❌ TTS error for utteranceId: $utteranceId, errorCode: $errorCode")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "❌ TTS error for utteranceId: $utteranceId")
            }
        })
    }

    fun speak(text: String, id: String? = "TTS_MESSAGE", queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized) {
            Log.w(TAG, "⚠️ TTS not initialized, cannot speak")
            return
        }
        
        if (text.isBlank()) {
            Log.w(TAG, "⚠️ Empty text, skipping TTS")
            return
        }
        
        // Check volume before speaking
        audioManager?.let { am ->
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentVolume == 0) {
                Log.w(TAG, "⚠️ Warning: Media volume is 0, user won't hear anything!")
            }
        }
        
        Log.d(TAG, "🗣️ Speaking (${text.length} chars): ${text.take(100)}...")
        val result = tts?.speak(text, queueMode, null, id)
        
        when (result) {
            TextToSpeech.ERROR -> {
                Log.e(TAG, "❌ Error speaking text")
            }
            TextToSpeech.SUCCESS -> {
                Log.d(TAG, "✅ TTS speak command successful")
            }
        }
    }

    fun playSilence(durationInMs: Long, queueMode: Int = TextToSpeech.QUEUE_ADD, id: String? = null) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, cannot play silence")
            return
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            tts?.playSilentUtterance(durationInMs, queueMode, id)
            Log.d(TAG, "Playing silence: ${durationInMs}ms")
        }
    }

    fun updateConfig(rate: Float, pitch: Float) {
        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)
        Log.d(TAG, "Updated TTS config - Rate: $rate, Pitch: $pitch")
    }

    fun setLanguage(locale: Locale) {
        val result = tts?.setLanguage(locale)
        when (result) {
            TextToSpeech.LANG_MISSING_DATA -> {
                Log.e(TAG, "Language data missing for: $locale")
            }
            TextToSpeech.LANG_NOT_SUPPORTED -> {
                Log.e(TAG, "Language not supported: $locale")
            }
            else -> {
                Log.d(TAG, "Language set to: $locale")
                
                // Attempt to find a high-quality voice for this locale
                try {
                    val voices = tts?.voices
                    if (voices != null) {
                        // Priority 1: "Network" voices (usually higher quality cloud-based)
                        // Priority 2: Voices with "high" or "enhanced" in the name
                        val bestVoice = voices
                            .filter { it.locale == locale }
                            .sortedByDescending { voice ->
                                var score = 0
                                if (voice.isNetworkConnectionRequired) score += 2
                                if (voice.name.contains("high", ignoreCase = true)) score += 1
                                if (voice.name.contains("enhanced", ignoreCase = true)) score += 1
                                score
                            }
                            .firstOrNull()

                        if (bestVoice != null) {
                            tts?.voice = bestVoice
                            Log.d(TAG, "✅ Selected high-quality voice: ${bestVoice.name}")
                        } else {
                            Log.d(TAG, "No specific high-quality voice found, using default for $locale")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set improved voice: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        tts?.stop()
        Log.d(TAG, "🛑 TTS stopped")
    }

    fun shutdown() {
        tts?.shutdown()
        isInitialized = false
        Log.d(TAG, "TTS shut down")
    }
    
    fun isReady(): Boolean = isInitialized
}
