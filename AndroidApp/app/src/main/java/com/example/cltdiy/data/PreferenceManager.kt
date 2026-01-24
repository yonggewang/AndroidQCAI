package com.example.cltdiy.data

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREF_NAME = "CLTDIY_PREFS"
    private const val KEY_OPENAI = "openai_key"
    private const val KEY_GEMINI = "gemini_key"
    private const val KEY_ENGINE = "selected_engine"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var openAIKey: String
        get() = prefs.getString(KEY_OPENAI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENAI, value).apply()

    var geminiKey: String
        get() = prefs.getString(KEY_GEMINI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI, value).apply()

    var selectedEngine: String
        get() = prefs.getString(KEY_ENGINE, AIEngine.GEMINI.name) ?: AIEngine.GEMINI.name
        set(value) = prefs.edit().putString(KEY_ENGINE, value).apply()

    private const val KEY_RATE = "speech_rate"
    private const val KEY_PITCH = "speech_pitch"

    var speechRate: Float
        get() = prefs.getFloat(KEY_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_PITCH, value).apply()
}
