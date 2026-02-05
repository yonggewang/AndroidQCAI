package com.quantumproperty.qcai.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.quantumproperty.qcai.data.Recommendation
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import com.quantumproperty.qcai.data.ChatMessage
import com.quantumproperty.qcai.data.AITopic

@Composable
fun CLTVibeView(viewModel: TeacherViewModel, isEnglish: Boolean) {
    val recommendations by viewModel.recommendations.collectAsState()
    val vibeHistory by viewModel.vibeHistory.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var energyLevel by remember { mutableStateOf(50f) }

    val lastAIMessage = messages.lastOrNull { !it.isUser && !it.isHidden }


    val filterChips = listOf(
        "Fast Wi-Fi", "Quiet for Work", "Scenic View", "Pet Friendly", 
        "Live Music", "Family Friendly", "Late Night", "Outdoor Seating"
    )

    val energyLevelLabel = when {
        energyLevel < 30 -> if (isEnglish) "Chill" else "宁静"
        energyLevel < 70 -> if (isEnglish) "Moderate" else "适中"
        else -> if (isEnglish) "Lively" else "热闹"
    }

    val sampleQuestions = if (isEnglish) {
        listOf(
            "Find a cozy coffee shop in South End good for remote work.",
            "Where is a scenic rooftop bar Uptown that isn't too loud?",
            "Best vintage clothing store in Plaza Midwood.",
            "Where can I find live jazz music tonight?",
            "We did a great job today. We want to go out and celebrate, where should we go?"
        )
    } else {
        listOf(
            "在 South End 找一家适合远程办公的舒适咖啡馆。",
            "Uptown 有哪些风景好但不太吵的屋顶酒吧？",
            "Plaza Midwood 最好的复古服装店是哪家？",
            "今晚哪里有现场爵士乐演出？",
            "我们今天工作很出色，想去庆祝一下，有什么推荐的地方？"
        )
    }

    val energyColor by animateColorAsState(
        targetValue = when {
            energyLevel < 30 -> Color(0xFF00ACC1)
            energyLevel < 70 -> Color(0xFF3F51B5)
            else -> Color(0xFF673AB7)
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6A1B9A), Color(0xFF4527A0))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "Charlotte Concierge" else "夏洛特管家",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isEnglish) "Hyper-local vibes & schedules" else "深度本地探索与生活指南",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }



        // Vibe Check Selector (Compact)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEnglish) "Vibe Check" else "氛围筛选",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                // Compact Slider Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF0F0F0)) // Light gray background
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Energy:" else "活跃度:",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    
                    Text(
                        text = energyLevelLabel,
                        fontWeight = FontWeight.Bold,
                        color = energyColor,
                        fontSize = 14.sp,
                        modifier = Modifier.width(70.dp)
                    )
                    
                    Slider(
                        value = energyLevel,
                        onValueChange = { energyLevel = it },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = energyColor,
                            activeTrackColor = energyColor
                        )
                    )
                }

                // Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(filterChips) { chip ->
                        Surface(
                            modifier = Modifier.clickable {
                                val q = if (isEnglish) 
                                    "Find a $chip place in Charlotte with $energyLevelLabel energy" 
                                    else "帮我找一个具有 $chip 氛围且活跃度为 $energyLevelLabel 的地方"
                                viewModel.sendMessage(q)
                            },
                            shape = RoundedCornerShape(50.dp),
                            color = Color(0xFF3F51B5).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = chip,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF3F51B5)
                            )
                        }
                    }
                }
            }
        }

        // Latest Response
        lastAIMessage?.let { msg ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEnglish) "Latest Response" else "最新回答",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        val displayText = msg.text.split("MATCH_SCORE_JSON")[0].trim()
                        Text(
                            text = displayText,
                            modifier = Modifier.padding(20.dp),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = Color(0xFF333333)
                        )
                    }
                }
            }
        }

        // Top Matches
        if (recommendations.isNotEmpty()) {
            item {
                Text(
                    text = if (isEnglish) "Top Vibe Matches" else "最佳氛围匹配",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            items(recommendations) { rec ->
                RecommendationCard(rec, isEnglish)
            }
        }

        // Recent Findings
        if (vibeHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Recent Findings" else "最近发现",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (isEnglish) "Clear" else "清除",
                        color = Color.Blue,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { viewModel.clearVibeHistory() }
                    )
                }
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    contentPadding = PaddingValues(bottom = 10.dp)
                ) {
                    items(vibeHistory) { rec ->
                        Box(modifier = Modifier.width(280.dp)) {
                            RecommendationCard(rec, isEnglish)
                        }
                    }
                }
            }
        }

        // Quick Search
        item {
            Text(
                text = if (isEnglish) "Quick Search" else "快速搜索",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        items(sampleQuestions) { question ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.sendMessage(question) },
                shape = RoundedCornerShape(15.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Blue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(text = question, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
        
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
fun RecommendationCard(rec: Recommendation, isEnglish: Boolean) {
    val scoreColor = when {
        rec.score >= 90 -> Color(0xFF4CAF50)
        rec.score >= 70 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = rec.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = rec.reason,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = scoreColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = scoreColor, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isEnglish) "${rec.score}% Vibe Match" else "${rec.score}% 匹配",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = rec.score / 100f,
                    modifier = Modifier.size(50.dp),
                    color = scoreColor,
                    strokeWidth = 4.dp,
                    trackColor = scoreColor.copy(alpha = 0.1f)
                )
                Text(
                    text = "${rec.score}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
        }
    }
}
