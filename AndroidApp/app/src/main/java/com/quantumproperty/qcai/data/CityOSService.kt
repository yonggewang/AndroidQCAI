package com.quantumproperty.qcai.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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

    suspend fun fetchDailyBrief(city: String = "Charlotte"): DailyBriefResponse = withContext(Dispatchers.IO) {
        val url = "$baseUrl/today?city=$city"
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

    companion object {
        val instance = CityOSService()
    }
}
