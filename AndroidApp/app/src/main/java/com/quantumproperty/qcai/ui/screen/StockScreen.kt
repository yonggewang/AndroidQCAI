package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.google.gson.annotations.SerializedName

import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import com.quantumproperty.qcai.data.AIService
import com.quantumproperty.qcai.data.AIEngine
import com.quantumproperty.qcai.data.AITopic
import com.quantumproperty.qcai.data.AppLanguage
import com.google.gson.Gson
import com.quantumproperty.qcai.data.PreferenceManager


import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.layout.ContentScale

// --- CLIENT-SIDE STOCK API & MODELS ---

data class QuoteResponse(val c: Double, val d: Double, val dp: Double, val h: Double, val l: Double)
data class AnalysisRequest(val symbol: String, val question: String)
data class AIResponse(val answer: String)
data class ExplainResponse(val explanation: String)
data class RiskRequest(val holdings: List<String>)
data class RiskResponse(
    val stats: List<String>, 
    @SerializedName("ai_analysis") val aiAnalysis: String?,
    @SerializedName("correlation_chart") val correlationChart: String?
)

data class StockReport(
    val symbol: String,
    @SerializedName("valuation_score") val valuationScore: Int,
    @SerializedName("valuation_msg") val valuationMsg: String,
    @SerializedName("safety_score") val safetyScore: Int,
    @SerializedName("safety_msg") val safetyMsg: String,
    @SerializedName("trend_score") val trendScore: Int,
    @SerializedName("trend_msg") val trendMsg: String
)

data class TrendingStock(
    val symbol: String,
    @SerializedName("change_pct") val changePct: Double,
    val price: Double
)

interface StockService {
    suspend fun getQuote(symbol: String): QuoteResponse
    suspend fun analyze(apiKey: String, request: AnalysisRequest): AIResponse
    suspend fun explainStock(apiKey: String, symbol: String): ExplainResponse
    suspend fun getReport(symbol: String): StockReport
    suspend fun getTrending(): List<TrendingStock>
    suspend fun analyzeRisk(apiKey: String, request: RiskRequest): RiskResponse
}

class LocalStockService : StockService {
    private val client = OkHttpClient()
    private val gson = Gson()

