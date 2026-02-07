package com.quantumproperty.qcai.data

import com.google.gson.annotations.SerializedName

data class DailyBriefResponse(
    @SerializedName("briefing_text") val briefingText: String,
    val weather: WeatherSummary?,
    @SerializedName("top_news") val topNews: List<NewsItem>,
    @SerializedName("generated_at") val generatedAt: String
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
