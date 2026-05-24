package com.quantumproperty.qcai.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class CityOSService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    data class LocalEventItem(
        val name: String,
        val date: String,
        val venue: String,
        val url: String,
        val source: String
    )

    // MARK: - Fetch Daily Briefing Client-side
    suspend fun fetchDailyBrief(
        @Suppress("UNUSED_PARAMETER") city: String = "Charlotte",
        language: String = "en",
        @Suppress("UNUSED_PARAMETER") forceRefresh: Boolean = false
    ): DailyBriefResponse = withContext(Dispatchers.IO) {
        println("CityOSService: Starting client-side daily brief compilation...")

        // 1. Fetch Weather from OpenMeteo
        val weather = fetchWeatherLocally()

        // 2. Fetch News from local RSS feeds
        val news = fetchNewsLocally()

        // 3. Fetch Events from GitHub
        val events = fetchEventsLocally()

        // 4. Fetch Traffic from Waze
        val traffic = fetchTrafficLocally()

        // 5. Construct prompt for Gemini
        val isChinese = language == "zh" || language == "cn"
        val isSpanish = language == "es"

        var prompt = "You are the morning assistant for QCAI Charlotte. Generate a friendly, engaging morning briefing for Charlotte NC.\n"
        if (isChinese) {
            prompt += "IMPORTANT: You MUST respond in CHINESE (Simplified). Translation is required.\n"
        } else if (isSpanish) {
            prompt += "IMPORTANT: You MUST respond in SPANISH. Translation is required.\n"
        }

        prompt += """
        Here is the current data:
        WEATHER: Temp ${weather.temp.toInt()}°F, ${weather.desc}. High: ${weather.high.toInt()}°F, Low: ${weather.low.toInt()}°F.
        
        TOP NEWS:
        """.trimIndent()
        
        for (item in news) {
            prompt += "\n- ${item.headline} (Source: ${item.source})"
        }

        prompt += "\n\nUPCOMING EVENTS:\n"
        for (item in events) {
            prompt += "- ${item.name} at ${item.venue} (${item.date})\n"
        }

        prompt += "\nTRAFFIC INCIDENTS:\n"
        if (traffic.isEmpty()) {
            prompt += "- No major traffic incidents reported. Roads are clear.\n"
        } else {
            for (t in traffic) {
                prompt += "- $t\n"
            }
        }

        prompt += "\nBased on the above weather, news, events, and traffic, please write a concise, lively 3-paragraph summary briefing for residents."

        // 6. Generate summary using Gemini client-side
        val engineChoice = AIEngine.valueOf(PreferenceManager.selectedEngine)
        val langEnum = if (isChinese) AppLanguage.CHINESE else if (isSpanish) AppLanguage.SPANISH else AppLanguage.ENGLISH

        val briefingText = AIService().sendMessage(
            text = prompt,
            engine = engineChoice,
            topic = AITopic.LIFE,
            language = langEnum,
            image = null,
            realEstateAddress = null
        )

        // 7. Generate Proactive Insights locally
        val nbList = listOf("South End", "NoDa", "Uptown", "Ballantyne", "Plaza Midwood", "Dilworth")
        val empList = listOf("Bank of America", "Wells Fargo", "Atrium Health", "Duke Energy", "Honeywell", "Lowe's")
        val randomNb = nbList.random()
        val randomEmp = empList.random()

        val (_, realityData) = LocalCalculators.shared.analyzeNeighborhoodPerformance(randomNb)
        val (_, careerData) = LocalCalculators.shared.analyzeCareerPath("What is the vibe at $randomEmp?")

        val extraDataDict = mutableMapOf<String, Any>()
        if (realityData != null) {
            extraDataDict["reality_check"] = realityData
        }
        if (careerData != null) {
            extraDataDict["career_boost"] = careerData
        }

        // 8. Return response
        val sdfGenerated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val generatedAtStr = sdfGenerated.format(Date())

        DailyBriefResponse(
            briefingText = briefingText,
            weather = weather,
            topNews = news,
            generatedAt = generatedAtStr,
            isCached = false,
            cacheAgeSeconds = 0,
            extraData = extraDataDict
        )
    }

    // MARK: - Fetch AI News Articles
    suspend fun fetchAINewsArticles(language: String = "en"): List<AINewsArticle> = withContext(Dispatchers.IO) {
        val url = when (language) {
            "cn", "zh" -> "https://qcai-net.github.io/ainews/summary_CN.json"
            "es" -> "https://qcai-net.github.io/ainews/summary_ES.json"
            else -> "https://qcai-net.github.io/ainews/summary.json"
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val json = response.body?.string() ?: "[]"
                val type = object : com.google.gson.reflect.TypeToken<List<AINewsArticle>>() {}.type
                val articles: List<AINewsArticle> = gson.fromJson(json, type)
                return@withContext articles.sortedByDescending { it.impactScore }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // MARK: - Chat query router client-side
    suspend fun queryChat(
        question: String,
        engine: String? = null,
        topic: String? = null,
        userAddress: String? = null,
        language: String? = "en",
        customPrompt: String? = null
    ): ChatResponse = withContext(Dispatchers.IO) {
        val qLower = question.lowercase()

        // --- 0. ROUTE TRASH SCHEDULE / GARBAGE PICKUP ---
        val trashKws = listOf("trash day", "garbage day", "trash schedule", "garbage pick", "recycling", "waste collection", "rubbish", "yard waste", "bulk pick", "trash collection", "garbage collection", "trash pick", "垃圾", "回收", "废物", "扔垃圾", "basura", "reciclaje", "desechos", "residuos")
        val isTrashMatch = trashKws.any { qLower.contains(it) }
        
        if (isTrashMatch) {
            val addr = extractAddress(question) ?: userAddress
            if (!addr.isNullOrEmpty()) {
                val geo = geocodeAddress(addr)
                if (geo != null) {
                    val schedule = fetchTrashSchedule(geo.y, geo.x)
                    if (schedule != null) {
                        val answer = "🗑️ **Trash Schedule for ${geo.address}**\n\n$schedule\n💡 *Set out containers by 6 AM on collection day.*"
                        return@withContext ChatResponse(answer, null)
                    }
                }
                return@withContext ChatResponse("❌ Could not find the trash collection schedule for '$addr'. Please make sure it is a valid Charlotte address within city limits.", null)
            } else {
                return@withContext ChatResponse("🗑️ I can look up your trash schedule! Please set your address in \"My Neighborhood\" or specify it (e.g., 'What is the trash schedule for 600 E Trade St?').", null)
            }
        }
        
        // --- 0.1 ROUTE SCHOOL ZONE / CALENDAR ---
        val schoolKws = listOf("school zone", "school assignment", "cms calendar", "cms spring break", "cms holiday", "elementary school", "middle school", "high school", "which school", "what school", "zoned for", "my school", "学校", "学区", "小学", "初中", "高中", "中学", "上学", "校历", "放假", "escuela", "colegio", "distrito escolar", "calendario escolar")
        val isSchoolMatch = schoolKws.any { qLower.contains(it) }
        
        if (isSchoolMatch) {
            val calKws = listOf("calendar", "spring break", "holiday", "first day", "last day", "校历", "放假")
            val isCalQuery = calKws.any { qLower.contains(it) }
            if (isCalQuery) {
                return@withContext ChatResponse(getSchoolCalendarSummary(), null)
            }
            
            val addr = extractAddress(question) ?: userAddress
            if (!addr.isNullOrEmpty()) {
                val geo = geocodeAddress(addr)
                if (geo != null) {
                    val zones = fetchSchoolZones(geo.y, geo.x)
                    if (zones != null) {
                        val answer = """
                        🏫 **School Zones for ${geo.address}**
                        
                        • **Elementary**: ${zones["elementary"] ?: "Unknown"}
                        • **Middle**: ${zones["middle"] ?: "Unknown"}
                        • **High**: ${zones["high"] ?: "Unknown"}
                        """.trimIndent()
                        return@withContext ChatResponse(answer, null)
                    }
                }
                return@withContext ChatResponse("❌ Could not find CMS school zone assignment for '$addr'. Please verify the address.", null)
            } else {
                return@withContext ChatResponse("🏫 I can find your school zones! Please set your address in \"My Neighborhood\" or specify it (e.g., 'What school zone is 600 E Trade St in?').", null)
            }
        }

        // --- 1. ROUTE COST OF LIVING REALITY CHECK ---
        val colKeywords = listOf("electric", "water", "utility", "utilities", "internet", "google fiber", "electricity", "power", "energy", "duke")
        val colGeneralKws = listOf("cost of living", " col ", "生活费", "消费")
        val isColMatch = qLower.contains("$") || qLower.contains("bill") || qLower.contains("账单")
        val hasColKws = colKeywords.any { qLower.contains(it) }
        val hasGeneralCol = colGeneralKws.any { qLower.contains(it) }

        if ((isColMatch && hasColKws) || hasGeneralCol) {
            println("CityOSService: Routing query to local Cost of Living Calculator")
            val (ans, extra) = LocalCalculators.shared.analyzeColFairness(question)
            return@withContext ChatResponse(ans, extra)
        }

        // --- 2. ROUTE RENT ANALYSIS OR NEIGHBORHOOD PERFORMANCE ---
        val rentKws = listOf("rent", "apartment", "租房", "房租", "公寓", "小区")
        val performanceKws = listOf("performance", "growth", "investment", "appreciation", "升值")
        val hasRentKws = rentKws.any { qLower.contains(it) }
        val hasPerformanceKws = performanceKws.any { qLower.contains(it) }

        if (hasRentKws || hasPerformanceKws) {
            if (hasPerformanceKws) {
                println("CityOSService: Routing query to local Neighborhood Performance analyzer")
                val (ans, extra) = LocalCalculators.shared.analyzeNeighborhoodPerformance(question)
                return@withContext ChatResponse(ans, extra)
            } else {
                println("CityOSService: Routing query to local Rent Fairness analyzer")
                val (ans, extra) = LocalCalculators.shared.analyzeRentFairness(question)
                return@withContext ChatResponse(ans, extra)
            }
        }

        // --- 3. ROUTE CAREER BOOST ANALYZER ---
        val careerKws = listOf("career analyzer", "salary check", "negotiate salary", "atrium", "novant", "bank of america", "wells fargo", "honeywell", "duke energy", "lowes", "red ventures", "nascar", "software engineer", "data analyst", "plumber", "electrician", "teacher", "police officer")
        val hasCareerKws = careerKws.any { qLower.contains(it) }
        if (hasCareerKws) {
            println("CityOSService: Routing query to local Career Booster analyzer")
            val (ans, extra) = LocalCalculators.shared.analyzeCareerPath(question)
            return@withContext ChatResponse(ans, extra)
        }

        // --- 4. FALLBACK: GENERAL LLM CHAT VIA AISERVICE ---
        val engineChoice = if (engine?.contains("gpt", ignoreCase = true) == true) AIEngine.CHATGPT else AIEngine.GEMINI
        val langEnum = if (language == "zh" || language == "cn") AppLanguage.CHINESE else if (language == "es") AppLanguage.SPANISH else AppLanguage.ENGLISH
        val topicEnum = AITopic.entries.find { it.id == topic || it.name == topic } ?: AITopic.CLT_VIBE

        val answerText = AIService().sendMessage(
            text = question,
            engine = engineChoice,
            topic = topicEnum,
            language = langEnum,
            image = null,
            realEstateAddress = if (topicEnum == AITopic.REAL_ESTATE) question else userAddress,
            customPrompt = customPrompt
        )

        ChatResponse(answerText, null)
    }

    suspend fun registerDeviceToken(token: String, platform: String = "android") = withContext(Dispatchers.IO) {
        println("CityOSService: Registering device token $token for platform $platform in Supabase is pending setup.")
    }

    suspend fun fetchScene(vibe: String): SceneResponse = withContext(Dispatchers.IO) {
        val prompt = """
        Act as Charlotte scene analyzer. Generate a JSON detailing the current $vibe scene in Charlotte, NC.
        Required JSON format:
        {
          "vibe": "$vibe",
          "narrative": "A 3-sentence narrative summarizing the current $vibe scene vibe in Charlotte.",
          "events": [
            {"name": "Event Name", "date": "Date/Time", "location": "Venue Location", "description": "Brief description", "url": null}
          ],
          "venues": [
            {"name": "Venue Name", "address": "Address", "description": "Why it fits the vibe", "vibe_match_score": 95.0}
          ]
        }
        Return ONLY the raw JSON, no formatting, no markdown backticks.
        """.trimIndent()

        val engineChoice = AIEngine.valueOf(PreferenceManager.selectedEngine)
        val responseJson = AIService().sendMessage(
            text = prompt,
            engine = engineChoice,
            topic = AITopic.MISC,
            language = AppLanguage.ENGLISH,
            image = null,
            realEstateAddress = null
        )

        val cleanJson = responseJson
            .replace("```json", "")
            .replace("```", "")
            .trim()

        gson.fromJson(cleanJson, SceneResponse::class.java)
    }

    // MARK: - Local Aggregations helpers

    private suspend fun fetchWeatherLocally(): WeatherSummary = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=35.2271&longitude=-80.8431&daily=temperature_2m_max,temperature_2m_min,weathercode&current_weather=true&temperature_unit=fahrenheit&timezone=America/New_York"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val current = json.optJSONObject("current_weather")
                    val daily = json.optJSONObject("daily")
                    if (current != null && daily != null) {
                        val code = current.optInt("weathercode", 0)
                        val desc = when (code) {
                            1, 2, 3 -> "Partly Cloudy"
                            45, 48 -> "Foggy"
                            51, 53, 55, 61, 63, 65 -> "Rainy"
                            71, 73, 75 -> "Snowy"
                            in 95..99 -> "Thunderstorms"
                            else -> "Clear sky"
                        }
                        val temp = current.optDouble("temperature", 70.0)
                        val highs = daily.optJSONArray("temperature_2m_max")
                        val lows = daily.optJSONArray("temperature_2m_min")
                        val high = highs?.optDouble(0, temp) ?: temp
                        val low = lows?.optDouble(0, temp) ?: temp
                        return@withContext WeatherSummary(temp, desc, high, low)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        WeatherSummary(72.0, "Fair", 75.0, 60.0)
    }

    private suspend fun fetchNewsLocally(): List<NewsItem> = withContext(Dispatchers.IO) {
        val allNews = mutableListOf<NewsItem>()
        val feeds = listOf(
            Pair("WCNC Charlotte", "https://www.wcnc.com/feeds/syndication/rss/news/local"),
            Pair("WCCB Charlotte", "https://www.wccbcharlotte.com/feed/"),
            Pair("Google News (Charlotte)", "https://news.google.com/rss/search?q=Charlotte+NC&hl=en-US&gl=US&ceid=US:en")
        )

        for ((source, urlString) in feeds) {
            try {
                // Use Jsoup to fetch XML and parse elements
                val doc = Jsoup.connect(urlString)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .parser(org.jsoup.parser.Parser.xmlParser())
                    .get()

                val items = doc.select("item")
                for (item in items.take(3)) {
                    val title = item.select("title").text()
                        .replace("<![CDATA[", "")
                        .replace("]]>", "")
                        .trim()
                    val link = item.select("link").text().trim()
                    if (title.isNotEmpty() && link.isNotEmpty()) {
                        allNews.add(NewsItem(title, source, link, ""))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        allNews.take(5)
    }

    private suspend fun fetchEventsLocally(): List<LocalEventItem> = withContext(Dispatchers.IO) {
        val events = mutableListOf<LocalEventItem>()
        try {
            val doc = Jsoup.connect("https://yonggewang.github.io/events/index.html").get()
            val listItems = doc.select("li")
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Date())

            for (item in listItems) {
                val rawLi = item.html()
                val liText = item.text()

                // Find dates like YYYY-MM-DD
                val datePattern = """(\d{4}-\d{2}-\d{2})""".toRegex()
                val dateMatch = datePattern.find(liText) ?: continue
                val eventDate = dateMatch.groupValues[1]

                if (eventDate < todayStr) continue

                val parts = liText.split(eventDate)
                val namePart = parts.firstOrNull()?.trim() ?: ""

                var venue = "Charlotte"
                val remaining = if (parts.size > 1) parts[1] else ""
                val venuePattern = """·\s*(.+?)\s*Source:""".toRegex()
                val venueMatch = venuePattern.find(remaining)
                if (venueMatch != null) {
                    venue = venueMatch.groupValues[1].trim()
                }

                var url = ""
                val hrefPattern = """href="(.*?)"""".toRegex()
                val hrefMatch = hrefPattern.find(rawLi)
                if (hrefMatch != null) {
                    url = hrefMatch.groupValues[1].trim()
                }

                if (url.isEmpty() || url.contains("cyberpandaapp.com")) continue

                events.add(LocalEventItem(namePart, eventDate, venue, url, "Ticketmaster"))
                if (events.size >= 5) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        events
    }

    private suspend fun fetchTrafficLocally(): List<String> = withContext(Dispatchers.IO) {
        val alerts = mutableListOf<String>()
        val url = "https://www.waze.com/live-map/api/georss?top=35.45&bottom=35.05&left=-81.10&right=-80.60&env=na&types=alerts,traffic"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseJson = JSONObject(response.body?.string() ?: "{}")
                    val wazeAlerts = responseJson.optJSONArray("alerts") ?: return@withContext emptyList()
                    for (i in 0 until wazeAlerts.length()) {
                        val alert = wazeAlerts.getJSONObject(i)
                        val type = alert.optString("type")
                        val subtype = alert.optString("subtype")
                        val street = alert.optString("street", "Unknown road")

                        if (type == "ACCIDENT") {
                            alerts.add("🚗 Accident on $street")
                        } else if (type == "JAM") {
                            alerts.add("🚦 Heavy traffic on $street")
                        } else if (type == "ROAD_CLOSED") {
                            alerts.add("🚧 Road closed: $street")
                        } else if (subtype.contains("CONSTRUCTION")) {
                            alerts.add("🔨 Construction on $street")
                        }

                        if (alerts.size >= 5) break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        alerts
    }

    // Local Data System
    data class Neighborhood(val name: String, val tag: String, val icon: String, val color: String, val url: String)
    data class CityService(val nameEn: String, val nameZh: String, val icon: String, val color: String, val url: String)

    fun getNeighborhoods(): List<Neighborhood> {
        return listOf(
            Neighborhood("Uptown", "The Hub", "Business", "#00B0FF", "https://www.charlotteobserver.com/news/local/article308393505.html"),
            Neighborhood("NoDa", "Arts District", "Brush", "#FF4081", "https://www.charlottesgotalot.com/neighborhoods/noda"),
            Neighborhood("South End", "Active Living", "DirectionsRun", "#00E676", "https://www.charlotteobserver.com/news/local/article306309761.html"),
            Neighborhood("Plaza Midwood", "Eclectic", "Restaurant", "#FF9100", "https://www.charlottesgotalot.com/neighborhoods/plaza-midwood")
        )
    }

    fun getCityServices(): List<CityService> {
        return listOf(
            CityService("311 Request", "311 请求", "Phone", "#4CAF50", "https://www.charlottenc.gov/Help311"),
            CityService("Trash Schedule", "垃圾回收", "Delete", "#8D6E63", "https://www.charlottenc.gov/Services/Trash-and-Recycling"),
            CityService("Transit", "公共交通", "DirectionsBus", "#2196F3", "https://www.charlottenc.gov/CATS"),
            CityService("Permits", "许可证", "Description", "#FF9800", "https://aca-prod.accela.com/CHARLOTTE")
        )
    }

    // MARK: - Local Geocoding and City Services Helpers
    
    private data class GeocodeResult(val x: Double, val y: Double, val address: String)
    
    private suspend fun geocodeAddress(address: String): GeocodeResult? = withContext(Dispatchers.IO) {
        var cleanAddress = address.trim()
        if (!cleanAddress.lowercase().contains("charlotte")) {
            cleanAddress += ", Charlotte, NC"
        }
        val escaped = URLEncoder.encode(cleanAddress, "UTF-8")
        val url = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates?SingleLine=$escaped&f=json&outSR=4326&maxLocations=1"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val best = candidates.getJSONObject(0)
                        val loc = best.optJSONObject("location")
                        if (loc != null) {
                            val x = loc.optDouble("x", 0.0)
                            val y = loc.optDouble("y", 0.0)
                            val formattedAddress = best.optString("address", cleanAddress)
                            return@withContext GeocodeResult(x, y, formattedAddress)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
    
    private suspend fun fetchTrashSchedule(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        val url = "https://services.arcgis.com/9Nl857LBlQVyzq54/arcgis/rest/services/Solid_Waste_Collection/FeatureServer/0/query?geometry=$lon,$lat&geometryType=esriGeometryPoint&inSR=4326&spatialRel=esriSpatialRelIntersects&outFields=WORK_DAY,ROUTE_TYPE,ROUTE_NOTE&f=json"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val features = json.optJSONArray("features") ?: return@withContext null
                    
                    var garbageDay: String? = null
                    var recyclingDay: String? = null
                    var recyclingWeek: String? = null
                    var yardWasteDay: String? = null
                    
                    val dayMap = mapOf(
                        "MON" to "Monday", "TUE" to "Tuesday", "WED" to "Wednesday",
                        "THU" to "Thursday", "FRI" to "Friday", "SAT" to "Saturday", "SUN" to "Sunday"
                    )
                    
                    for (i in 0 until features.length()) {
                        val f = features.getJSONObject(i)
                        val attrs = f.optJSONObject("attributes") ?: continue
                        val routeType = attrs.optString("ROUTE_TYPE", "")
                        val workDay = attrs.optString("WORK_DAY", "")
                        val note = attrs.optString("ROUTE_NOTE", "")
                        val fullDay = dayMap[workDay] ?: workDay
                        
                        if (routeType == "GARB") {
                            garbageDay = fullDay
                        } else if (routeType == "RECY") {
                            recyclingDay = fullDay
                            if (note.uppercase().contains("GREEN")) {
                                recyclingWeek = "GREEN"
                            } else if (note.uppercase().contains("ORANGE")) {
                                recyclingWeek = "ORANGE"
                            }
                        } else if (routeType == "YARD") {
                            yardWasteDay = fullDay
                        }
                    }
                    
                    if (garbageDay == null && recyclingDay == null) return@withContext null
                    
                    val sb = StringBuilder()
                    if (garbageDay != null) {
                        sb.append("• **Garbage**: $garbageDay\n")
                    }
                    if (recyclingDay != null) {
                        val weekInfo = if (recyclingWeek != null) " ($recyclingWeek week)" else ""
                        sb.append("• **Recycling**: $recyclingDay$weekInfo\n")
                    }
                    if (yardWasteDay != null) {
                        sb.append("• **Yard Waste**: $yardWasteDay\n")
                    }
                    return@withContext sb.toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
    
    private suspend fun fetchSchoolZones(lat: Double, lon: Double): Map<String, String>? = withContext(Dispatchers.IO) {
        val levels = listOf("elementary", "middle", "high")
        val serviceNames = mapOf(
            "elementary" to "CMSElementarySchoolDistricts",
            "middle" to "CMSMiddleSchoolDistricts",
            "high" to "CMSHighSchoolDistricts"
        )
        val zones = mutableMapOf<String, String>()
        
        for (level in levels) {
            val serviceName = serviceNames[level] ?: continue
            val url = "https://meckgis.mecklenburgcountync.gov/server/rest/services/$serviceName/MapServer/0/query?geometry=$lon,$lat&geometryType=esriGeometryPoint&spatialRel=esriSpatialRelWithin&inSR=4326&outFields=*&returnGeometry=false&f=json"
            val request = Request.Builder().url(url).get().build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val features = json.optJSONArray("features")
                        if (features != null && features.length() > 0) {
                            val attr = features.getJSONObject(0).optJSONObject("attributes")
                            if (attr != null) {
                                val schoolName = attr.optString("${level.substring(0, 4)}_name")
                                    .takeIf { it.isNotEmpty() }
                                    ?: attr.optString("${level}_name")
                                    .takeIf { it.isNotEmpty() }
                                    ?: attr.optString("name", "Unknown")
                                zones[level] = schoolName
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext if (zones.isNotEmpty()) zones else null
    }
    
    private fun getSchoolCalendarSummary(): String {
        return """
        📅 **CMS 2025-2026 Calendar Highlights:**
        - Start of School: August 25, 2025
        - Last day of school: June 10, 2026
        - Thanksgiving Break: Nov 26 - Nov 28, 2025
        - Winter Break: Dec 22, 2025 - Jan 2, 2026
        - Spring Break: April 6 - April 10, 2026
        - Memorial Day: May 25, 2026
        """.trimIndent()
    }
    
    private fun extractAddress(text: String): String? {
        val pattern = "\\b\\d+\\s+[A-Za-z0-9\\s\\.]+\\b(street|st|road|rd|avenue|ave|drive|dr|court|ct|trail|trl|lane|ln|way|boulevard|blvd)\\b"
        val regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = regex.matcher(text)
        if (matcher.find()) {
            return matcher.group().trim()
        }
        return null
    }

    companion object {
        val instance = CityOSService()
    }
}
