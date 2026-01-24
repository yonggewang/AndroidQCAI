package com.example.cltdiy.data

enum class AppLanguage {
    CHINESE, ENGLISH
}

enum class AIEngine(val rawValue: String) {
    GEMINI("Gemini"),
    CHATGPT("ChatGPT")
}

enum class AITopic(val chineseName: String, val englishName: String, val id: String) {
    AI_ANALYSIS("AI深度分析", "AI Analysis", "AI深度分析"),
    DIY("房屋维护", "DIY Help", "房屋维护"),
    FOOD("夏村美食", "Charlotte Food", "food"),
    REAL_ESTATE("房产助手", "Real Estate Assistant", "财产分析"),
    WORLD_NEWS("世界头条", "World News", "世界头条"),
    LIFE("本地生活", "Local Life", "本地生活"),
    FINANCE_NEWS("财经头条", "Finance News", "财经头条"),
    FORD("福特信息", "Ford", "ford"),
    MISC("杂项", "Misc", "杂项")
}


data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)

data class HotToolItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chineseName: String,
    val englishName: String,
    val url: String
)

data class TopMenuItem(
    val icon: String,
    val englishName: String,
    val chineseName: String,
    val chineseUrl: String,
    val englishUrl: String
)

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val vipLevel: Int = 1 // 0: Guest, 1: Registered, 99: Root
)
