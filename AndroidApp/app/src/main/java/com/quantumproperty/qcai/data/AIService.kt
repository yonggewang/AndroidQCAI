package com.quantumproperty.qcai.data

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
import io.github.jan.supabase.gotrue.auth

class AIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    private val openAIEndpoint = "https://api.openai.com/v1/chat/completions"
    // Matching iOS endpoint
    private val geminiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    private val geminiEmbedEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent"
    private val pineconeEndpoint = "https://clt-vibe-rag-e13kol2.svc.aped-4627-b74a.pinecone.io/query"
    private val pineconeKey = "pcsk_7HuA7A_7T3VQ5Fuq6dTk6hwJPZedEJKU5Rk4pDk4PngnJstzMYSGzbpWJrwGaswWNUVNZj"
    private val vercelEndpoint = "https://vercel-backendcltai.vercel.app/api/analyze?teamId=team_DxHeE18WWzpaGC1GLeY3l6oQ"

    suspend fun sendMessage(
        text: String, 
        engine: AIEngine, 
        topic: AITopic, 
        language: AppLanguage,
        image: Bitmap? = null,
        realEstateAddress: String? = null,
        customPrompt: String? = null
    ): String {
        val isEnglish = language == AppLanguage.ENGLISH
        var geminiKey = PreferenceManager.geminiKey
        var openAIKey = PreferenceManager.openAIKey

        // Resolve and verify keys
        when (engine) {
            AIEngine.GEMINI -> {
                val user = supabase.auth.currentUserOrNull()
                if (user == null) {
                    throw IOException(if (isEnglish) "Please log in to use AI features." else "请先登录以使用 AI 功能。")
                }
                
                if (geminiKey.isEmpty()) {
                    try {
                        UserManager().loadAppSecrets()
                        geminiKey = PreferenceManager.geminiKey
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (geminiKey.isEmpty()) {
                        throw IOException(if (isEnglish) "Missing Gemini API Key. Please configure it in settings or check your database connection." else "无法获取 Gemini API 密钥，请在设置中配置或检查数据库连接。")
                    }
                }
            }
            AIEngine.CHATGPT -> {
                val user = supabase.auth.currentUserOrNull()
                if (user == null) {
                    throw IOException(if (isEnglish) "Please log in to use AI features." else "请先登录以使用 AI 功能。")
                }
                
                if (openAIKey.isEmpty()) {
                    try {
                        UserManager().loadAppSecrets()
                        openAIKey = PreferenceManager.openAIKey
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (openAIKey.isEmpty()) {
                        throw IOException(if (isEnglish) "Missing OpenAI API Key. Please configure it in settings or check your database connection." else "无法获取 OpenAI API 密钥，请在设置中配置或检查数据库连接。")
                    }
                }
            }
        }

        var context = ""
        // RAG Logic for CLT Vibe
        if (topic == AITopic.CLT_VIBE && geminiKey.isNotEmpty()) {
            val ragContext = fetchContextFromPinecone(text, geminiKey)
            if (ragContext != null) {
                context = "\n\n【Local Context from RAG】:\n$ragContext"
            }
        }

        var processedPrompt = customPrompt ?: (generateSystemPrompt(topic, engine, language) + context)

        if (topic == AITopic.REAL_ESTATE && customPrompt == null) {
            val addressToSearch = realEstateAddress ?: extractAddress(text)
            val freshData = fetchDataFromVercel(addressToSearch)
            if (freshData != null) {
                processedPrompt += "\n\n【重要：最新房产核实数据】\n以下数据来自实时搜索，请务必将其作为“事实”基础进行分析：\n$freshData"
            }
        }

        return when (engine) {
            AIEngine.CHATGPT -> {
                sendToChatGPT(openAIKey, processedPrompt, text, image)
            }
            AIEngine.GEMINI -> {
                sendToGemini(geminiKey, processedPrompt, text, image)
            }
        }
    }

    private fun generateSystemPrompt(topic: AITopic, engine: AIEngine, language: AppLanguage): String {
        val isEnglish = language == AppLanguage.ENGLISH
        
        var basePrompt = if (isEnglish) {
            """
            You are the intelligent assistant for the "QCAI", primarily serving the Chinese community in Charlotte, North Carolina.
            Your name is "QCAI Assistant".
            Please answer questions in English.
            """.trimIndent()
        } else {
            """
            你是夏洛特的智能助手，主要服务于美国北卡罗来纳州夏洛特（Charlotte, NC）的华人社区。
            你的名字是“QCAI”。
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
                AITopic.CLT_VIBE -> """
                You are the QCAI Concierge for CHARLOTTE, NC (Queen City). Your goal is to provide precise, local, and actionable recommendations.
                
                【RESPONSE RULES】:
                1. NO FILLER. Start directly with the answer.
                2. Use the 【Local Context from RAG】 to provide specific details.
                3. For every recommendation, you MUST provide: Name, Address, and a Vibe description.
                4. You MUST end your response with a `MATCH_SCORE_JSON` block.
                5. The JSON MUST include an "image_url" field (if available in the context).
                
                【JSON FORMAT EXAMPLE】:
                MATCH_SCORE_JSON
                [
                  {
                    "name": "Place Name",
                    "score": 95,
                    "reason": "Direct Vibe description...",
                    "price": "$$",
                    "rating": "4.5",
                    "image_url": "https://..."
                  }
                ]
                """.trimIndent()
                AITopic.STOCK -> "\nCurrent Mode: Stock Analyst."
                AITopic.BUSINESS -> "\nCurrent Mode: AI & Business Expert. Focus on AI technology, hardware (RTX/Workstations), and networking in Charlotte."
                AITopic.COLLEGE -> "\nCurrent Mode: College Admissions Advisor."
                AITopic.NONE -> "\nCurrent Mode: General Assistant."
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
                AITopic.CLT_VIBE -> """
                你是夏洛特（Charlotte, NC）的本地生活专家（QCAI 管家）。你的目标是为华人社区提供极其精准且实用的本地指南。
                
                【垃圾与回收规则】：
                - 如果查询结果显示为“Orange”或“Green”周，请直接告诉用户：“本周是您的回收周，请把【垃圾桶】和【回收桶】都推出来。”
                - 如果本周不是该区域的回收周，请告诉用户：“本周只需推【垃圾桶】。”
                - 必须明确告知用户具体的【清运星期几】（例如：每周四）。
                
                【重要执行指令】：
                1. 严禁客套话。
                2. 必须基于【Local Context from RAG】事实回答。
                3. 每条推荐必须包含具体的店名/地点、地址以及 Vibe 描述。
                4. 回答末尾必须包含 MATCH_SCORE_JSON 块。
                5. JSON 格式必须包含 "image_url" 字段 (如果 Context 中提供了图片链接，请务必填入)。
                
                请立即开始为您服务：
                """.trimIndent()
                AITopic.STOCK -> "\n当前模式：股票分析师。"
                AITopic.BUSINESS -> "\n当前模式：AI 与商务专家。专注于 AI 技术、硬件（RTX/工作站）以及夏洛特的商务社交。"
                AITopic.COLLEGE -> "\n当前模式：升学顾问。"
                AITopic.NONE -> "\n当前模式：通用助手。"
            }
        }
        
        return basePrompt + suffix
    }

    private suspend fun fetchContextFromPinecone(query: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Get Embedding from Gemini
            val embedding = getGeminiEmbedding(query, apiKey) ?: return@withContext null
            
            // 2. Query Pinecone
            val body = JSONObject()
            val vector = JSONArray()
            embedding.forEach { vector.put(it) }
            body.put("vector", vector)
            body.put("topK", 3)
            body.put("includeMetadata", true)
            
            val request = Request.Builder()
                .url(pineconeEndpoint)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Api-Key", pineconeKey)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                
                val json = JSONObject(response.body?.string() ?: "")
                val matches = json.optJSONArray("matches") ?: return@withContext null
                val contextStr = StringBuilder()
                for (i in 0 until matches.length()) {
                    val match = matches.getJSONObject(i)
                    val metadata = match.optJSONObject("metadata") ?: continue
                    val name = metadata.optString("name")
                    val desc = metadata.optString("description")
                    val img = metadata.optString("image_url")
                    
                    contextStr.append("- Name: $name\n")
                    contextStr.append("  Description: $desc\n")
                    if (img.isNotEmpty()) {
                        contextStr.append("  Image: $img\n")
                    }
                    contextStr.append("\n")
                }
                return@withContext contextStr.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun getGeminiEmbedding(text: String, apiKey: String): List<Double>? = withContext(Dispatchers.IO) {
        val url = "$geminiEmbedEndpoint?key=$apiKey"
        
        val content = JSONObject()
        val parts = JSONArray()
        parts.put(JSONObject().put("text", text))
        content.put("parts", parts)
        
        val body = JSONObject()
        body.put("model", "models/gemini-embedding-001")
        body.put("content", content)
        body.put("output_dimensionality", 3072)
        
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                
                val json = JSONObject(response.body?.string() ?: "")
                val embedding = json.optJSONObject("embedding") ?: return@withContext null
                val values = embedding.optJSONArray("values") ?: return@withContext null
                val result = mutableListOf<Double>()
                for (i in 0 until values.length()) {
                    result.add(values.getDouble(i))
                }
                return@withContext result
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
        body.put("max_tokens", 4000)

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
        tools.put(JSONObject().put("googleSearch", JSONObject()))

        val body = JSONObject()
        body.put("systemInstruction", systemInstruction)
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
