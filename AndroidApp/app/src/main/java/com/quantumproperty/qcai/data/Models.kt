package com.quantumproperty.qcai.data

enum class AppLanguage {
    CHINESE, ENGLISH, SPANISH
}

enum class AIEngine(val rawValue: String) {
    GEMINI("Gemini"),
    CHATGPT("ChatGPT")
}


enum class AITopic(val chineseName: String, val englishName: String, val id: String) {
    WORLD_NEWS("世界头条", "World News", "topNews"),
    FINANCE_NEWS("财经头条", "Finance News", "financeNews"),
    AI_ANALYSIS("AI深度分析", "AI Analysis", "aiAnalysis"),
    FOOD("夏村美食", "Charlotte Food", "food"),
    DIY("房屋维护", "DIY Help", "diy"),
    REAL_ESTATE("房产助手", "Real Estate Assistant", "realEstate"),
    LIFE("本地生活", "Local Life", "life"),
    MISC("杂项", "Misc", "misc"),
    CLT_VIBE("夏洛特 Vibe", "CLT Vibe", "cltVibe"),
    BUSINESS("商务交流", "Business", "business"),
    STOCK("AI 股市透视", "AI Stock Analysis", "stock"),
    COLLEGE("升学助手", "College Prep", "college"),
    NONE("无", "None", "none")
}

data class Recommendation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val score: Int,
    val reason: String,
    val price: String? = null,
    val rating: String? = null,
    val imageUrl: String? = null
)


data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isHidden: Boolean = false,
    val extraData: Map<String, Any>? = null
)

data class HotToolItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val icon: String = "",
    val chineseName: String,
    val englishName: String,
    val spanishName: String = "",
    val url: String
)

data class TopMenuItem(
    val icon: String,
    val englishName: String,
    val chineseName: String,
    val chineseUrl: String,
    val englishUrl: String,
    val spanishUrl: String = "",
    val topic: AITopic
)

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val vipLevel: Int = 1 // 0: Guest, 1: Registered, 99: Root
)
