package com.quantumproperty.qcai.ui.screen

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.quantumproperty.qcai.data.Recommendation
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import com.quantumproperty.qcai.data.ChatMessage
import com.quantumproperty.qcai.data.AITopic
import com.quantumproperty.qcai.data.ChatConfigManager
import kotlinx.coroutines.launch

@Composable
fun CLTVibeView(viewModel: TeacherViewModel, isEnglish: Boolean) {
    val recommendations by viewModel.recommendations.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var energyLevel by remember { mutableStateOf(50f) }
    val lastAIMessage = messages.lastOrNull { !it.isUser && !it.isHidden }
    
    // Address State
    var savedHomeAddress by remember { mutableStateOf(com.quantumproperty.qcai.data.PreferenceManager.homeAddress) }
    var isShowingAddressEditor by remember { mutableStateOf(false) }

    var dailyBrief by remember { mutableStateOf<com.quantumproperty.qcai.data.DailyBriefResponse?>(null) }
    var isLoadingBrief by remember { mutableStateOf(true) }
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Remote Config State
    val configSuggestions by ChatConfigManager.instance.suggestions.collectAsState()
    val featuredSuggestions by ChatConfigManager.instance.featuredSuggestions.collectAsState()
    val isConfigLoaded by ChatConfigManager.instance.isLoaded.collectAsState()

    suspend fun loadBrief(context: android.content.Context, force: Boolean = false) {
        isLoadingBrief = true
        try {
            dailyBrief = com.quantumproperty.qcai.data.CityOSService.instance.fetchDailyBrief(forceRefresh = force)
        } catch (e: Exception) { 
            e.printStackTrace()
            // Show error toast
            android.widget.Toast.makeText(context, "Briefing Load Failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        } finally { isLoadingBrief = false }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val listState = rememberLazyListState()
    
    LaunchedEffect(Unit) { 
        launch { ChatConfigManager.instance.fetchConfig() }
        loadBrief(context) 
    }
    
    // Auto-Scroll to Expert Intel when a new AI message arrives
    LaunchedEffect(lastAIMessage) {
        if (lastAIMessage != null) {
            // Wait a brief moment for the item to be composed and measured
            kotlinx.coroutines.delay(300) 
            listState.animateScrollToItem(5)
        }
    }

    val filterChips = listOf(
        "Tech Networking", "Live Music", "Art & Culture", "Social Mixers", 
        "Family Friendly", "Fast Wi-Fi", "Quiet for Work", "Scenic View", 
        "Pet Friendly", "Late Night", "Outdoor Seating"
    )

    val energyLevelLabel = when {
        energyLevel < 30 -> if (isEnglish) "Chill" else "宁静"
        energyLevel < 70 -> if (isEnglish) "Moderate" else "适中"
        else -> if (isEnglish) "Lively" else "热闹"
    }

    val energyColor by animateColorAsState(
        targetValue = when {
            energyLevel < 30 -> Color(0xFF00ACC1)
            energyLevel < 70 -> Color(0xFF3F51B5)
            else -> Color(0xFF9C27B0)
        }
    )

    val sampleQuestions = if (configSuggestions.isNotEmpty()) {
        configSuggestions.map { it.prompt.localized(isEnglish) }
    } else if (isEnglish) {
        listOf(
            "Tell me about Coco and the Director. Is it expensive?",
            "Best breweries in South End with outdoor seating?",
            "Recommend a romantic dinner spot in Uptown.",
            "Where can I find the best coffee in NoDa?",
            "Is it safe to walk around Uptown at night?",
            "Find a family-friendly park near Dilworth.",
            "What are the top-rated schools in Ballantyne?",
            "Any live music venues active this weekend?",
            "Where to find authentic NC BBQ in Charlotte?"
        )
    } else {
        listOf(
            "Coco and the Director 怎么样？贵吗？",
            "推荐 South End 有户外座位的精酿啤酒厂。",
            "Uptown 有哪些适合约会的浪漫餐厅？",
            "NoDa 最好的咖啡馆在哪里？",
            "晚上在 Uptown 散步安全吗？",
            "Dilworth 附近有哪些适合家庭的公园？",
            "Ballantyne 评分最高的学校有哪些？",
            "这周末有哪些有现场音乐的场所？",
            "夏洛特哪里有正宗的北卡烧烤？"
        )
    }


    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // High-Tech Ambient Blurs
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF007AFF).copy(alpha = 0.15f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(-100f, -100f),
                    radius = 800f
                ),
                radius = 800f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF9D50BB).copy(alpha = 0.12f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width + 100f, 600f),
                    radius = 700f
                ),
                radius = 700f
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. TOP STATUS BAR (Branding)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = if (isEnglish) "Charlotte Today" else "夏洛特今日",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnglish) "City OS Intel" else "城市综合智能",
                            color = Color(0xFF007AFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    IconButton(onClick = { scope.launch { loadBrief(context, true) } }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // 2. DAILY BRIEFING (Glassmorphic)
            item {
                if (dailyBrief != null) {

                    Surface(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isEnglish) "Live Briefing" else "实时简报",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Freshness Badge
                                val minutesAgo = try {
                                    val instant = Instant.parse(dailyBrief?.generatedAt?.replace(".\\d+".toRegex(), "Z"))
                                    ChronoUnit.MINUTES.between(instant, Instant.now())
                                } catch (e: Exception) { 0L }

                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (minutesAgo < 1) (if (isEnglish) "Just now" else "刚刚") else (if (isEnglish) "${minutesAgo}m ago" else "${minutesAgo}分钟前"),
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(Modifier.weight(1f))
                                dailyBrief?.weather?.let { w ->
                                    Text(text = "${w.temp.toInt()}°", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Text(
                                text = dailyBrief?.briefingText ?: "",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                                modifier = Modifier.clickable { isExpanded = !isExpanded }
                            )

                            // Proactive Smart Trigger Cards in Briefing
                            dailyBrief?.extraData?.let { extra ->
                                Column(
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            // Scroll to Expert Intel area (index 5)
                                            // The click itself doesn't generate content, so we just scroll to the section
                                            listState.animateScrollToItem(5)
                                        }
                                    },
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    (extra["reality_check"] as? Map<String, Any>)?.let { nbData ->
                                        RealityCheckDashboardView(data = nbData, isEnglish = isEnglish)
                                    }
                                    (extra["the_scene"] as? Map<String, Any>)?.let { sceneData ->
                                        TheSceneDashboardView(data = sceneData, isEnglish = isEnglish)
                                    }
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(dailyBrief?.topNews ?: emptyList()) { news ->
                                    Surface(
                                        color = Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                    ) {
                                        Text(
                                            text = news.headline,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Social Vibe Entry
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.showTheSceneView("Tech") },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFF9C27B0),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF9C27B0).copy(alpha = 0.1f))
                                        .padding(8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "The Scene" else "社群氛围",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isEnglish) "Browse local events and hot spots" else "浏览本地活动和热门地点",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF9C27B0))
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("Tech", "Music", "Art", "Social", "Family")) { cat ->
                                    Surface(
                                        color = Color(0xFF9C27B0).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable {
                                            val prompt = if (isEnglish) "What's the $cat scene like in Charlotte?" else "夏洛特的 $cat 氛围怎么样？"
                                            viewModel.sendMessage(prompt)
                                        }
                                    ) {
                                        Text(
                                            text = cat,
                                            color = Color(0xFF9C27B0),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (isLoadingBrief) {
                    // High-Tech Loading Card with Circular Spinner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = if (isEnglish) "Fetching City Intel..." else "正在获取城市信息...",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Retry Button
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            scope.launch { loadBrief(context, true) }
                        },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = if (isEnglish) "Tap to Load Intel" else "点击加载城市智能",
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(20.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // 2.2 HOME ADDRESS SETTINGS
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "My Neighborhood" else "我的街区",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = if (savedHomeAddress.isEmpty()) (if (isEnglish) "Set Home" else "设置地址") else (if (isEnglish) "Edit" else "修改"),
                                color = Color(0xFF007AFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { isShowingAddressEditor = !isShowingAddressEditor }
                            )
                        }

                        if (savedHomeAddress.isNotEmpty() && !isShowingAddressEditor) {
                            Text(
                                text = savedHomeAddress,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (isShowingAddressEditor) {
                            TextField(
                                value = savedHomeAddress,
                                onValueChange = { 
                                    savedHomeAddress = it
                                    com.quantumproperty.qcai.data.PreferenceManager.homeAddress = it
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(if (isEnglish) "Enter your CLT address..." else "输入您的夏洛特地址...") },
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                            Button(
                                onClick = { isShowingAddressEditor = false },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isEnglish) "Save" else "保存")
                            }
                        }
                    }
                }
            }

            // 2.5 FEATURED SUGGESTIONS (Chips)
            if (featuredSuggestions.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(featuredSuggestions) { suggestion ->
                            Surface(
                                modifier = Modifier.clickable {
                                    var prompt = suggestion.prompt.localized(isEnglish)
                                    if (savedHomeAddress.isNotEmpty()) {
                                        prompt = prompt.replace("[your address]", savedHomeAddress)
                                                       .replace("[address]", savedHomeAddress)
                                    }
                                    viewModel.sendMessage(prompt)
                                    scope.launch {
                                        // Scroll to Expert Intel area (index 5)
                                        listState.animateScrollToItem(5)
                                    }
                                },
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = suggestion.emoji, fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = suggestion.label.localized(isEnglish),
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Vibe Pulse Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEnglish) "Select Energy" else "选择活跃度",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VibePulseChip(
                            label = if (isEnglish) "Chill" else "宁静",
                            icon = Icons.Default.Spa,
                            color = Color(0xFF00ACC1),
                            isActive = energyLevel < 30,
                            modifier = Modifier.weight(1f)
                        ) { energyLevel = 15f }

                        VibePulseChip(
                            label = if (isEnglish) "Moderate" else "适中",
                            icon = Icons.Default.Coffee,
                            color = Color(0xFF3F51B5),
                            isActive = energyLevel >= 30 && energyLevel < 70,
                            modifier = Modifier.weight(1f)
                        ) { energyLevel = 50f }

                        VibePulseChip(
                            label = if (isEnglish) "Lively" else "热闹",
                            icon = Icons.Default.Bolt,
                            color = Color(0xFF9C27B0),
                            isActive = energyLevel >= 70,
                            modifier = Modifier.weight(1f)
                        ) { energyLevel = 85f }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filterChips) { chip ->
                            Surface(
                                color = energyColor.copy(alpha = 0.1f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, energyColor.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable {
                                    val q = if (isEnglish) "Find a $chip place in CLT with $energyLevelLabel vibe" else "找带有 $chip 氛围的 $energyLevelLabel 地方"
                                    viewModel.sendMessage(q)
                                    scope.launch {
                                        // Scroll to Expert Intel area (index 5)
                                        listState.animateScrollToItem(5)
                                    }
                                }
                            ) {
                                Text(
                                    text = chip,
                                    color = energyColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Latest Response
            lastAIMessage?.let { msg ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isEnglish) "Expert Intel" else "专家分析",
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                val displayText = msg.text.split("MATCH_SCORE_JSON")[0].trim()
                                Text(
                                    text = displayText,
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp,
                                    color = Color.White.copy(alpha = 0.95f)
                                )

                                // Visual Decision Cards
                                msg.extraData?.let { extra ->
                                    val type = extra["type"] as? String
                                    Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    when (type) {
                                        "worth_it" -> WorthItScorecardView(data = extra, isEnglish = isEnglish)
                                        "reality_check" -> RealityCheckDashboardView(data = extra, isEnglish = isEnglish)
                                        "rent_analysis" -> RentAnalysisCardView(data = extra, isEnglish = isEnglish)
                                        "the_scene" -> TheSceneDashboardView(data = extra, isEnglish = isEnglish)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Matches
            if (recommendations.isNotEmpty()) {
                item {
                    Text(
                        text = if (isEnglish) "Top Matches" else "最佳匹配",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                items(recommendations) { rec ->
                    RecommendationCard(rec, isEnglish)
                }
            }

            // 6. Quick Search
            item {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column {
                        Text(
                            text = if (isEnglish) "Quick Search" else "快速搜索",
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isEnglish) "Ask about safety, work, or investment." else "询问关于安全、工作或投资的问题。",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    sampleQuestions.forEach { question ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                var finalPrompt = question
                                if (savedHomeAddress.isNotEmpty()) {
                                    finalPrompt = finalPrompt.replace("[your address]", savedHomeAddress)
                                                             .replace("[address]", savedHomeAddress)
                                }
                                viewModel.sendMessage(finalPrompt)
                                // Scroll handled by LaunchedEffect(lastAIMessage)
                            },
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(text = question, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun VibePulseChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (isActive) color.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) color else Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.White else color.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RecommendationCard(rec: Recommendation, isEnglish: Boolean) {
    val scoreColor = when {
        rec.score >= 90 -> Color(0xFF4CAF50)
        rec.score >= 70 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo
            if (rec.imageUrl != null) {
                AsyncImage(
                    model = rec.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(80.dp).background(Color(0xFF007AFF).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF007AFF))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = rec.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    rec.price?.let {
                        Text(text = it, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Text(text = rec.reason, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 2)
                
                Spacer(Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Score Badge
                    Surface(color = scoreColor.copy(alpha = 0.1f), shape = CircleShape) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = scoreColor, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = if (isEnglish) "${rec.score}% Match" else "${rec.score}% 匹配", color = scoreColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Rating
                    rec.rating?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = it, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = rec.score / 100f, modifier = Modifier.size(40.dp), color = scoreColor, strokeWidth = 3.dp, trackColor = Color.White.copy(alpha = 0.1f))
                Text(text = "${rec.score}%", color = scoreColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

