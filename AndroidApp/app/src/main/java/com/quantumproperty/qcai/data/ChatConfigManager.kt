package com.quantumproperty.qcai.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class LocalizedString(
    val en: String,
    val zh: String
) {
    fun localized(isEnglish: Boolean): String = if (isEnglish) en else zh
}

data class ChatSuggestion(
    val id: String,
    val emoji: String,
    val label: LocalizedString,
    val prompt: LocalizedString,
    val category: String,
    @SerializedName("is_featured") val isFeatured: Boolean
)

data class ChatConfig(
    val version: Int,
    @SerializedName("last_updated") val lastUpdated: String,
    @SerializedName("welcome_message") val welcomeMessage: LocalizedString?,
    val suggestions: List<ChatSuggestion>,
    @SerializedName("pro_tips") val proTips: List<LocalizedString>?
)

class ChatConfigManager private constructor() {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val configURL = "https://qcai-net.github.io/qcai/chat_config.json"

    private val _suggestions = MutableStateFlow<List<ChatSuggestion>>(emptyList())
    val suggestions: StateFlow<List<ChatSuggestion>> = _suggestions

    private val _featuredSuggestions = MutableStateFlow<List<ChatSuggestion>>(emptyList())
    val featuredSuggestions: StateFlow<List<ChatSuggestion>> = _featuredSuggestions

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    private val fallbackSuggestions = listOf(
        ChatSuggestion(
            id = "trash",
            emoji = "🗑️",
            label = LocalizedString("Trash Day", "垃圾日"),
            prompt = LocalizedString("When is trash day at [your address]?", "我家地址的垃圾收集日是哪天？"),
            category = "city_services",
            isFeatured = true
        ),
        ChatSuggestion(
            id = "coffee",
            emoji = "☕",
            label = LocalizedString("Coffee", "咖啡"),
            prompt = LocalizedString("Where can I get good coffee in Charlotte?", "夏洛特哪里有好喝的咖啡？"),
            category = "vibe",
            isFeatured = true
        ),
        ChatSuggestion(
            id = "school_zone",
            emoji = "🏫",
            label = LocalizedString("School Zone", "学区查询"),
            prompt = LocalizedString("What school am I zoned for at [your address]?", "我家地址所属的学校是什么？"),
            category = "education",
            isFeatured = true
        ),
        ChatSuggestion(
            id = "hidden_gems",
            emoji = "✨",
            label = LocalizedString("Hidden Gems", "本地秘境"),
            prompt = LocalizedString("Show me some hidden gems in Charlotte.", "带我看看夏洛特的本地秘境。"),
            category = "lifestyle",
            isFeatured = true
        )
    )

    suspend fun fetchConfig() = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(configURL).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    loadFallback()
                    return@withContext
                }
                val body = response.body?.string() ?: return@withContext loadFallback()
                val config = gson.fromJson(body, ChatConfig::class.java)
                
                _suggestions.value = config.suggestions
                _featuredSuggestions.value = config.suggestions.filter { it.isFeatured }
                _isLoaded.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadFallback()
        }
    }

    private fun loadFallback() {
        _suggestions.value = fallbackSuggestions
        _featuredSuggestions.value = fallbackSuggestions.filter { it.isFeatured }
        _isLoaded.value = true
    }

    companion object {
        val instance by lazy { ChatConfigManager() }
    }
}
