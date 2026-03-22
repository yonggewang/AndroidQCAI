package com.quantumproperty.qcai.ui.screen

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.quantumproperty.qcai.utils.BrowserUtils
import androidx.compose.ui.platform.LocalContext
import com.quantumproperty.qcai.data.ChatMessage
import com.quantumproperty.qcai.data.AITopic
import com.quantumproperty.qcai.data.ChatConfigManager
import com.quantumproperty.qcai.data.AppLanguage
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch

@Composable
fun CLTVibeView(viewModel: TeacherViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH
    val context = LocalContext.current
    val recommendations by viewModel.recommendations.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var energyLevel by remember { mutableStateOf(50f) }
    val lastAIMessage = messages.lastOrNull { !it.isUser && !it.isHidden }
    
    // Address State
    var savedHomeAddress by remember { mutableStateOf(com.quantumproperty.qcai.data.PreferenceManager.homeAddress) }
    var isShowingAddressEditor by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    
    val dailyBrief by viewModel.currentDailyBrief.collectAsState(initial = null)
    val chatConfigSuggestions by ChatConfigManager.instance.suggestions.collectAsState()
    var worthItInput by remember { mutableStateOf("") }
    
    val isRecording by viewModel.isRecording.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.toggleRecording()
        } else {
            val errorMsg = if (isEnglish) "Microphone permission required" else "需要麦克风权限才能进行语音对话"
            viewModel.showError(errorMsg)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val prompt = if (isEnglish) "Please analyze this image for local vibe insights." else "请结合本地氛围分析这张图片。"
            viewModel.sendMessage(prompt, image = bitmap, explicitTopic = AITopic.CLT_VIBE)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                cameraLauncher.launch()
            } catch (e: Exception) {
                viewModel.showError(e.localizedMessage ?: "Camera error")
            }
        } else {
            val errorMsg = if (isEnglish) "Camera permission required" else "需要相机权限"
            viewModel.showError(errorMsg)
        }
    }
    
    LaunchedEffect(Unit) { 
        viewModel.setTopic(AITopic.CLT_VIBE)
        launch { ChatConfigManager.instance.fetchConfig() }
        launch { viewModel.fetchDailyBrief() }
    }
    
    // Auto-Scroll to Expert Intel when a new AI message arrives
    LaunchedEffect(lastAIMessage) {
        if (lastAIMessage != null) {
            kotlinx.coroutines.delay(300) 
            listState.animateScrollToItem(3) // Adjusted index likely
        }
    }


    val energyLevelLabel = when {
        energyLevel < 30 -> when {
            isSpanish -> "Tranquilo"
            isEnglish -> "Chill"
            else -> "宁静"
        }
        energyLevel < 70 -> when {
            isSpanish -> "Moderado"
            isEnglish -> "Moderate"
            else -> "适中"
        }
        else -> when {
            isSpanish -> "Animado"
            isEnglish -> "Lively"
            else -> "热闹"
        }
    }



    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // High-Tech Ambient Blurs (Purple/Pink theme for Vibe)
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF9D50BB).copy(alpha = 0.15f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    radius = 800f
                ),
                radius = 800f
            )
        }


        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 0. SPACER FOR GLOBAL ICONS
            item { Spacer(Modifier.height(40.dp)) }

            // 1. CITY PULSE HERO
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isSpanish -> "Pulso de la Ciudad"
                                    isEnglish -> "City Pulse"
                                    else -> "城市脉搏"
                                },
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = when {
                                    isSpanish -> "Experimenta CLT con información de IA en tiempo real."
                                    isEnglish -> "Experience CLT through real-time AI insights."
                                    else -> "通过实时 AI 洞察体验夏洛特。"
                                },
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                        Surface(
                            color = Color(0xFF9D50BB).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(56.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9D50BB).copy(alpha = 0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LocationCity, null, tint = Color(0xFF9D50BB), modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f))
                }
            }

            // 2. Charlotte Today (Daily Briefing)
            dailyBrief?.let { brief ->
                item {
                    DailyBriefingHeader(data = brief, appLanguage = appLanguage)
                }
            }

            // 2. Expert Intel (AI Response / Loading)
            if (isLoading || lastAIMessage != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    isSpanish -> "Información de Expertos"
                                    isEnglish -> "Expert Intel"
                                    else -> "专家分析"
                                },
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF9D50BB), modifier = Modifier.size(16.dp))
                        }
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent,
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9D50BB).copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF2196F3).copy(alpha = 0.2f),
                                                Color(0xFFE91E63).copy(alpha = 0.2f)
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
                                            color = Color(0xFF9D50BB),
                                            modifier = Modifier.size(32.dp),
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = when {
                                                isSpanish -> "QCAI está pensando..."
                                                isEnglish -> "QCAI is thinking..."
                                                else -> "QCAI 正在思考..."
                                            },
                                            color = Color.White.copy(alpha = 0.9f),
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
                                            color = Color.White.copy(alpha = 0.95f)
                                        )
        
                                        // Visual Decision Cards
                                        lastAIMessage.extraData?.let { extra ->
                                            val type = extra["type"] as? String
                                            Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))
                                            
                                            when (type) {
                                                "worth_it" -> WorthItScorecardView(data = extra, isSpanish = isSpanish, isEnglish = isEnglish)
                                                "reality_check" -> RealityCheckDashboardView(data = extra, isSpanish = isSpanish, isEnglish = isEnglish)
                                                "rent_analysis" -> RentAnalysisCardView(data = extra, isSpanish = isSpanish, isEnglish = isEnglish)
                                                "the_scene" -> TheSceneDashboardView(data = extra, isSpanish = isSpanish, isEnglish = isEnglish)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. My Neighborhood (Home Address)
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
                                text = when {
                                    isSpanish -> "Mi Vecindario"
                                    isEnglish -> "My Neighborhood"
                                    else -> "我的街区"
                                },
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = if (savedHomeAddress.isEmpty()) (
                                    when {
                                        isSpanish -> "Fijar Casa"
                                        isEnglish -> "Set Home"
                                        else -> "设置地址"
                                    }
                                ) else (
                                    when {
                                        isSpanish -> "Editar"
                                        isEnglish -> "Edit"
                                        else -> "修改"
                                    }
                                ),
                                color = Color(0xFF9D50BB),
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
                                placeholder = { 
                                    Text(when {
                                        isSpanish -> "Ingrese su dirección..."
                                        isEnglish -> "Enter your CLT address..."
                                        else -> "输入您的夏洛特地址..."
                                    }) 
                                },
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                            Button(
                                onClick = { isShowingAddressEditor = false },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D50BB))
                            ) {
                                Text(if (isEnglish) "Save" else if (isSpanish) "Guardar" else "保存")
                            }
                        }
                    }
                }
            }

            // 4. Quick Search (Suggestions)
            item {
                var isExpanded by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isSpanish -> "Búsqueda Rápida"
                                isEnglish -> "Quick Search"
                                else -> "快速搜索"
                            },
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    
                    if (isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chatConfigSuggestions.forEach { suggestion ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        viewModel.sendMessage("[$energyLevelLabel Energy] ${suggestion.prompt.localized(isEnglish)}", explicitTopic = AITopic.CLT_VIBE)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(text = suggestion.emoji, fontSize = 20.sp)
                                        Text(
                                            text = suggestion.prompt.localized(isEnglish),
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.ArrowCircleUp, contentDescription = null, tint = Color.White.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Select Vibe (Compact)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Vibe:" else if (isSpanish) "Vibe:" else "氛围:",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VibePulseChip(
                            label = when {
                                isSpanish -> "Tranquilo"
                                isEnglish -> "Chill"
                                else -> "宁静"
                            },
                            icon = Icons.Default.Spa,
                            color = Color(0xFF00ACC1),
                            isActive = energyLevel < 30,
                            modifier = Modifier.weight(1f)
                        ) { energyLevel = 15f }

                        VibePulseChip(
                            label = when {
                                isSpanish -> "Moderado"
                                isEnglish -> "Mod"
                                else -> "适中"
                            },
                            icon = Icons.Default.Coffee,
                            color = Color(0xFF3F51B5),
                            isActive = energyLevel >= 30 && energyLevel < 70,
                            modifier = Modifier.weight(1f)
                        ) { energyLevel = 50f }

                        VibePulseChip(
                            label = when {
                                isSpanish -> "Animado"
                                isEnglish -> "Lively"
                                else -> "热闹"
                            },
                            icon = Icons.Default.Bolt,
                            color = Color(0xFF9C27B0),
                            isActive = energyLevel >= 70,
                            modifier = Modifier.weight(1f)
                        ) { energyLevel = 85f }
                    }
                }
            }

            // 6. Worth It? Analysis
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = when {
                                    isSpanish -> "¿Vale la pena? Análisis"
                                    isEnglish -> "Worth It? Analysis"
                                    else -> "值得吗？深度评价"
                                },
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    isSpanish -> "Reseñas y veredictos reales"
                                    isEnglish -> "Real reviews & verdicts"
                                    else -> "基于真实评价的避雷指南"
                                },
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    }

                    // Interactive Check Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextField(
                            value = worthItInput,
                            onValueChange = { worthItInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            placeholder = { 
                                Text(when {
                                    isSpanish -> "Buscar un lugar..."
                                    isEnglish -> "Check a place..."
                                    else -> "想查查哪里好玩？"
                                }, fontSize = 14.sp) 
                            },
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (worthItInput.isNotBlank()) {
                                    val prompt = "Is $worthItInput worth it? Give me a verdict and local hacks."
                                    viewModel.sendMessage("[$energyLevelLabel Energy] $prompt", explicitTopic = AITopic.CLT_VIBE)
                                    worthItInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.ArrowCircleRight, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                        }
                    }

                    // Trending
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val trends = if (isEnglish || isSpanish) listOf("Optimist Hall", "Camp North End", "The Giddy Goat") else listOf("中餐馆", "Optimist Hall", "Camp North End")
                        items(trends) { spot ->
                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.clickable {
                                    viewModel.sendMessage("[$energyLevelLabel Energy] Is $spot worth it?", explicitTopic = AITopic.CLT_VIBE)
                                }
                            ) {
                                Text(
                                    text = spot,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 7. The Scene
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF9C27B0).copy(alpha = 0.03f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isSpanish -> "La Escena"
                                        isEnglish -> "The Scene"
                                        else -> "社群氛围"
                                    },
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when {
                                        isSpanish -> "Eventos en tiempo real y ambiente social"
                                        isEnglish -> "Real-time events & social vibes"
                                        else -> "实时活动与社群动态"
                                    },
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(24.dp))
                        }

                        // Category Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val scenes = listOf("Tech", "Music", "Art", "Social", "Family")
                            items(scenes) { cat ->
                                Surface(
                                    color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.clickable {
                                        val prompt = if (isEnglish) "What's the $cat scene like in Charlotte?" else "夏洛特的 $cat 氛围怎么样？"
                                        viewModel.sendMessage("[$energyLevelLabel Energy] $prompt", explicitTopic = AITopic.CLT_VIBE)
                                    }
                                ) {
                                    Text(
                                        text = cat,
                                        color = Color(0xFF9C27B0),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.openTheScene() }, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    isSpanish -> "Encuentra tu Ambiente"
                                    isEnglish -> "Find Your Vibe"
                                    else -> "探索你的圈子"
                                },
                                color = Color(0xFF9C27B0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowCircleRight, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Legacy Sections (Guides & Services - moved lower or optional)
            item {
                Column {
                    Text(
                        text = when {
                            isSpanish -> "Guías del Vecindario"
                            isEnglish -> "Neighborhood Guides"
                            else -> "社区指南"
                        }, 
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val neighborhoods = com.quantumproperty.qcai.data.CityOSService.instance.getNeighborhoods()
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(neighborhoods) { hood ->
                            NeighborhoodCard(
                                name = hood.name, 
                                tag = hood.tag, 
                                icon = getVibeIcon(hood.icon), 
                                accentColor = Color(android.graphics.Color.parseColor(hood.color)),
                                onClick = { BrowserUtils.openURL(context, hood.url) }
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    val services = com.quantumproperty.qcai.data.CityOSService.instance.getCityServices()
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        services.forEach { service ->
                            ServiceShortcut(label = if (isEnglish) service.nameEn else service.nameZh, icon = getVibeIcon(service.icon), color = Color(android.graphics.Color.parseColor(service.color))) { BrowserUtils.openURL(context, service.url) }
                        }
                    }
                }
            }

            // 5. Matches
            if (recommendations.isNotEmpty()) {
                item {
                    Text(
                        text = when {
                            isSpanish -> "Mejores Coincidencias"
                            isEnglish -> "Top Matches"
                            else -> "最佳匹配"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                items(recommendations) { rec ->
                    RecommendationCard(rec, isEnglish)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
        
            TextInputArea(
                onSend = { viewModel.sendMessage(it, explicitTopic = AITopic.CLT_VIBE) },
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
                placeholder = when {
                    isSpanish -> "Pregunta a QCAI Vibe..."
                    isEnglish -> "Ask QCAI Vibe..."
                    else -> "咨询 QCAI 氛围..."
                }
            )

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
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) color else Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.White else color.copy(alpha = 0.8f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
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

@Composable
fun NeighborhoodCard(
    name: String,
    tag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.width(140.dp).height(100.dp).clickable { onClick() },
        color = accentColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = tag,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ServiceShortcut(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DailyBriefingHeader(
    data: com.quantumproperty.qcai.data.DailyBriefResponse, 
    appLanguage: com.quantumproperty.qcai.data.AppLanguage
) {
    var isExpanded by remember { mutableStateOf(false) }
    var newsExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = when (appLanguage) {
                com.quantumproperty.qcai.data.AppLanguage.SPANISH -> "Charlotte Hoy"
                com.quantumproperty.qcai.data.AppLanguage.ENGLISH -> "Charlotte Today"
                com.quantumproperty.qcai.data.AppLanguage.CHINESE -> "今日夏洛特"
            },
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = when (appLanguage) {
                    com.quantumproperty.qcai.data.AppLanguage.SPANISH -> "¡Buenos Días!"
                    com.quantumproperty.qcai.data.AppLanguage.ENGLISH -> "Good Morning!"
                    com.quantumproperty.qcai.data.AppLanguage.CHINESE -> "早上好！"
                },
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            
            data.weather?.let { w ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Text(
                        text = "${w.temp.toInt()}°",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Briefing Text
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
            color = Color(0xFF007AFF).copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.2f))
        ) {
            Text(
                text = data.briefingText,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(16.dp).animateContentSize(),
                maxLines = if (isExpanded) Int.MAX_VALUE else 3
            )
        }

        // Top Stories Link
        if (data.topNews.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { newsExpanded = !newsExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Newspaper, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (appLanguage) {
                            com.quantumproperty.qcai.data.AppLanguage.SPANISH -> "Historias Principales (${data.topNews.size})"
                            com.quantumproperty.qcai.data.AppLanguage.ENGLISH -> "Top Stories (${data.topNews.size})"
                            com.quantumproperty.qcai.data.AppLanguage.CHINESE -> "今日头条 (${data.topNews.size})"
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = if (newsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }

                if (newsExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        data.topNews.forEach { news ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    BrowserUtils.openURL(context, news.url)
                                },
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = news.headline,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = news.source,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getVibeIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when(name) {
        "Business" -> Icons.Default.Business
        "Brush" -> Icons.Default.Brush
        "DirectionsRun" -> Icons.Default.DirectionsRun
        "Restaurant" -> Icons.Default.Restaurant
        "Phone" -> Icons.Default.Phone
        "Delete" -> Icons.Default.Delete
        "DirectionsBus" -> Icons.Default.DirectionsBus
        "Description" -> Icons.Default.Description
        else -> Icons.Default.Place
    }
}

