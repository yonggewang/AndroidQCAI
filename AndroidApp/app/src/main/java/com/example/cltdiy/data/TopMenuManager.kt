package com.example.cltdiy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class TopMenuManager {
    private val client = OkHttpClient()

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
        val request = Request.Builder()
            .url("https://quantumpropertyllc.github.io/topmenu.txt")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val text = response.body?.string() ?: return@withContext emptyList()
                parseMenuText(text)
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
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            val components = trimmedLine.split(",")
            if (components.size >= 5) {
                val icon = components[0].trim()
                val nameEn = components[1].trim()
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
            if (topicIndex >= topics.size) break
        }
        return items
    }
}