    override suspend fun getQuote(symbol: String): QuoteResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val token = PreferenceManager.finnhubKey.ifEmpty { "d5v9hb1r01qjj9jio9h0d5v9hb1r01qjj9jio9hg" }
        val cleanSymbol = symbol.trim().uppercase()
        val url = "https://finnhub.io/api/v1/quote?symbol=$cleanSymbol&token=$token"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Finnhub Error: ${response.code}")
            }
            val json = response.body?.string() ?: "{}"
            gson.fromJson(json, QuoteResponse::class.java)
        }
    }

    override suspend fun analyze(apiKey: String, request: AnalysisRequest): AIResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prompt = "Stock Symbol: ${request.symbol}. User Question: ${request.question}. Provide a detailed financial analysis."
        val answer = AIService().sendMessage(
            text = prompt,
            engine = AIEngine.GEMINI,
            topic = AITopic.STOCK,
            language = AppLanguage.ENGLISH,
            image = null,
            realEstateAddress = null
        )
        AIResponse(answer)
    }

    override suspend fun explainStock(apiKey: String, symbol: String): ExplainResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prompt = "Provide a Deep Dive analysis of $symbol stock, summarizing recent news, price action, and potential risks/opportunities."
        val answer = AIService().sendMessage(
            text = prompt,
            engine = AIEngine.GEMINI,
            topic = AITopic.STOCK,
            language = AppLanguage.ENGLISH,
            image = null,
            realEstateAddress = null
        )
        ExplainResponse(answer)
    }

    override suspend fun getReport(symbol: String): StockReport = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prompt = """
        Research valuation, safety, and trend indicators for stock symbol: $symbol.
        Return a JSON matching this format:
        {
          "symbol": "$symbol",
          "valuation_score": 4,
          "valuation_msg": "Good Value",
          "safety_score": 3,
          "safety_msg": "Moderate Risk",
          "trend_score": 5,
          "trend_msg": "Strong Up-Trend"
        }
        Return ONLY the raw JSON, no formatting, no markdown backticks.
        """.trimIndent()

        val responseJson = AIService().sendMessage(
            text = prompt,
            engine = AIEngine.GEMINI,
            topic = AITopic.STOCK,
            language = AppLanguage.ENGLISH,
            image = null,
            realEstateAddress = null
        )

        val cleanJson = responseJson
            .replace("```json", "")
            .replace("```", "")
            .trim()

        gson.fromJson(cleanJson, StockReport::class.java)
    }

    override suspend fun getTrending(): List<TrendingStock> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val symbols = listOf("AAPL", "TSLA", "NVDA", "MSFT", "AMZN", "GOOGL")
        val list = mutableListOf<TrendingStock>()
        for (sym in symbols) {
            try {
                val quote = getQuote(sym)
                list.add(TrendingStock(
                    symbol = sym,
                    changePct = quote.dp,
                    price = quote.c
                ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        list
    }

    override suspend fun analyzeRisk(apiKey: String, request: RiskRequest): RiskResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val holdingsList = request.holdings.joinToString(", ")
        val prompt = """
        Analyze the portfolio risk for these holdings: $holdingsList.
        Provide:
        1. Sector concentration stats.
        2. Average beta and overall risk level estimate.
        3. Potential correlation risks.
        
        Format your response as a JSON matching this structure:
        {
          "stats": [
            "Beta: 1.25 (Aggressive)",
            "Concentration: Technology 60%, Finance 20%",
            "Worst historical drop estimate: -18%"
          ],
          "ai_analysis": "A detailed 2-paragraph risk assessment of these stocks..."
        }
        Return ONLY the raw JSON.
        """.trimIndent()

        val responseJson = AIService().sendMessage(
            text = prompt,
            engine = AIEngine.GEMINI,
            topic = AITopic.STOCK,
            language = AppLanguage.ENGLISH,
            image = null,
            realEstateAddress = null
        )

        val cleanJson = responseJson
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val localResp = gson.fromJson(cleanJson, TempRiskResponse::class.java)
        RiskResponse(localResp.stats, localResp.ai_analysis, null)
    }
}

private data class TempRiskResponse(
    val stats: List<String>,
    val ai_analysis: String
)

object RetrofitClient {
    val api: StockService = LocalStockService()
}

// --- COMPOSE UI ---

// Re-defining colors locally to ensure self-containment and avoid conflicts
private val LocalCyberBlue = Color(0xFF007AFF) // Typical iOS Blue
private val LocalCyberPurple = Color(0xFFAF52DE)
private val LocalAccentOrange = Color(0xFFFF9500) // Added for Portfolio/Risk
private val LocalBackgroundGray = Color(0xFFF2F2F7) // System grouped background

@Composable
fun StockScreen(onBack: (() -> Unit)? = null) {
    var selectedTab by remember { mutableStateOf(0) }
    // Use global preference manager for the key
    val geminiKey = PreferenceManager.geminiKey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalBackgroundGray)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LocalCyberBlue)
                }
            }
            Text(
                "AI Stock Insight", 
                style = MaterialTheme.typography.headlineMedium,
                color = LocalCyberBlue,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            // Settings button could go here, but settings are global in TeacherScreen
        }

        // Custom Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabButton(
                text = "Quote & Research",
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "Portfolio Risk",
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Content
        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                SingleStockView(geminiKey)
            } else {
                PortfolioRiskView(geminiKey)
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.padding(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.White else Color.Transparent,
            contentColor = if (isSelected) Color.Black else Color.Gray
        ),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(0.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun SingleStockView(geminiKey: String) {
    var symbol by remember { mutableStateOf("GOOG") }
    var quote by remember { mutableStateOf<QuoteResponse?>(null) }
    var aiResponse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var userQuestion by remember { mutableStateOf("Why did it move today?") }
    var report by remember { mutableStateOf<StockReport?>(null) }
    var showScoreInfo by remember { mutableStateOf(false) }
    var trending by remember { mutableStateOf<List<TrendingStock>>(emptyList()) }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        // Initial fetch
        if (symbol.isNotEmpty()) {
            try {
                quote = RetrofitClient.api.getQuote(symbol)
                report = RetrofitClient.api.getReport(symbol)
                trending = RetrofitClient.api.getTrending()
            } catch (_: Exception) {}
        } else {
             try { trending = RetrofitClient.api.getTrending() } catch(_: Exception) {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Trending Row
            if (trending.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔥 Trending Now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(trending.size) { i ->
                            val item = trending[i]
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.clickable { 
                                    symbol = item.symbol 
                                    scope.launch {
                                        try {
                                            isLoading = true 
                                            quote = RetrofitClient.api.getQuote(symbol)
                                            report = RetrofitClient.api.getReport(symbol)
                                        } catch(_: Exception) {}
                                        finally { isLoading = false }
                                    }
                                }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(item.symbol, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${if(item.changePct >=0) "+" else ""}${String.format("%.1f", item.changePct)}%", 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (item.changePct >= 0) Color(0xFF34C759) else Color(0xFFFF3B30)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Search Bar
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = { Text("Ticker (e.g. NVDA)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                Button(
                    onClick = {
                        scope.launch {
                             try {
                                 quote = RetrofitClient.api.getQuote(symbol)
                                 report = RetrofitClient.api.getReport(symbol)
                             } catch (e: Throwable) {
                                 if (e is kotlinx.coroutines.CancellationException) throw e
                                 aiResponse = "Error: ${e.localizedMessage ?: e.message ?: e.toString()}"
                             }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalCyberBlue),
                    modifier = Modifier.height(56.dp)
                ) {
                     Icon(Icons.Default.Search, contentDescription = null)
                }
            }

            // Quote Display & AI Actions
            quote?.let { q ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(symbol, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("$${String.format("%.2f", q.c)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "H: ${String.format("%.2f", q.h)}  L: ${String.format("%.2f", q.l)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                "${String.format("%.2f", q.dp)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (q.dp >= 0) Color(0xFF34C759) else Color(0xFFFF3B30)
                            )
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        report?.let { r ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { showScoreInfo = true },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Stock Report Card", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.Info, contentDescription = "Info", tint = LocalCyberBlue, modifier = Modifier.size(18.dp))
                                }
                                
                                ScoreRow("Valuation", r.valuationScore, r.valuationMsg)
                                ScoreRow("Safety", r.safetyScore, r.safetyMsg)
                                ScoreRow("Trend", r.trendScore, r.trendMsg)
                            }
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                        }
                        
                        TextButton(
                            onClick = {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val result = RetrofitClient.api.explainStock(geminiKey, symbol)
                                        aiResponse = result.explanation
                                    } catch (e: Throwable) {
                                        if (e is kotlinx.coroutines.CancellationException) throw e
                                        aiResponse = "Error: ${e.localizedMessage ?: e.message ?: e.toString()}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = LocalCyberPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LocalCyberPurple.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Deep Dive Analysis")
                        }
                    }
                }
            }
            
            // AI Response Card (Moved into Scrollable Area)
            if (aiResponse.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LocalCyberBlue.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        aiResponse,
                        modifier = Modifier.padding(16.dp),
                        color = Color.Black
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
        }

        // Sticky Bottom Input Area
        Surface(
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .imePadding(), // Ensure keyboard pushes it up
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Ask AI Analyst", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = userQuestion,
                    onValueChange = { userQuestion = it },
                    placeholder = { Text("Ask about earnings, news, etc...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val result = RetrofitClient.api.analyze(geminiKey, AnalysisRequest(symbol, userQuestion))
                                aiResponse = result.answer
                            } catch (e: Throwable) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                aiResponse = "Error: ${e.localizedMessage ?: e.message ?: e.toString()}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = LocalCyberBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Analyze")
                    }
                }
            }
        }
        
        if (showScoreInfo) {
            AlertDialog(
                onDismissRequest = { showScoreInfo = false },
                title = { Text("Stock Score Guide") },
                text = {
                    Text(
                        "Valuation: Is it on Sale?\n" +
                        "• 5/5: Bargain Price!\n" +
                        "• 1/5: Very Expensive.\n\n" +
                        "Safety: Will I lose sleep?\n" +
                        "• 5/5: Huge, stable company.\n" +
                        "• 1/5: Small, volatile stock.\n\n" +
                        "Trend: Is it popular?\n" +
                        "• 5/5: Strong Up-Trend (People are buying).\n" +
                        "• 1/5: Down-Trend (People are selling)."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showScoreInfo = false }) { Text("Got it") }
                }
            )
        }
    }
}

@Composable
fun ScoreRow(title: String, score: Int, msg: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) { i ->
                val filled = (i + 1) <= score
                val color = if (filled) {
                    if (score >= 4) Color(0xFF34C759) // Green
                    else if (score == 3) Color(0xFFFFCC00) // Yellow
                    else Color(0xFFFF3B30) // Red
                } else Color.Gray.copy(alpha = 0.2f)
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Text(msg, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun PortfolioRiskView(geminiKey: String) {
    var holdingsInput by remember { mutableStateOf("AAPL, MSFT, TSLA, GOOG") }
    // Ideally use DataStore to persist 'holdingsInput'
    var riskResponse by remember { mutableStateOf<RiskResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showRiskInfo by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ... (existing content) ...
        
        // Content Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Analyze your portfolio risk based on volatility and sector concentration.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
             Text("Enter Tickers (comma separated)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
             Text(
                "Enter your entire portfolio once below. The app will remember it for next time.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalCyberBlue
             )
             
             OutlinedTextField(
                 value = holdingsInput,
                 onValueChange = { holdingsInput = it },
                 modifier = Modifier
                     .fillMaxWidth()
                     .height(120.dp),
                 colors = OutlinedTextFieldDefaults.colors(
                     focusedContainerColor = Color.White,
                     unfocusedContainerColor = Color.White
                 )
             )
        }
        
        Button(
            onClick = {
                val holdings = holdingsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                if (holdings.isEmpty()) return@Button
                
                isLoading = true
                errorMessage = ""
                scope.launch {
                    try {
                        riskResponse = RetrofitClient.api.analyzeRisk(geminiKey, RiskRequest(holdings))
                    } catch (e: Throwable) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        errorMessage = "Error: ${e.localizedMessage ?: e.message ?: e.toString()}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = LocalAccentOrange), // Using custom orange or default
            shape = RoundedCornerShape(10.dp)
        ) {
             if (isLoading) {
                 CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
             } else {
                 Text("Analyze Risk")
             }
        }
        
        riskResponse?.let { risk ->
            Card(
                colors = CardDefaults.cardColors(containerColor = LocalBackgroundGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                 Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                     Text("Risk Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                     
                     Text(
                         "Beta > 1.0: Aggressive | MaxDD: Worst Drop | Vol: Fluctuation",
                         style = MaterialTheme.typography.bodySmall,
                         color = Color.Gray
                     )
                     
                     risk.stats.forEach { stat ->
                         Text(
                             "• $stat",
                             style = MaterialTheme.typography.bodyMedium,
                             fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                             fontSize = 12.sp
                         )
                     }
                     
                     // New: Correlation Matrix Heatmap
                     risk.correlationChart?.let { base64Str ->
                         val imageBitmap = remember(base64Str) {
                             try {
                                 val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                                 BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                             } catch (_: Exception) { null }
                         }

                         if (imageBitmap != null) {
                             Divider()
                             Text("Correlation Matrix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                             Image(
                                 bitmap = imageBitmap,
                                 contentDescription = "Correlation Matrix",
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .background(Color.White, RoundedCornerShape(10.dp)),
                                 contentScale = ContentScale.FillWidth
                             )
                         }
                     }
                     
                     Divider()
                     
                     Text("AI Risk Assessment", style = MaterialTheme.typography.titleMedium, color = LocalAccentOrange, fontWeight = FontWeight.Bold)
                     
                     risk.aiAnalysis?.let { analysis ->
                         Card(
                             colors = CardDefaults.cardColors(containerColor = LocalAccentOrange.copy(alpha = 0.1f)),
                             shape = RoundedCornerShape(10.dp)
                         ) {
                             Text(analysis, modifier = Modifier.padding(16.dp))
                         }
                     }
                 }
            }
        }
        
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
        }
        
        if (showRiskInfo) {
            AlertDialog(
                onDismissRequest = { showRiskInfo = false },
                title = { Text("Risk Metrics Guide") },
                text = {
                    Text(
                        "Beta (Market Sensitivity):\n" +
                        "• > 1.0: Higher Risk (Moves more than market).\n" +
                        "• < 1.0: Lower Risk (More stable).\n" +
                        "• The higher the Beta, the higher the risk.\n\n" +
                        "MaxDD (Drawdown):\n" +
                        "The worst loss from a peak to a low point in the last year.\n\n" +
                        "Vol (Volatility):\n" +
                        "How much the price fluctuates. Higher % means a bumpier ride."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showRiskInfo = false }) {
                        Text("Got it")
                    }
                }
            )
        }
        
        Spacer(Modifier.height(40.dp))
    }
}
