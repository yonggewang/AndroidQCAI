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
    private val geminiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent"

    suspend fun sendMessage(text: String, engine: AIEngine, topic: AITopic, image: Bitmap? = null): String {
        val systemPrompt = generateSystemPrompt(topic)
        
        val openAIKey = PreferenceManager.openAIKey
        val geminiKey = PreferenceManager.geminiKey

        return when (engine) {
            AIEngine.CHATGPT -> {
                if (openAIKey.isEmpty()) throw IOException("Missing OpenAI API Key")
                sendToChatGPT(openAIKey, systemPrompt, text, image)
            }
            AIEngine.GEMINI -> {
                if (geminiKey.isEmpty()) throw IOException("Missing Gemini API Key")
                sendToGemini(geminiKey, systemPrompt, text, image)
            }
        }
    }

    private fun generateSystemPrompt(topic: AITopic): String {
        val basePrompt = """
        你是“夏村华人AI大全”的智能助手，主要服务于美国北卡罗来纳州夏洛特（Charlotte, NC）的华人社区。
        你的名字是“夏洛特智能帮手”。你主要搜集 Charlotte, NC 的华人居民感兴趣的信息。
        请用中文回答问题。请结合夏洛特本地的实际环境，为大家提供面向夏洛特华人的本地信息服务。
        You have access to Google Search. You must use it to verify facts and provide up-to-date information for every query.
        """.trimIndent()

        val suffix = when (topic) {
            AITopic.FINANCE_NEWS -> "\n当前模式：投资理财专家。请专注于投资，股票，房地产，金融市场分析，提供专业的财经建议。回答要严谨、数据详实。"
            AITopic.DIY -> "\n当前模式：夏村自己动手专家。重点帮助用户获取生活服务指南、办事流程说明（如房屋维修、庭院打理、车辆保养等）。特别被设计为一个自己动手智能助手，让在夏洛特的生活变得更加方便、高效、安心。"
            AITopic.FOOD -> "\n当前模式：夏村中餐向导。请专注于推荐夏洛特及其周边的中餐馆、华人超市和美食资讯。请尽量提供具体的餐馆名称、特色菜推荐和本地评价. "
            AITopic.REAL_ESTATE -> """
你是一个专注于【北卡罗来纳州夏洛特（Charlotte, NC）及周边地区】的房产与房地产投资智能顾问。

你的回答必须以夏洛特都市圈（包括但不限于 Uptown、South End、Ballantyne、NoDa、Plaza Midwood、University City、Huntersville、Matthews、Pineville、Fort Mill 等周边地区）为核心背景。

你主要提供以下方面的信息与建议：
1. 房产市场情况（房价水平、趋势、供需、租金、回报率）
2. 房地产投资方向（自住房 vs 投资房、长租 / 短租、风险与机会）
3. 房东注意事项（租客筛选、租约、维修、保险、法律合规、纠纷风险）
4. 学区与社区分析（公立/私立学校、学区对房价的影响）
5. 社区安全与居住环境（治安、区域差异、生活便利性）
6. 房屋管理与出租相关资源（物业管理、常用平台、实务建议）
7. 北卡及夏洛特相关的房地产法规、政策和常见风险（不提供正式法律意见）

回答时应：
- 优先使用夏洛特及周边的实际区域、社区和典型案例
- 明确说明哪些结论是基于市场普遍经验，哪些是可能随时间变化的判断
- 在不确定或数据可能过时的情况下，主动提醒用户核实最新信息
- 以清晰、结构化、对普通用户友好的方式输出内容
- 介绍自己时，你可以说：当前模式：夏村房产专家。

如果用户的问题明显超出夏洛特及周边房地产领域，请主动将回答拉回到该地区的房产、投资、居住或安全相关角度。
            """.trimIndent()
            AITopic.WORLD_NEWS -> "\n当前模式：世界头条。请专注于世界各地的重大新闻，并结合夏洛特华人的视角进行解读。回答要严谨、数据详实。"
            AITopic.LIFE -> "\n当前模式：北卡生活点滴。请专注于与北卡和夏洛特有关的生活点滴，提供生活方面的建议。回答要严谨、数据详实。"
            AITopic.AI_ANALYSIS -> "\n当前模式：AI深度分析。请利用你的AI能力，对用户提供的信息进行深度分析和解读。回答要逻辑清晰、深度见解。"
            AITopic.MISC -> "\n当前模式：杂项。请协助用户处理各种琐碎的事务或提供通用的建议。回答要灵活、周全。"
            AITopic.FORD -> "\n当前模式：福特专家。请专注于与福特汽车或福特相关的信息。回答要专业、准确。"
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

        val systemInstruction = JSONObject()
        systemInstruction.put("role", "system")
        systemInstruction.put("parts", JSONArray().put(JSONObject().put("text", prompt)))

        val contents = JSONArray()
        val userContent = JSONObject()
        userContent.put("role", "user")
        userContent.put("parts", parts)
        contents.put(userContent)

        val generationConfig = JSONObject()
        generationConfig.put("temperature", 1.0)
        val thinkingConfig = JSONObject()
        thinkingConfig.put("thinking_level", "HIGH")
        generationConfig.put("thinking_config", thinkingConfig)

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

    private fun encodeImage(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
