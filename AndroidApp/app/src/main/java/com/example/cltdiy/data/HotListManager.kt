package com.example.cltdiy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HotListManager {
    private val client = OkHttpClient()

    suspend fun fetchHotList(): List<HotToolItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://quantumpropertyllc.github.io/hotlist.txt")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val text = response.body?.string() ?: return@withContext emptyList()
                parseHotList(text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseHotList(text: String): List<HotToolItem> {
        return text.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(",")
                if (parts.size >= 3) {
                    HotToolItem(
                        chineseName = parts[0].trim(),
                        englishName = parts[1].trim(),
                        url = parts[2].trim()
                    )
                } else if (parts.size == 2) {
                    // Backward compatibility or legacy format
                    HotToolItem(
                        chineseName = parts[0].trim(),
                        englishName = parts[0].trim(),
                        url = parts[1].trim()
                    )
                } else {
                    null
                }
            }
    }
}
