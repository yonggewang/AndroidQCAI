package com.quantumproperty.qcai.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

@Serializable
data class COLBenchmarks(
    val utilities: Map<String, Map<String, Double>>,
    val housing: Map<String, Double>,
    val general: Map<String, Double>,
    val trends: COLTrends
)

@Serializable
data class COLTrends(
    val annual_increase: String,
    val market_sentiment: String,
    val description: String
)

@Serializable
data class CareerData(
    val salaries: Map<String, JobSalary>,
    val employers: Map<String, String>
)

@Serializable
data class JobSalary(
    val entry: Double,
    val mid: Double,
    val senior: Double,
    val trend: String
)

class LocalCalculators private constructor() {

    private var colBenchmarks: COLBenchmarks? = null
    private var careerData: CareerData? = null

    // Neighborhood averages data (Ported from real_estate_service.py)
    private val neighborhoodData = mapOf(
        "south end" to mapOf("1br" to 1850.0, "2br" to 2400.0, "vibe" to "👑 Charlotte's most popular hub. Professional, vibrant, and extremely walkable via the Rail Trail."),
        "noda" to mapOf("1br" to 1650.0, "2br" to 2280.0, "vibe" to "🎨 Arts district with a soul. Quirky, transit-oriented, and home to great breweries."),
        "uptown" to mapOf("1br" to 1850.0, "2br" to 2450.0, "vibe" to "🏙️ The corporate heart. Sports, luxury high-rises, and close to everything business."),
        "ballantyne" to mapOf("1br" to 1650.0, "2br" to 1950.0, "vibe" to "⛳ Suburban luxury. Clean, safe, great schools, and booming with new development."),
        "plaza midwood" to mapOf("1br" to 1500.0, "2br" to 1950.0, "vibe" to "🎸 Eclectic and historic. Diverse nightlife, established 'cool', and very walkable."),
        "dilworth" to mapOf("1br" to 1600.0, "2br" to 2100.0, "vibe" to "🌳 Leafy and historic. Medical hub close-by with charming bungalows & parks."),
        "elizabeth" to mapOf("1br" to 1550.0, "2br" to 2000.0, "vibe" to "🏥 Historic charm. Home to hospitals, great bakeries, and a cozy community feel."),
        "university city" to mapOf("1br" to 1300.0, "2br" to 1650.0, "vibe" to "🎓 UNCC area. Student-heavy, affordable, and well-connected by Light Rail."),
        "steele creek" to mapOf("1br" to 1450.0, "2br" to 1850.0, "vibe" to "✈️ Near the airport. Growing fast with new shops and lake access nearby."),
        "matthews" to mapOf("1br" to 1400.0, "2br" to 1800.0, "vibe" to "🌳 Charming small-town feel with a historic downtown and great family atmosphere."),
        "huntersville" to mapOf("1br" to 1500.0, "2br" to 1900.0, "vibe" to "🛶 Lake Norman adjacent. Modern suburbs, parks, and high-end shopping at Birkdale Village."),
        "mint hill" to mapOf("1br" to 1350.0, "2br" to 1750.0, "vibe" to "🚜 Quiet and rural but growing. Space, privacy, and a tight-knit community.")
    )

    private val marketTrends = mapOf(
        "south end" to mapOf("growth" to "+8.2%", "outlook" to "Strong", "reason" to "Continued expansion of medical hub and corporate HQ relocations."),
        "noda" to mapOf("growth" to "+6.5%", "outlook" to "Stable", "reason" to "High demand for historic renovations and proximity to Light Rail."),
        "uptown" to mapOf("growth" to "+4.1%", "outlook" to "Moderate", "reason" to "Supply reaching equilibrium with several new luxury high-rises."),
        "ballantyne" to mapOf("growth" to "+9.0%", "outlook" to "Aggressive", "reason" to "Ballantyne Reimagined completion creating a 'second downtown'."),
        "plaza midwood" to mapOf("growth" to "+7.2%", "outlook" to "Strong", "reason" to "Heavy redevelopment of central corridors and high foot traffic."),
        "matthews" to mapOf("growth" to "+5.8%", "outlook" to "Stable", "reason" to "Consistent demand for family housing and downtown revitalization."),
        "huntersville" to mapOf("growth" to "+7.5%", "outlook" to "Strong", "reason" to "Rapid commercial development and Lake Norman lifestyle appeal.")
    )

