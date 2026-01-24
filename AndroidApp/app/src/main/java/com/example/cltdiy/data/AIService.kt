package com.example.cltdiy.data

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class AIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val openAIEndpoint = "https://api.openai.com/v1/chat/completions"
    // Matching iOS endpoint
    private val geminiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
    private val vercelEndpoint = "https://vercel-backendcltai.vercel.app/api/analyze"

    suspend fun sendMessage(
        text: String, 
        engine: AIEngine, 
        topic: AITopic, 
        language: AppLanguage,
        image: Bitmap? = null,
        realEstateAddress: String? = null
    ): String {
        val systemPrompt = generateSystemPrompt(topic, engine, language)
        
        val openAIKey = PreferenceManager.openAIKey
        val geminiKey = PreferenceManager.geminiKey

        return when (engine) {
            AIEngine.CHATGPT -> {
                if (openAIKey.isEmpty()) throw IOException("Missing OpenAI API Key")
                
                var processedPrompt = systemPrompt
                if (topic == AITopic.REAL_ESTATE) {
                    val addressToSearch = realEstateAddress ?: extractAddress(text)
                    val freshData = fetchDataFromVercel(addressToSearch)
                    if (freshData != null) {
                        processedPrompt += "\n\n【重要：最新房产核实数据】\n以下数据来自实时搜索，请务必将其作为“事实”基础进行分析：\n$freshData"
                    }
                }
                
                sendToChatGPT(openAIKey, processedPrompt, text, image)
            }
            AIEngine.GEMINI -> {
                if (geminiKey.isEmpty()) throw IOException("Missing Gemini API Key")
                sendToGemini(geminiKey, systemPrompt, text, image)
            }
        }
    }

    private fun generateSystemPrompt(topic: AITopic, engine: AIEngine, language: AppLanguage): String {
        val isEnglish = language == AppLanguage.ENGLISH
        
        var basePrompt = if (isEnglish) {
            """
            You are the intelligent assistant for the "Charlotte Chinese AI Hub", primarily serving the Chinese community in Charlotte, North Carolina.
            Your name is "Charlotte Intelligent Helper".
            Please answer questions in English.
            """.trimIndent()
        } else {
            """
            你是“夏村华人AI大全”的智能助手，主要服务于美国北卡罗来纳州夏洛特（Charlotte, NC）的华人社区。
            你的名字是“夏洛特智能帮手”。
            请用中文回答问题。
            """.trimIndent()
        }

        if (engine == AIEngine.GEMINI) {
            basePrompt += "\nYou have access to Google Search. You must use it to verify facts."
        }
        
        val suffix = if (isEnglish) {
            when (topic) {
                AITopic.AI_ANALYSIS -> "\nCurrent Mode: AI Deep Analysis Expert."
                AITopic.DIY -> "\nCurrent Mode: Charlotte Home Maintenance Expert."
                AITopic.FOOD -> "\nCurrent Mode: Charlotte Food Guide."
                AITopic.REAL_ESTATE -> "\nCurrent Mode: Charlotte Real Estate Expert."
                AITopic.WORLD_NEWS -> "\nCurrent Mode: Top News Summarizer."
                AITopic.LIFE -> "\nCurrent Mode: North Carolina Life Advisor."
                AITopic.FINANCE_NEWS -> "\nCurrent Mode: Finance News Expert."
                AITopic.MISC -> "\nCurrent Mode: Miscellaneous Information Assistant."
                AITopic.FORD -> "\nCurrent Mode: Ford Information Expert."
            }
        } else {
            when (topic) {
                AITopic.AI_ANALYSIS -> "\n当前模式：AI深度分析专家。"
                AITopic.DIY -> "\n当前模式：夏村房屋维护专家。"
                AITopic.FOOD -> "\n当前模式：夏村美食向导。"
                AITopic.REAL_ESTATE -> "\n当前模式：夏村房产专家。"
                AITopic.WORLD_NEWS -> "\n当前模式：世界头条摘要。"
                AITopic.LIFE -> "\n当前模式：北卡生活点滴。"
                AITopic.FINANCE_NEWS -> "\n当前模式：财经头条专家。"
                AITopic.MISC -> "\n当前模式：杂项信息助手。"
                AITopic.FORD -> "\n当前模式：福特信息专家。"
            }
        }
        
        return basePrompt + suffix
    }

    private suspend fun sendToChatGPT(apiKey: String, prompt: String, userMessage: String, image: Bitmap?): String = withContext(Dispatchers.IO) {
        val userContent = JSONArray()
        userContent.put(JSONObject().put("type", "text").put("text", userMessage))

        image?.let {
            val base64Image = encodeImage(it)
            val imageUrl = JSONObject().put("url", "data:image/jpeg;base64,$base64Image")
            userContent.put(JSONObject().put("type", "image_url").put("image_url", imageUrl))
        }

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", prompt))
        messages.put(JSONObject().put("role", "user").put("content", userContent))

        val body = JSONObject()
        body.put("model", "gpt-4o-mini")
        body.put("messages", messages)
        body.put("max_tokens", 1000)

        val request = Request.Builder()
            .url(openAIEndpoint)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("ChatGPT Error: ${response.code} ${response.body?.string()}")
            
            val json = JSONObject(response.body?.string() ?: "")
            val choices = json.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            return@withContext message?.optString("content") ?: ""
        }
    }

    private suspend fun sendToGemini(apiKey: String, prompt: String, userMessage: String, image: Bitmap?): String = withContext(Dispatchers.IO) {
        val url = "$geminiEndpoint?key=$apiKey"

        val parts = JSONArray()
        parts.put(JSONObject().put("text", "User Question: $userMessage"))

        image?.let {
            val base64Image = encodeImage(it)
            val inlineData = JSONObject()
            inlineData.put("mime_type", "image/jpeg")
            inlineData.put("data", base64Image)
            parts.put(JSONObject().put("inline_data", inlineData))
        }

        // Gemini v1beta structure
        // System instruction is a top level object
        val systemInstruction = JSONObject()
        systemInstruction.put("parts", JSONArray().put(JSONObject().put("text", prompt)))

        val contents = JSONArray()
        val userContent = JSONObject()
        userContent.put("role", "user")
        userContent.put("parts", parts)
        contents.put(userContent)

        val generationConfig = JSONObject()
        generationConfig.put("temperature", 1.0)
        
        val tools = JSONArray()
        tools.put(JSONObject().put("google_search", JSONObject()))

        val body = JSONObject()
        body.put("system_instruction", systemInstruction)
        body.put("contents", contents)
        body.put("generation_config", generationConfig)
        body.put("tools", tools)

        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Gemini Error: ${response.code} ${response.body?.string()}")

            val json = JSONObject(response.body?.string() ?: "")
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val partsArr = content?.optJSONArray("parts")
            val firstPart = partsArr?.optJSONObject(0)
            return@withContext firstPart?.optString("text") ?: ""
        }
    }
    
    private suspend fun fetchDataFromVercel(address: String): String? = withContext(Dispatchers.IO) {
        if (address.isBlank()) return@withContext null
        
        val jsonBody = JSONObject().put("address", address)
        val request = Request.Builder()
            .url(vercelEndpoint)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    return@withContext json.optString("data")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
    
    private fun extractAddress(text: String): String {
        // Simple extraction logic matching iOS: look for text inside quotes
        val parts = text.split("\"")
        if (parts.size >= 3) {
            return parts[1]
        }
        return text
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
