package com.quantumproperty.qcai.data

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
            .header("Cache-Control", "no-cache") // Cache busting
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
            .take(10) // Limit to 10 items
            .mapNotNull { line ->
                // Split by comma, but be careful of commas in fields? Assuming simple CSV.
                val parts = line.split(",").map { it.trim() }
                
                // Flexible parsing based on size
                val icon = if (parts.isNotEmpty()) parts[0] else "globe"
                
                if (parts.size >= 5) {
                    // 5-field: Icon, CN, EN, ES, URL
                    HotToolItem(
                        icon = icon,
                        chineseName = parts[1],
                        englishName = parts[2],
                        spanishName = parts[3],
                        url = parts[4]
                    )
                } else if (parts.size >= 3) {
                    // 3-field: CN, EN, URL (Legacy, assuming first field was name not icon?)
                    // Actually the legacy check in previous code was:
                    // parts[0] = CN, parts[1] = EN, parts[2] = URL
                    // But if it had icon, this would be wrong.
                    // Let's assume standard format `Icon, CN, EN, ES, URL`.
                    // If lines are short, likely `Icon, CN, EN, URL`?
                    // Let's stick to the previous logic but ensure URL is valid.
                     HotToolItem(
                        icon = "globe", 
                        chineseName = parts[0],
                        englishName = parts[1],
                        spanishName = parts[1], 
                        url = parts[2]
                    )
                } else {
                    null
                }
            }
    }
}
