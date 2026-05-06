package com.quantumproperty.qcai.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import com.quantumproperty.qcai.data.AppLanguage

@Composable
fun HomeScreen(viewModel: TeacherViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH

    val dailyBrief by viewModel.currentDailyBrief.collectAsState(initial = null)
    
    LaunchedEffect(Unit) { 
        viewModel.fetchDailyBrief() 
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // High-Tech Ambient Blurs (Blue/Purple theme)
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF007AFF).copy(alpha = 0.15f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(100f, 100f),
                    radius = 900f
                ),
                radius = 900f
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. HEADER: The Pulse
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = when {
                                isSpanish -> "Vibras Charlotte AI"
                                isEnglish -> "Charlotte AI Vibe"
                                else -> "夏洛特 AI 氛围"
                            },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                isSpanish -> "Explorar QCAI"
                                isEnglish -> "Ask QCAI"
                                else -> "咨询 QCAI"
                            },
                            color = Color(0xFF007AFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.fetchDailyBrief(true) }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(80.dp)) // Make room for global icons
                    }
                }
            }
            
            // 2. CONCIERGE SEARCH (Ask QCAI)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        viewModel.setSelectedTab(1) // Switch to Business Hub
                    },
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF007AFF))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = when {
                                isSpanish -> "Pregunta a QCAI..."
                                isEnglish -> "Ask QCAI anything..."
                                else -> "询问 QCAI 任何问题..."
                            },
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // 3. QUICK TILES
            item {
                val language by viewModel.appLanguage.collectAsState()
                val context = LocalContext.current
                
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        QuickTile(
                            icon = Icons.Default.Engineering, // Hire a Pro
                            label = when(language) {
                                AppLanguage.CHINESE -> "聘请专家"
                                AppLanguage.SPANISH -> "Contratar Pro"
                                else -> "Hire a Pro"
                            },
                            color = Color(0xFF007AFF),
                            modifier = Modifier.weight(1f)
                        ) { 
                             viewModel.openProfessionalProfile(context)
                        }
                        
                        QuickTile(
                            icon = Icons.Default.ShoppingCart, // Shop AI Gear
                            label = when(language) {
                                AppLanguage.CHINESE -> "AI 装备"
                                AppLanguage.SPANISH -> "Tienda AI"
                                else -> "Shop AI Gear"
                            },
                            color = Color(0xFFAF52DE), // Purple
                            modifier = Modifier.weight(1f)
                        ) { 
                             viewModel.setSelectedTab(1) // Business Hub
                        }
                    }
                }
            }

            // 4. DAILY BRIEFING & AI NEWS
            item {
                val aiNewsArticles by viewModel.aiNewsArticles.collectAsState()
                val context = LocalContext.current
                
                LaunchedEffect(Unit) {
                    viewModel.fetchAINewsArticles()
                }

                if (aiNewsArticles.isNotEmpty()) {
                    AINewsCard(
                        articles = aiNewsArticles,
                        language = appLanguage,
                        onReadMoreClick = { url ->
                            com.quantumproperty.qcai.utils.BrowserUtils.openURL(context, url)
                        }
                    )
                } else if (dailyBrief == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            
            // 5. HERO CARD (AI Strategy)
            item {
                val context = LocalContext.current
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.openAIRoadmap(context) }, 
                    color = Color(0xFF6200EE),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isSpanish -> "Estrategia de IA"
                                    isEnglish -> "AI Strategy Roadmap"
                                    else -> "AI 战略规划"
                                },
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = when {
                                    isSpanish -> "Obtenga un plan personalizado para su negocio."
                                    isEnglish -> "Get a custom plan for your business."
                                    else -> "为您的业务获取定制方案。"
                                },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun QuickTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(56.dp).clickable { onClick() },
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
             overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AINewsCard(
    articles: List<com.quantumproperty.qcai.data.AINewsArticle>,
    language: AppLanguage,
    onReadMoreClick: (String) -> Unit
) {
    if (articles.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    val topArticle = articles.first()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        language == AppLanguage.SPANISH -> "Pulso de Noticias AI"
                        language == AppLanguage.CHINESE -> "AI 新闻脉搏"
                        else -> "AI News Pulse"
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(Modifier.height(12.dp))

            if (!isExpanded) {
                // Collapsed: Show only top article
                Text(
                    text = topArticle.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = topArticle.summary,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                // Expanded: Show full list
                articles.forEachIndexed { index, article ->
                    if (index > 0) {
                        Spacer(Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(Modifier.height(16.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF007AFF).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = article.industry.uppercase(),
                                    color = Color(0xFF007AFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            // Impact Score Dots
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(5) { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (i < article.impactScore) Color(0xFFAF52DE) else Color.White.copy(alpha = 0.1f),
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(4.dp))
                         
                        Text(
                            text = article.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = article.summary,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // Charlotte Impact Box
                        Surface(
                            color = Color(0xFFAF52DE).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFAF52DE).copy(alpha = 0.3f))
                        ) {
                             Column(modifier = Modifier.padding(12.dp)) {
                                 Text(
                                     text = when {
                                         language == AppLanguage.SPANISH -> "IMPACTO EN CHARLOTTE"
                                         language == AppLanguage.CHINESE -> "对夏洛特的影响"
                                         else -> "CHARLOTTE IMPACT"
                                     },
                                     color = Color(0xFFAF52DE),
                                     fontSize = 10.sp,
                                     fontWeight = FontWeight.Bold
                                 )
                                 Spacer(Modifier.height(2.dp))
                                 Text(
                                     text = article.charlotteImpact,
                                     color = Color.White.copy(alpha = 0.9f),
                                     fontSize = 12.sp
                                 )
                             }
                        }
                    }
                }

            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    val url = when(language) {
                        AppLanguage.CHINESE -> "https://qcai-net.github.io/ainews/index_CN.html"
                        AppLanguage.SPANISH -> "https://qcai-net.github.io/ainews/index_ES.html"
                        else -> "https://qcai-net.github.io/ainews/index.html"
                    }
                    onReadMoreClick(url) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        language == AppLanguage.SPANISH -> "Leer resumen completo en la web"
                        language == AppLanguage.CHINESE -> "在网页上阅读完整摘要"
                        else -> "Read Full Digest on Web"
                    },
                    color = Color.White
                )
            }
        }
    }
}
