package com.quantumproperty.qcai.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit


class CityOSService {
    // Android Emulator uses 10.0.2.2 for localhost
    // PROD: https://cyberpandaapp.com/city
    
    private val baseUrl = "https://cyberpandaapp.com/city"
    private val aiNewsUrl = "https://yonggewang.github.io/ainews/summary.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchDailyBrief(city: String = "Charlotte", language: String = "en", forceRefresh: Boolean = false): DailyBriefResponse = withContext(Dispatchers.IO) {
        val url = if (forceRefresh) "$baseUrl/today?city=$city&lang=$language&refresh=true" else "$baseUrl/today?city=$city&lang=$language"
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

    suspend fun fetchAINewsArticles(language: String = "en"): List<AINewsArticle> = withContext(Dispatchers.IO) {
        val url = when (language) {
            "cn", "zh" -> "https://yonggewang.github.io/ainews/summary_CN.json"
            "es" -> "https://yonggewang.github.io/ainews/summary_ES.json"
            else -> "https://yonggewang.github.io/ainews/summary.json"
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            return@withContext emptyList()
        }

        val json = response.body?.string() ?: "[]"
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<AINewsArticle>>() {}.type
            val articles: List<AINewsArticle> = gson.fromJson(json, type)
            // Return sorting by impact score
            return@withContext articles.sortedByDescending { it.impactScore }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
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
        val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

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
        val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
        
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

    // Local Data System (Restored)
    data class Neighborhood(val name: String, val tag: String, val icon: String, val color: String, val url: String)
    data class CityService(val nameEn: String, val nameZh: String, val icon: String, val color: String, val url: String)

    fun getNeighborhoods(): List<Neighborhood> {
        return listOf(
            Neighborhood("Uptown", "The Hub", "Business", "#00B0FF", "https://www.charlotteobserver.com/news/local/article308393505.html"),
            Neighborhood("NoDa", "Arts District", "Brush", "#FF4081", "https://www.charlottesgotalot.com/neighborhoods/noda"),
            Neighborhood("South End", "Active Living", "DirectionsRun", "#00E676", "https://www.charlotteobserver.com/news/local/article306309761.html"),
            Neighborhood("Plaza Midwood", "Eclectic", "Restaurant", "#FF9100", "https://www.charlottesgotalot.com/neighborhoods/plaza-midwood") // Fixed hex
        )
    }

    fun getCityServices(): List<CityService> {
        return listOf(
            CityService("311 Request", "311 请求", "Phone", "#4CAF50", "https://www.charlottenc.gov/Help311"),
            CityService("Trash Schedule", "垃圾回收", "Delete", "#8D6E63", "https://www.charlottenc.gov/Services/Trash-and-Recycling"),
            CityService("Transit", "公共交通", "DirectionsBus", "#2196F3", "https://www.charlottenc.gov/CATS"),
            CityService("Permits", "许可证", "Description", "#FF9800", "https://aca-prod.accela.com/CHARLOTTE")
        )
    }

    companion object {
        val instance = CityOSService()
    }
}
