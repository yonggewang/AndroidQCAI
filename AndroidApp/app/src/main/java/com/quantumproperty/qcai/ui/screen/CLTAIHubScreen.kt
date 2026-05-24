package com.quantumproperty.qcai.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.quantumproperty.qcai.data.AppLanguage
import com.quantumproperty.qcai.data.AITopic
import com.quantumproperty.qcai.ui.viewmodel.MarketplaceViewModel
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import com.quantumproperty.qcai.utils.BrowserUtils
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun CLTAIHubScreen(viewModel: TeacherViewModel) {
    val marketplaceViewModel: MarketplaceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.setTopic(AITopic.BUSINESS)
        marketplaceViewModel.loadItems()
        viewModel.fetchAINewsArticles()
        viewModel.fetchDailyBrief()
    }
    
    val isRecording by viewModel.isRecording.collectAsState()
    val dailyBrief by viewModel.currentDailyBrief.collectAsState(initial = null)
    val aiNewsArticles by viewModel.aiNewsArticles.collectAsState()
    
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lastAIMessage = messages.lastOrNull { !it.isUser && !it.isHidden }
    
    var isAINewsExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.toggleRecording()
        else viewModel.showError(if (isEnglish) "Microphone permission required" else "需要麦克风权限")
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val prompt = if (isEnglish) "Please analyze this image for business insights." else "请分析这张图片商业价值。"
            viewModel.sendMessage(prompt, image = bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try { cameraLauncher.launch() } catch (e: Exception) { viewModel.showError(e.localizedMessage ?: "Camera error") }
        } else {
            viewModel.showError(if (isEnglish) "Camera permission required" else "需要相机权限")
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Header with Language & Profile (Static)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray.copy(alpha = 0.02f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val sdf = SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault())
                    Text(
                        text = sdf.format(Date()).uppercase(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "CLT AI Hub",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        dailyBrief?.weather?.let { weather ->
                            Row(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Match weather desc to icon heuristically
                                val d = weather.desc.lowercase()
                                val icon = when {
                                    d.contains("rain") -> Icons.Default.Cloud
                                    d.contains("cloud") -> Icons.Default.Cloud
                                    d.contains("clear") || d.contains("sun") -> Icons.Default.WbSunny
                                    else -> Icons.Default.CloudQueue
                                }
                                Icon(icon, contentDescription = "Weather", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${weather.temp.toInt()}°",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
                
                // User Profile Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.LightGray, CircleShape)
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp), // Space for bottom input
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Expert Intel (AI response / loading card)
                if (isLoading || lastAIMessage != null) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when {
                                            isSpanish -> "Información de Expertos"
                                            isEnglish -> "Expert Intel"
                                            else -> "专家分析"
                                        },
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                if (!isLoading) {
                                    IconButton(
                                        onClick = { viewModel.clearMessages() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Transparent,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF007AFF).copy(alpha = 0.1f),
                                                    Color(0xFFAF52DE).copy(alpha = 0.1f)
                                                )
                                            )
                                        )
                                        .padding(24.dp)
                                ) {
                                    if (isLoading) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF007AFF),
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 3.dp
                                            )
                                            Text(
                                                text = when {
                                                    isSpanish -> "QCAI está pensando..."
                                                    isEnglish -> "QCAI is thinking..."
                                                    else -> "QCAI 正在思考..."
                                                },
                                                color = Color.Black.copy(alpha = 0.8f),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            val displayText = lastAIMessage!!.text.split("MATCH_SCORE_JSON")[0].trim()
                                            Text(
                                                text = displayText,
                                                fontSize = 15.sp,
                                                lineHeight = 24.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. AI News Pulse (Expandable)
                if (aiNewsArticles.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isAINewsExpanded = !isAINewsExpanded }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "AI News Pulse" else if (isSpanish) "Noticias de IA" else "AI 简报脉搏",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Icon(
                                        imageVector = if (isAINewsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle",
                                        tint = Color(0xFF007AFF)
                                    )
                                }

                                if (!isAINewsExpanded) {
                                    val topArticle = aiNewsArticles.first()
                                    Column(
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(topArticle.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(topArticle.summary, fontSize = 14.sp, color = Color.Gray, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                        
                                        OutlinedButton(
                                            onClick = { BrowserUtils.openURL(context, "https://qcai-net.github.io/ainews/ainews.html") },
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, Color(0xFF007AFF)),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(if (isEnglish) "Read more" else "阅读更多", color = Color(0xFF007AFF), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                } else {
                                    Column {
                                        aiNewsArticles.forEachIndexed { index, article ->
                                            if (index > 0) Divider(modifier = Modifier.padding(horizontal = 16.dp))
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(article.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                                Text(article.summary, fontSize = 14.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. AI Strategy Roadmap (Hero)
                item {
                    val gradient = Brush.linearGradient(colors = listOf(Color(0xFF007AFF), Color(0xFFAF52DE)))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(gradient, RoundedCornerShape(16.dp))
                            .clickable { viewModel.openAIRoadmap(context) }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (isEnglish) "AI Strategy Roadmap" else if (isSpanish) "Hoja de Ruta de IA" else "AI 战略路线图",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    if (isEnglish) "Plan your AI transformation journey." else "规划您的 AI 转型之旅。",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Icon(Icons.Default.ArrowCircleRight, contentDescription = "Go", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                // 5. AI Resource Center
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "AI Resource Center" else if (isSpanish) "Centro de Recursos IA" else "AI 资源中心",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                ResourceRow(
                                    title = if (isEnglish) "Professional Services" else "专业服务",
                                    subtitle = if (isEnglish) "Hire vetted AI experts to build your solutions." else "聘请经过认证的 AI 专家。",
                                    icon = Icons.Default.Badge,
                                    iconColor = Color(0xFF007AFF),
                                    buttonTitle = if (isEnglish) "Connect" else "联系",
                                    action = { viewModel.openProfessionalProfile(context) }
                                )
                                Divider(modifier = Modifier.padding(start = 60.dp))
                                ResourceRow(
                                    title = if (isEnglish) "Local AI Jobs" else "本地 AI 职位",
                                    subtitle = if (isEnglish) "Discover the latest AI opportunities near you." else "发现附近的 AI 工作机会。",
                                    icon = Icons.Default.Work,
                                    iconColor = Color(0xFFFF9500),
                                    buttonTitle = if (isEnglish) "View" else "查看",
                                    action = { BrowserUtils.openURL(context, "https://qcai-net.github.io/ainews/aijobs.html") }
                                )
                                Divider(modifier = Modifier.padding(start = 60.dp))
                                ResourceRow(
                                    title = if (isEnglish) "LLM Expert Knowledge" else "LLM 专家知识库",
                                    subtitle = if (isEnglish) "Models, hardware & performance parameters guide." else "模型、硬件与性能指南。",
                                    icon = Icons.Default.AutoStories,
                                    iconColor = Color(0xFF007AFF),
                                    buttonTitle = if (isEnglish) "Learn" else "学习",
                                    action = { BrowserUtils.openURL(context, "https://qcai-net.github.io/llm/llminfo.html") }
                                )
                            }
                        }
                    }
                }

                // 6. AI Hardware Shop
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "AI Hardware Shop" else if (isSpanish) "Tienda de Hardware IA" else "AI 硬件商店",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                ResourceRow(
                                    title = if (isEnglish) "Pre-built AI Workstations" else "预配置 AI 工作站",
                                    subtitle = if (isEnglish) "Get the tools to power your local LLMs." else "获取支持本地 LLM 的工具。",
                                    icon = Icons.Default.Memory,
                                    iconColor = Color(0xFFAF52DE),
                                    buttonTitle = if (isEnglish) "Shop" else "选购",
                                    action = { BrowserUtils.openURL(context, "https://qcai-net.github.io/aihardware/") }
                                )
                                Divider(modifier = Modifier.padding(start = 60.dp))
                                ResourceRow(
                                    title = if (isEnglish) "DIY AI Workstations" else "DIY AI 工作站",
                                    subtitle = if (isEnglish) "Build your own dedicated rig for AI workloads." else "为 AI 工作负载组装专用设备。",
                                    icon = Icons.Default.Build,
                                    iconColor = Color(0xFF34C759),
                                    buttonTitle = if (isEnglish) "Guide" else "指南",
                                    buttonColor = Color(0xFF34C759),
                                    action = { BrowserUtils.openURL(context, "https://qcai-net.github.io/aihardware/diy.html") }
                                )
                            }
                        }
                    }
                }

                // 7. Community & Marketplace
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LargeNavigationRow(
                            title = if (isEnglish) "Community AI Events" else "社区 AI 活动",
                            subtitle = if (isEnglish) "Upcoming local meetups, workshops & seminars." else "即将举行的本地聚会、研讨会。",
                            icon = Icons.Default.Event,
                            iconColor = Color(0xFFAF52DE),
                            action = { viewModel.showEventsView() }
                        )
                        LargeNavigationRow(
                            title = if (isEnglish) "AI Marketplace" else "AI 市场",
                            subtitle = if (isEnglish) "Buy, rent, or hire local AI resources." else "购买、租赁或雇佣本地 AI 资源。",
                            icon = Icons.Default.Storefront,
                            iconColor = Color(0xFFFF9500),
                            action = { viewModel.showMarketplaceView() }
                        )
                    }
                }
            }
        }

        // Bottom Search Bar / Text Input Area
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            TextInputArea(
                onSend = { viewModel.sendMessage(it, explicitTopic = AITopic.BUSINESS) },
                onCameraClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                          cameraLauncher.launch()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onMicClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.toggleRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                isRecording = isRecording,
                placeholder = if (isEnglish) "Ask QCAI..." else if (isSpanish) "Pregunta a QCAI..." else "咨询 QCAI..."
            )
        }
    }
}

@Composable
fun ResourceRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    buttonTitle: String,
    buttonColor: Color = Color(0xFF007AFF),
    action: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { action() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            color = buttonColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = buttonTitle,
                color = buttonColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun LargeNavigationRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    action: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { action() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Forward", tint = Color.LightGray)
        }
    }
}
