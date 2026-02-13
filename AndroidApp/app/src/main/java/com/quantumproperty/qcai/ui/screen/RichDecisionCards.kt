package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// MARK: - Rich Visual Cards

@Composable
fun WorthItScorecardView(data: Map<String, Any>, isEnglish: Boolean) {
    val score = (data["score"] as? Double)?.toInt() ?: (data["score"] as? Int) ?: 70
    val verdict = data["verdict"] as? String ?: "Worth it"
    val pros = data["pros"] as? List<String> ?: emptyList()
    val cons = data["cons"] as? List<String> ?: emptyList()
    val hack = data["hack"] as? String

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular Score Gauge
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF4CAF50),
                    strokeWidth = 6.dp,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(text = "SCORE", color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column {
                Text(
                    text = if (isEnglish) "VERDICT" else "评语",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = verdict.uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // Pros & Cons
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = if (isEnglish) "PROS" else "优点", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                pros.forEach { pro ->
                    Text(text = "• $pro", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ThumbDown, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = if (isEnglish) "CONS" else "缺点", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                }
                cons.forEach { con ->
                    Text(text = "• $con", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        // Local Hack
        hack?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFD600).copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFD600))
                Column {
                    Text(
                        text = if (isEnglish) "LOCAL HACK" else "本地秘籍",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD600).copy(alpha = 0.8f)
                    )
                    Text(text = it, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun RealityCheckDashboardView(data: Map<String, Any>, isEnglish: Boolean) {
    val neighborhood = data["neighborhood"] as? String ?: "Charlotte"
    val growth = data["growth"] as? String ?: "+0.0%"
    val outlook = data["outlook"] as? String ?: "Stable"
    val driver = data["driver"] as? String ?: "Local urban development."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF007AFF).copy(alpha = 0.05f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = neighborhood, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(
                    text = if (isEnglish) "2026 Market Outlook" else "2026 市场展望",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Surface(
                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = growth,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardMetric(
                label = if (isEnglish) "OUTLOOK" else "展望",
                value = outlook,
                color = Color(0xFF007AFF),
                modifier = Modifier.weight(1f)
            )
            DashboardMetric(
                label = if (isEnglish) "STRATEGY" else "策略",
                value = if (isEnglish) "Buy & Hold" else "长期持有",
                color = Color(0xFFF57C00),
                modifier = Modifier.weight(1f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isEnglish) "PRIMARY GROWTH DRIVER" else "核心增长动力",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = driver,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun RentAnalysisCardView(data: Map<String, Any>, isEnglish: Boolean) {
    val price = (data["price"] as? Double)?.toInt() ?: (data["price"] as? Int) ?: 0
    val avg = (data["avg"] as? Double)?.toInt() ?: (data["avg"] as? Int) ?: 0
    val verdict = data["verdict"] as? String ?: "fair"
    val diff = (data["diff_pct"] as? Double)?.toInt() ?: (data["diff_pct"] as? Int) ?: 0

    val verdictColor = when (verdict) {
        "great" -> Color(0xFF4CAF50)
        "fair" -> Color(0xFF007AFF)
        "premium" -> Color(0xFFF57C00)
        "high" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = if (isEnglish) "Rent Fair-Check" else "租金公平性检测", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Surface(color = verdictColor, shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = verdict.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column {
                Text(text = if (isEnglish) "YOUR PRICE" else "你的价格", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
                Text(text = "$$price", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
            Column {
                Text(text = if (isEnglish) "AVG FOR AREA" else "该区平均", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
                Text(text = "$$avg", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }

        val diffText = if (diff >= 0) "+$diff%" else "$diff%"
        Text(
            text = diffText + (if (isEnglish) " relative to market avg" else " 相对于市场均价"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (diff > 10) Color(0xFFF44336) else if (diff < -5) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun TheSceneDashboardView(data: Map<String, Any>, isEnglish: Boolean) {
    val vibe = data["vibe"] as? String
    val narrative = data["narrative"] as? String
    val events = data["events"] as? List<Map<String, Any>> ?: emptyList()
    val venues = data["venues"] as? List<Map<String, Any>> ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF9C27B0).copy(alpha = 0.05f))
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isEnglish) "THE SCENE" else "社群氛围",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            vibe?.let {
                Text(
                    text = it.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // Narrative
        narrative?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )
        }

        // Events Carousel
        if (events.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isEnglish) "UPCOMING EVENTS" else "近期活动",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(events) { event ->
                        Column(
                            modifier = Modifier
                                .width(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = event["date"] as? String ?: "",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9C27B0)
                            )
                            Text(
                                text = event["name"] as? String ?: "",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(8.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = event["location"] as? String ?: "",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Venues List
        if (venues.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isEnglish) "TOP VENUES" else "热门地点",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                venues.take(3).forEachIndexed { index, venue ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF9C27B0), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = venue["name"] as? String ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        
                        val score = (venue["vibe_match_score"] as? Double) ?: 0.0
                        Text(
                            text = "${(score * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                    }
                    if (index < minOf(venues.size, 3) - 1) {
                        Divider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SalaryBar(label: String, amount: String, height: androidx.compose.ui.unit.Dp, isPrime: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = amount, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isPrime) Color(0xFFF57C00) else Color.White.copy(alpha = 0.7f))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(height)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isPrime) Color(0xFFF57C00) else Color.White.copy(alpha = 0.2f))
        )
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
fun DashboardMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.4f))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}
