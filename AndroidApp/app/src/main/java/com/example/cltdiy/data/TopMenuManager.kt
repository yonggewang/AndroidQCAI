package com.example.cltdiy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class TopMenuManager {
    // Re-instantiate OkHttp client with tight timeouts to fail fast if stuck
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val topics = listOf(
        AITopic.WORLD_NEWS,
        AITopic.FINANCE_NEWS,
        AITopic.AI_ANALYSIS,
        AITopic.FOOD,
        AITopic.DIY,
        AITopic.REAL_ESTATE,
        AITopic.LIFE,
        AITopic.MISC
    )

    suspend fun fetchTopMenu(): List<TopMenuItem> = withContext(Dispatchers.IO) {
        // Use simpler URL, rely on Cache-Control header
        val url = "https://quantumpropertyllc.github.io/topmenu.txt"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "CLTDIY-Android/1.0")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Cache-Control", "no-cache")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()

                val text = response.body?.string() ?: return@withContext emptyList()
                // Clean BOM and typical Windows line endings just in case
                val cleanText = text.replace("\uFEFF", "").replace("\r\n", "\n")
                parseMenuText(cleanText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseMenuText(text: String): List<TopMenuItem> {
        val items = mutableListOf<TopMenuItem>()
        val lines = text.lines()
        var topicIndex = 0

        for (line in lines) {
            try {
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) continue

                val components = trimmedLine.split(",", limit = 6)
                if (components.size >= 5) {
                    val icon = components[0].trim()
                    var nameEn = components[1].trim()
                    if (nameEn.equals("Ford", ignoreCase = true)) {
                        nameEn = "Charlotte Food"
                    }
                    val nameCn = components[2].trim()
                    val urlCn = components[3].trim()
                    val urlEn = components[4].trim()

                    if (topicIndex < topics.size) {
                        items.add(TopMenuItem(
                            icon = icon,
                            englishName = nameEn,
                            chineseName = nameCn,
                            chineseUrl = urlCn,
                            englishUrl = urlEn,
                            topic = topics[topicIndex]
                        ))
                        topicIndex++
                    }
                }
            } catch (e: Exception) {
                // Continue to next line if one fails
                e.printStackTrace()
            }
            if (topicIndex >= topics.size) break
        }
        return items
    }
}