    private val zipToNeighborhood = mapOf(
        "28203" to "south end",
        "28202" to "uptown",
        "28277" to "ballantyne",
        "28205" to "noda",
        "28105" to "matthews",
        "28226" to "ballantyne",
        "28078" to "huntersville"
    )

    fun init(context: Context) {
        colBenchmarks = loadJSON(context, "col_benchmarks.json")
        careerData = loadJSON(context, "career_data.json")
    }

    private inline fun <reified T> loadJSON(context: Context, filename: String): T? {
        return try {
            val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<T>(jsonString)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractPrice(text: String): Double? {
        val pattern = """\$?\s*((\d+(?:,\d{3})*)(?:\.\d{2})?)""".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return null
        val priceStr = match.groupValues[1].replace(",", "")
        return priceStr.toDoubleOrNull()
    }

    // MARK: - Cost of Living Fairness Analysis
    fun analyzeColFairness(question: String): Pair<String, Map<String, Any>?> {
        val text = question.lowercase()
        val userPrice = extractPrice(text) ?: return getColGeneralSummary()

        // 1. Determine Category
        var category = "electric"
        if (text.contains("water") || text.contains("aqua") || text.contains("h2o") || text.contains("liquid")) {
            category = "water"
        } else if (text.contains("internet") || text.contains("wifi") || text.contains("fiber") || text.contains("spectrum") || text.contains("broadband")) {
            category = "internet"
        } else if (text.contains("trash") || text.contains("waste") || text.contains("garbage") || text.contains("recycling")) {
            category = "trash"
        } else if (text.contains("total") || text.contains("all") || text.contains("bills") || text.contains("everything") || text.contains("combined")) {
            category = "total_avg"
        } else if (text.contains("electric") || text.contains("power") || text.contains("energy") || text.contains("duke")) {
            category = "electric"
        }

        // 2. Determine Size
        var sizeKey = "apartment_1br"
        if (text.contains("2br") || text.contains("2 bedroom") || text.contains("two bedroom") || text.contains("2-bedroom")) {
            sizeKey = "apartment_2br"
        } else if (text.contains("3br") || text.contains("3 bedroom") || text.contains("three bedroom") || text.contains("3-bedroom") || text.contains("house") || text.contains("home")) {
            sizeKey = "house_3br"
        } else if (text.contains("1br") || text.contains("1 bedroom") || text.contains("one bedroom") || text.contains("1-bedroom") || text.contains("studio")) {
            sizeKey = "apartment_1br"
        }

        // 3. Compare with benchmarks
        val benchmarks = colBenchmarks
        val sizeData = benchmarks?.utilities?.get(sizeKey)
        val avgPrice = sizeData?.get(category)
            ?: return Pair("I see the price of \$${userPrice.toInt()}, but I'm not sure which utility (electric, water, internet) or home size (1BR, 2BR, House) you're referring to. (e.g. 'Is \$150 high for electric in a 1BR?')", null)

        val diffPct = (userPrice - avgPrice) / avgPrice
        var verdict = "Normal"
        if (diffPct > 0.3) {
            verdict = "Significantly High"
        } else if (diffPct > 0.1) {
            verdict = "Slightly High"
        } else if (diffPct < -0.1) {
            verdict = "Excellent Value"
        }

        var summary = "📊 **COL Reality Check: ${category.replaceFirstChar { it.uppercase() }} (${sizeKey.replace("_", " ").split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }})**\n\n"
        summary += "Current CLT Avg: **\$${avgPrice.toInt()}** | Your Cost: **\$${String.format("%.2f", userPrice)}**\n"
        summary += "Verdict: **$verdict**\n\n"

        if (verdict == "Significantly High") {
            summary += "💡 **Pro Tip:** Your $category bill is about ${(diffPct * 100).toInt()}% above the Charlotte average. Check for insulation leaks, older appliances, or contact Duke Energy/CLT Water for a free usage audit."
        } else if (verdict == "Normal") {
            summary += "✅ This aligns well with current market data for the Queen City."
        } else {
            summary += "✨ You're getting a great deal compared to the local averages!"
        }

        val extra = mapOf(
            "type" to "col_analysis",
            "category" to category,
            "user_price" to userPrice,
            "avg_price" to avgPrice,
            "diff_pct" to (diffPct * 100).toInt(),
            "verdict" to verdict
        )

        return Pair(summary, extra)
    }

    private fun getColGeneralSummary(): Pair<String, Map<String, Any>?> {
        val benchmarks = colBenchmarks ?: return Pair("Cost of living data currently unavailable.", null)

        var summary = "🏘️ **Charlotte Cost of Living (COL) Overview**\n\n"
        summary += "**Market Sentiment:** ${benchmarks.trends.market_sentiment}\n"
        summary += "**Annual Trend:** ${benchmarks.trends.annual_increase}\n\n"
        summary += "**Quick Benchmarks:**\n"
        benchmarks.general["groceries_single"]?.let { groc ->
            summary += "• Groceries (Single): **\$${groc.toInt()}**/mo\n"
        }
        benchmarks.general["transit_pass"]?.let { transit ->
            summary += "• Transit Pass: **\$${transit.toInt()}**/mo\n\n"
        }
        summary += "_${benchmarks.trends.description}_"

        val extra = mapOf(
            "type" to "col_summary",
            "sentiment" to benchmarks.trends.market_sentiment,
            "growth" to benchmarks.trends.annual_increase
        )

        return Pair(summary, extra)
    }

    // MARK: - Rent Fairness Analysis
    fun analyzeRentFairness(question: String): Pair<String, Map<String, Any>?> {
        val text = question.lowercase()
        val userPrice = extractPrice(text) ?: return Pair("I can help with that! Just tell me the rent amount and the neighborhood (e.g. 'Is \$1800 fair in NoDa?').", null)

        // 1. Detect Neighborhood
        var foundNb: String? = null
        for (nb in neighborhoodData.keys) {
            if (text.contains(nb)) {
                foundNb = nb
                break
            }
        }

        if (foundNb == null) {
            return Pair("I see the price of \$${userPrice.toInt()}, but which neighborhood are you looking at? I'm currently an expert on South End, NoDa, Uptown, Plaza Midwood, and Ballantyne.", null)
        }

        val match = foundNb

        // 2. Detect Bedroom Count (Default to 1BR)
        var brCount = "1br"
        if (text.contains("2br") || text.contains("2 bedroom") || text.contains("two bedroom")) {
            brCount = "2br"
        }

        // 3. Lookup Rent Average
        val data = neighborhoodData[match]
        val avg = data?.get(brCount) as? Double ?: return Pair("I see the price of \$${userPrice.toInt()}, but I don't have enough data for ${match.replaceFirstChar { it.uppercase() }} yet.", null)

        val vibe = data["vibe"] as? String ?: "No specific local vibe notes yet."
        val diffPct = (userPrice - avg) / avg

        var summary = "🏠 **Rent Analysis: ${match.split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }} (${brCount.uppercase()})**\n\n"
        summary += "Current Avg: **\$${avg.toInt()}** | Your Price: **\$${userPrice.toInt()}**\n\n"

        var verdictKey = "fair"
        var verdict = "✅ **Verdict: Market Fair**"
        var reason = "This price falls right in the sweet spot for ${match.replaceFirstChar { it.uppercase() }}. It aligns perfectly with current market data."

        if (diffPct < -0.12) {
            verdict = "💎 **Verdict: A Rare Find!**"
            verdictKey = "great"
            reason = "At \$${userPrice.toInt()}, this is significantly below the \$${avg.toInt()} market average for ${match.replaceFirstChar { it.uppercase() }}. If the unit is in good condition, this is an incredible value for the area."
        } else if (diffPct < 0.08) {
            // Already initialized to market fair
        } else if (diffPct < 0.20) {
            verdict = "📈 **Verdict: Slightly Premium**"
            verdictKey = "premium"
            reason = "This is about ${(diffPct * 100).toInt()}% above average. This is common for high-floor units, luxury amenities (pool/gym), or proximity to the Light Rail."
        } else {
            verdict = "🚩 **Verdict: High Premium**"
            verdictKey = "high"
            reason = "You're paying a substantial premium over the \$${avg.toInt()} average. Unless this is a top-tier penthouse or includes all utilities, you might want to cross-shop other buildings nearby."
        }

        summary += "$verdict\n\n"
        summary += "**Context:** $reason\n"
        summary += "**Local Vibe:** $vibe\n\n"
        summary += "*Note: Rent varies by building age and amenities. Always tour the unit first!*"

        val extra = mapOf(
            "type" to "rent_analysis",
            "neighborhood" to match.split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
            "price" to userPrice,
            "avg" to avg,
            "verdict" to verdictKey,
            "diff_pct" to (diffPct * 100).toInt()
        )

        return Pair(summary, extra)
    }

    // MARK: - Neighborhood Real Estate Performance Analysis
    fun analyzeNeighborhoodPerformance(neighborhood: String): Pair<String, Map<String, Any>?> {
        val nb = neighborhood.lowercase().trim()

        // 1. Try to find neighborhood name match
        var match: String? = null
        for (key in neighborhoodData.keys) {
            if (nb.contains(key) || key.contains(nb)) {
                match = key
                break
            }
        }

        // 2. Try Zip code match
        if (match == null) {
            val zipPattern = """\b(28\d{3})\b""".toRegex()
            val result = zipPattern.find(nb)
            if (result != null) {
                val zip = result.groupValues[1]
                match = zipToNeighborhood[zip]
            }
        }

        if (match == null) {
            return Pair("Neighborhood '$neighborhood' not found in local benchmarks.", null)
        }

        val matchedNb = match
        val trend = marketTrends[matchedNb] ?: mapOf("growth" to "N/A", "outlook" to "Unknown", "reason" to "General market growth.")
        val data = neighborhoodData[matchedNb] ?: mapOf("vibe" to "General market info.")

        var summary = "🏘️ **Real Estate Reality Check: ${matchedNb.split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }}**\n\n"
        summary += "📈 **Annual Growth:** ${trend["growth"] ?: "N/A"}\n"
        summary += "🎯 **Investment Outlook:** ${trend["outlook"] ?: "Unknown"}\n\n"
        summary += "**Market Driver:** ${trend["reason"] ?: "N/A"}\n\n"
        data["vibe"]?.let { vibe ->
            summary += "**Local Insight:** $vibe\n\n"
        }
        summary += "💡 *Tip: Single-family homes in this area are currently seeing 15% higher appreciation than high-rise condos.*"

        val extra = mapOf(
            "type" to "reality_check",
            "neighborhood" to matchedNb.split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
            "growth" to (trend["growth"] ?: "N/A"),
            "outlook" to (trend["outlook"] ?: "Unknown"),
            "driver" to (trend["reason"] ?: "N/A")
        )

        return Pair(summary, extra)
    }

    // MARK: - Career & Income Booster
    fun analyzeCareerPath(question: String): Pair<String, Map<String, Any>?> {
        val text = question.lowercase()
        val career = careerData ?: return Pair("Career booster data currently unavailable.", null)

        // 1. Job search
        var foundJob: String? = null
        for (job in career.salaries.keys) {
            if (text.contains(job)) {
                foundJob = job
                break
            }
        }

        // 2. Employer search
        var foundEmployer: String? = null
        for (emp in career.employers.keys) {
            if (text.contains(emp)) {
                foundEmployer = emp
                break
            }
        }

        if (foundJob == null && foundEmployer == null) {
            val summary = "I can help you boost your career in Charlotte! Ask me about specialized salaries (e.g. teachers, software engineers, plumbers), or request an 'Inside Scoop' on major local employers like Atrium Health, CMS, or Bank of America."
            return Pair(summary, null)
        }

        var summary = "💼 **Charlotte Career & Income Booster**\n\n"
        val extra = mutableMapOf<String, Any>("type" to "career_boost")

        if (foundJob != null) {
            val jobData = career.salaries[foundJob]
            if (jobData != null) {
                summary += "📊 **Local Market: ${foundJob.split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }}**\n"
                summary += "• Entry Level: **\$${jobData.entry.toInt()}**\n"
                summary += "• Mid-Career: **\$${jobData.mid.toInt()}**\n"
                summary += "• Senior: **\$${jobData.senior.toInt()}**\n"
                summary += "• Annual Growth: **${jobData.trend}**\n\n"

                extra.putAll(mapOf(
                    "job" to foundJob.split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                    "salary_entry" to "\$${jobData.entry.toInt()}",
                    "salary_mid" to "\$${jobData.mid.toInt()}",
                    "salary_senior" to "\$${jobData.senior.toInt()}",
                    "growth" to jobData.trend
                ))
            }
        }

        if (foundEmployer != null) {
            val vibeText = career.employers[foundEmployer]
            if (vibeText != null) {
                summary += "🏢 **Inside Scoop: ${foundEmployer.split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }}**\n"
                summary += "$vibeText\n\n"
                summary += "💡 *Tip: Resumes for this company should emphasize the specific keywords mentioned above.*"

                extra.putAll(mapOf(
                    "employer" to foundEmployer.split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                    "vibe" to vibeText
                ))
            }
        }

        return Pair(summary, extra)
    }

    companion object {
        val shared = LocalCalculators()
    }
}
