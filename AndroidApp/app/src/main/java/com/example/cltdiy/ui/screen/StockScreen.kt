package com.example.cltdiy.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import com.example.cltdiy.data.PreferenceManager
import com.example.cltdiy.ui.screen.PrimaryPurple // Reusing from TeacherScreen definitions if accessible, or redefine

// --- RETROFIT API & MODELS ---

data class QuoteResponse(val c: Double, val d: Double, val dp: Double, val h: Double, val l: Double)
data class AnalysisRequest(val symbol: String, val question: String)
data class AIResponse(val answer: String)
data class ExplainResponse(val explanation: String)
data class RiskRequest(val holdings: List<String>)
data class RiskResponse(
    val stats: List<String>, 
    val ai_analysis: String?
)

interface StockService {
    @GET("stock/quote/{symbol}")
    suspend fun getQuote(@Path("symbol") symbol: String): QuoteResponse

    @POST("stock/analyze")
    suspend fun analyze(
        @Header("x-gemini-api-key") apiKey: String,
        @Body request: AnalysisRequest
    ): AIResponse
    
    @GET("stock/explain/{symbol}")
    suspend fun explainStock(
        @Header("x-gemini-api-key") apiKey: String,
        @Path("symbol") symbol: String
    ): ExplainResponse
    
    @POST("stock/risk/analyze")
    suspend fun analyzeRisk(
        @Header("x-gemini-api-key") apiKey: String,
        @Body request: RiskRequest
    ): RiskResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://cyberpandaapp.com/"
    
    val api: StockService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StockService::class.java)
    }
}

// --- COMPOSE UI ---

// Re-defining colors locally to ensure self-containment if TeacherScreen constant isn't public
val CyberBlue = Color(0xFF007AFF) // Typical iOS Blue
val CyberPurple = Color(0xFFAF52DE)
val BackgroundGray = Color(0xFFF2F2F7) // System grouped background

@Composable
fun StockScreen(onOpenSettings: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(0) }
    // Use global preference manager for the key
    val geminiKey = PreferenceManager.geminiKey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "AI Stock Insight", 
                style = MaterialTheme.typography.headlineMedium,
                color = CyberBlue,
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
                SingleStockView(geminiKey, onOpenSettings)
            } else {
                PortfolioRiskView(geminiKey, onOpenSettings)
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
fun SingleStockView(geminiKey: String, onOpenSettings: () -> Unit) {
    var symbol by remember { mutableStateOf("AAPL") }
    var quote by remember { mutableStateOf<QuoteResponse?>(null) }
    var aiResponse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var userQuestion by remember { mutableStateOf("Why did it move today?") }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Initial fetch
        if (symbol.isNotEmpty()) {
            try {
                quote = RetrofitClient.api.getQuote(symbol)
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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
                    if (geminiKey.isEmpty()) {
                        onOpenSettings()
                        return@Button
                    }
                    scope.launch {
                         try {
                             quote = RetrofitClient.api.getQuote(symbol)
                         } catch (e: Exception) {
                             aiResponse = "Error: ${e.message}"
                         }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
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
                            color = if (q.dp >= 0) Color(0xFF34C759) else Color(0xFFFF3B30) // iOS Green/Red
                        )
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    // Deep Dive Analysis Button
                    TextButton(
                        onClick = {
                            if (geminiKey.isEmpty()) {
                                onOpenSettings()
                                return@TextButton
                            }
                            isLoading = true
                            scope.launch {
                                try {
                                    val result = RetrofitClient.api.explainStock(geminiKey, symbol)
                                    aiResponse = result.explanation
                                } catch (e: Exception) {
                                    aiResponse = "Error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = CyberPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberPurple.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Deep Dive Analysis")
                    }
                }
            }
            
            // Ask AI Analyst
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ask AI Analyst", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Ask questions like 'What is their latest earnings report?' or 'Why is it down today?'",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                OutlinedTextField(
                    value = userQuestion,
                    onValueChange = { userQuestion = it },
                    placeholder = { Text("Question...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                
                Button(
                    onClick = {
                        if (geminiKey.isEmpty()) {
                            onOpenSettings()
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            try {
                                val result = RetrofitClient.api.analyze(geminiKey, AnalysisRequest(symbol, userQuestion))
                                aiResponse = result.answer
                            } catch (e: Exception) {
                                aiResponse = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Analyze")
                    }
                }
                
                if (aiResponse.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberBlue.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            aiResponse,
                            modifier = Modifier.padding(16.dp),
                            color = Color.Black
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun PortfolioRiskView(geminiKey: String, onOpenSettings: () -> Unit) {
    var holdingsInput by remember { mutableStateOf("AAPL, MSFT, TSLA, GOOG") }
    // Ideally use DataStore to persist 'holdingsInput'
    var riskResponse by remember { mutableStateOf<RiskResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Analyze your portfolio risk based on volatility and sector concentration.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
             Text("Enter Tickers (comma separated)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
             Text(
                "Enter your entire portfolio once below. The app will remember it for next time.",
                style = MaterialTheme.typography.bodySmall,
                color = CyberBlue
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
                if (geminiKey.isEmpty()) {
                    onOpenSettings()
                    return@Button
                }
                if (holdings.isEmpty()) return@Button
                
                isLoading = true
                errorMessage = ""
                scope.launch {
                    try {
                        riskResponse = RetrofitClient.api.analyzeRisk(geminiKey, RiskRequest(holdings))
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange), // Using custom orange or default
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
                colors = CardDefaults.cardColors(containerColor = BackgroundGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                 Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                     Text("Risk Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                     
                     risk.stats.forEach { stat ->
                         Text(
                             "• $stat",
                             style = MaterialTheme.typography.bodyMedium,
                             fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                         )
                     }
                     
                     Divider()
                     
                     Text("AI Risk Assessment", style = MaterialTheme.typography.titleMedium, color = AccentOrange, fontWeight = FontWeight.Bold)
                     
                     risk.ai_analysis?.let { analysis ->
                         Card(
                             colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.1f)),
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
        
        Spacer(Modifier.height(40.dp))
    }
}
