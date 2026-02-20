package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import com.quantumproperty.qcai.ui.viewmodel.MarketplaceViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.quantumproperty.qcai.data.AppLanguage
import com.quantumproperty.qcai.utils.BrowserUtils
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.result.launch

@Composable
fun BusinessHubScreen(viewModel: TeacherViewModel) {
    val marketplaceViewModel: MarketplaceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val marketItems by marketplaceViewModel.items.collectAsState()
    val isMarketLoading by marketplaceViewModel.isLoading.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        marketplaceViewModel.loadItems()
    }
    
    val isRecording by viewModel.isRecording.collectAsState()

    val messages by viewModel.messages.collectAsState()
    val lastAIMessage = messages.lastOrNull { !it.isUser && !it.isHidden }

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
            val prompt = if (isEnglish) "Please analyze this image for business insights." else "请分析这张图片的商业价值。"
            viewModel.sendMessage(prompt, image = bitmap)
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // High-Tech Ambient Blurs (Gold/Orange theme for Business)
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF9500).copy(alpha = 0.12f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(0f, size.height),
                    radius = 900f
                ),
                radius = 900f
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 0. SPACER FOR GLOBAL ICONS
            item { Spacer(Modifier.height(40.dp)) }

            // 1. HERO SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isSpanish -> "Centro de Negocios"
                                isEnglish -> "Business Hub"
                                else -> "商务中心"
                            },
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when {
                                isSpanish -> "Su puerta de entrada a la economía de IA de CLT."
                                isEnglish -> "Your gateway to Charlotte's AI economy."
                                else -> "您进入夏洛特 AI 经济的门户。"
                            },
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    Surface(
                        color = Color(0xFFFF9500).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(56.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BusinessCenter, null, tint = Color(0xFFFF9500), modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            // 1.5 AI Response (Business Assistant)
            lastAIMessage?.let { msg ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    isSpanish -> "Asistente de Negocios QCAI"
                                    isEnglish -> "QCAI Business Assistant"
                                    else -> "QCAI 商务助手"
                                },
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(16.dp))
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
                            }
                        }
                    }
                }
            }

            // 2. PROFESSIONAL SERVICES (Grouping header)
            item {
                Divider(color = Color.White.copy(alpha = 0.1f))
            }

            // 2. VERIFIED AGENCIES (Seed)
            // 1. Verified Professionals Section
            item {
                val professionals by viewModel.verifiedProfessionals.collectAsState()
                
                Text(
                    text = when {
                        isSpanish -> "Profesionales de IA Verificados"
                        isEnglish -> "Verified AI Professionals"
                        else -> "认证 AI 专家"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (professionals.isEmpty()) {
                    Text(
                        text = if (isEnglish) "Loading experts..." else "正在加载专家...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(professionals) { pro ->
                            ProfessionalCard(
                                name = pro.name,
                                title = pro.title,
                                onClick = {
                                    viewModel.openProfessionalProfile(context, pro.id)
                                }
                            )
                        }
                    }
                }
            }

            // 3. HARDWARE SHOP TEASER
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFF9500).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).clickable { BrowserUtils.openURL(context, "https://quantumpropertyllc.github.io/aihardware/") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isSpanish -> "Estaciones de Trabajo IA"
                                    isEnglish -> "Pre-build AI Workstations"
                                    else -> "预装 AI 工作站"
                                },
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    isSpanish -> "Obtén las herramientas para el futuro."
                                    isEnglish -> "Get the tools to build the future."
                                    else -> "获取构建未来的工具。"
                                },
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFFF9500))
                    }
                }
            }

            // 4. DIY HARDWARE SHOP
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    color = Color(0xFF34C759).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34C759).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).clickable { BrowserUtils.openURL(context, "https://quantumpropertyllc.github.io/aihardware/diy.html") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isSpanish -> "Estaciones IA DIY"
                                    isEnglish -> "DIY AI Workstations"
                                    else -> "DIY AI 工作站"
                                },
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    isSpanish -> "Construye tu propio equipo personalizado."
                                    isEnglish -> "Build your own custom rig."
                                    else -> "构建您自己的自定义设备。"
                                },
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = when {
                                isSpanish -> "Guía"
                                isEnglish -> "Guide"
                                else -> "指南"
                            },
                            color = Color(0xFF34C759),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // 5. MARKETPLACE INTEGRATION
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isSpanish -> "Mercado IA"
                                isEnglish -> "AI Marketplace"
                                else -> "AI 市场"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                         Text(
                            text = when {
                                isSpanish -> "Ver Todo >"
                                isEnglish -> "View All >"
                                else -> "查看全部 >"
                            },
                            color = Color(0xFF007AFF),
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { 
                                viewModel.showMarketplaceView()
                            }
                        )
                    }
                    Text(
                        text = when {
                            isSpanish -> "Comprar, Contratar o Alquilar — Todo AI"
                            isEnglish -> "Buy, Hire, or Rent — Everything AI"
                            else -> "购买、雇佣或租用 — AI一切"
                        },
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
            
            // Marketplace Preview List
             item {
                 if (isMarketLoading) {
                     Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator(color = Color(0xFFFF9500))
                     }
                 } else if (marketItems.isEmpty()) {
                     Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                         Text(
                             text = when {
                                 isSpanish -> "No se encontraron artículos"
                                 isEnglish -> "No items found"
                                 else -> "暂无商品"
                             },
                             color = Color.White.copy(alpha = 0.5f)
                         )
                     }
                 } else {
                     LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                         items(marketItems.take(5)) { item ->
                             MarketplacePreviewCard(item) {
                                 // Handle marketplace item click (e.g., share/email or open details)
                                 // For now, let's open a generic contact or search for it
                                 BrowserUtils.openURL(context, "https://www.google.com/search?q=${item.title}")
                             }
                         }
                     }
                 }
             }
             
             // 6. COMMUNITY EVENTS
             item {
                 Column(modifier = Modifier.fillMaxWidth().clickable { viewModel.showEventsView() }) {
                     Row(
                         modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                         horizontalArrangement = Arrangement.SpaceBetween,
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Text(
                             text = when {
                                 isSpanish -> "Eventos de IA Comunitarios"
                                 isEnglish -> "Community AI Events"
                                 else -> "社区 AI 活动"
                             },
                             color = Color.White,
                             fontWeight = FontWeight.Bold,
                             fontSize = 18.sp
                         )
                         Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.5f))
                     }
                     
                     Surface(
                         modifier = Modifier.fillMaxWidth().height(100.dp),
                         color = Color(0xFF9C27B0).copy(alpha = 0.15f),
                         shape = RoundedCornerShape(16.dp),
                         border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.3f))
                     ) {
                         Row(
                             modifier = Modifier.padding(16.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Box(
                                 modifier = Modifier.size(48.dp).background(Color(0xFF9C27B0), androidx.compose.foundation.shape.CircleShape),
                                 contentAlignment = Alignment.Center
                             ) {
                                 Icon(Icons.Default.Event, null, tint = Color.White)
                             }
                             Spacer(Modifier.width(16.dp))
                             Column {
                                 Text(
                                     text = when {
                                         isSpanish -> "Próximos Eventos y Reuniones"
                                         isEnglish -> "Upcoming Events & Meetups"
                                         else -> "近期活动与聚会"
                                     },
                                     color = Color.White,
                                     fontWeight = FontWeight.Bold,
                                     fontSize = 16.sp
                                 )
                                 Text(
                                     text = when {
                                         isSpanish -> "Conéctate con la Comunidad de IA de Charlotte"
                                         isEnglish -> "Connect with Charlotte’s AI Community"
                                         else -> "链接夏洛特的 AI 社区"
                                     },
                                     color = Color.White.copy(alpha = 0.7f),
                                     fontSize = 12.sp
                                 )
                             }
                         }
                     }
                 }
             }
        }
        
            TextInputArea(
                onSend = { viewModel.sendMessage(it) },
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
                    isSpanish -> "Pregunta a QCAI Business..."
                    isEnglish -> "Ask QCAI Business..."
                    else -> "咨询 QCAI 商务..."
                }
            )

        }
    }
}

@Composable
fun MarketplacePreviewCard(item: com.quantumproperty.qcai.data.MarketplaceItemModel, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.width(160.dp).height(200.dp).clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                 if (item.imageUrl != null) {
                     AsyncImage(
                        model = item.imageUrl,
                        contentDescription = "Item Image",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                     )
                 } else {
                     Icon(Icons.Default.ShoppingCart, null, tint = Color.White.copy(alpha = 0.3f))
                 }
                 
                 // Price tag overlay
                 Box(
                     modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                 ) {
                     Text("$${String.format("%.0f", item.price)}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                 }
            }
            
            Column(modifier = Modifier.padding(8.dp)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(item.description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 2, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun ProfessionalCard(name: String, title: String, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.width(140.dp).height(120.dp).clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(32.dp).background(Color(0xFF007AFF), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Column {
                Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(text = title, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1)
            }
            
            Surface(
                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "VERIFIED",
                    color = Color(0xFF4CAF50),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
