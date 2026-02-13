package com.quantumproperty.qcai.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.concurrent.TimeUnit


class CityOSService {
    // Android Emulator uses 10.0.2.2 for localhost
    // PROD: https://cyberpandaapp.com/city
    
    private val baseUrl = "https://cyberpandaapp.com/city"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchDailyBrief(city: String = "Charlotte", forceRefresh: Boolean = false): DailyBriefResponse = withContext(Dispatchers.IO) {
        val url = if (forceRefresh) "$baseUrl/today?city=$city&refresh=true" else "$baseUrl/today?city=$city"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch daily brief: ${response.code}")
        }

        val json = response.body?.string() ?: "{}"
        gson.fromJson(json, DailyBriefResponse::class.java)
    }

    suspend fun queryChat(
        question: String,
        engine: String? = null,
        userAddress: String? = null,
        language: String? = "en",
        customPrompt: String? = null  // For specialized queries like Property Analysis
    ): ChatResponse = withContext(Dispatchers.IO) {
        val url = "$baseUrl/chat/query"
        val bodyMap = mutableMapOf(
            "question" to question,
            "engine" to (engine ?: "Gemini"),
            "user_address" to (userAddress ?: ""),
            "language" to (language ?: "en")
        )
        
        // Add custom_prompt if provided
        if (customPrompt != null) {
            bodyMap["custom_prompt"] = customPrompt
        }
        
        val jsonBody = gson.toJson(bodyMap)
        val body = okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Failed to query City OS: ${response.code}")
        }

        val json = response.body?.string() ?: "{}"
        gson.fromJson(json, ChatResponse::class.java)
    }

    suspend fun registerDeviceToken(token: String, platform: String = "android") = withContext(Dispatchers.IO) {
        val url = "$baseUrl/notifications/register"
        val jsonBody = "{\"token\": \"$token\", \"platform\": \"$platform\"}"
        val body = okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)
        
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
            
        try {
            val response = client.newCall(request).execute()
            println("Notification Register Status: ${response.code}")
        } catch (e: Exception) {
            println("Failed to register notification token: ${e.message}")
        }
    }

    suspend fun fetchScene(vibe: String): SceneResponse = withContext(Dispatchers.IO) {
        val encodedVibe = java.net.URLEncoder.encode(vibe, "UTF-8")
        val url = "$baseUrl/scene?vibe=$encodedVibe"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch scene: ${response.code}")
        }
        
        val json = response.body?.string() ?: "{}"
        gson.fromJson(json, SceneResponse::class.java)
    }

    companion object {
        val instance = CityOSService()
    }
}
