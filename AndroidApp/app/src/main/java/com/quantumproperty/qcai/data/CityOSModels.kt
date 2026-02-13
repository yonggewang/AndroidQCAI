package com.quantumproperty.qcai.data

import com.google.gson.annotations.SerializedName

data class DailyBriefResponse(
    @SerializedName("briefing_text") val briefingText: String,
    val weather: WeatherSummary?,
    @SerializedName("top_news") val topNews: List<NewsItem>,
    @SerializedName("generated_at") val generatedAt: String,
    @SerializedName("is_cached") val isCached: Boolean = false,
    @SerializedName("cache_age_seconds") val cacheAgeSeconds: Int = 0,
    @SerializedName("extra_data") val extraData: Map<String, Any>? = null
)

data class ChatResponse(
    val answer: String,
    @SerializedName("extra_data") val extraData: Map<String, Any>? = null
)

data class WeatherSummary(
    val temp: Double,
    val desc: String,
    val high: Double,
    val low: Double
)

data class NewsItem(
    val headline: String,
    val source: String,
    val url: String,
    val summary: String
)

data class SceneResponse(
    val vibe: String,
    val narrative: String,
    val events: List<SceneEvent>,
    val venues: List<SceneVenue>
)

data class SceneEvent(
    val name: String,
    val date: String,
    val location: String,
    val description: String,
    val url: String?
)

data class SceneVenue(
    val name: String,
    val address: String,
    val description: String,
    @SerializedName("vibe_match_score") val vibeMatchScore: Double
)
