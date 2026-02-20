package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import com.quantumproperty.qcai.data.HotToolItem
import com.quantumproperty.qcai.data.AppLanguage
import com.quantumproperty.qcai.utils.BrowserUtils
import androidx.compose.ui.platform.LocalContext

@Composable
fun AIToolkitScreen(viewModel: TeacherViewModel) {
    val hotList by viewModel.hotListItems.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val context = LocalContext.current
    
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH
    
    val coreTools = remember(appLanguage) {
        listOf(
            ToolkitItem(
                name = when {
                    isSpanish -> "Analista de Acciones"
                    isEnglish -> "Stock Analyst"
                    else -> "股票分析"
                }, 
                shortName = "Stock", 
                icon = Icons.Default.ShowChart, 
                color = Color(0xFF4CAF50)
            ) { 
                viewModel.setTopic(com.quantumproperty.qcai.data.AITopic.STOCK)
            },
            ToolkitItem(
                name = when {
                    isSpanish -> "Bienes Raíces"
                    isEnglish -> "Real Estate"
                    else -> "房地产"
                }, 
                shortName = "Estate", 
                icon = Icons.Default.Home, 
                color = Color(0xFF2196F3)
            ) { 
                 viewModel.showAIRealEstateTools()
            },
            ToolkitItem(
                name = when {
                    isSpanish -> "Identificador de Llamadas"
                    isEnglish -> "Caller ID"
                    else -> "来电查询"
                }, 
                shortName = "Lookup", 
                icon = Icons.Default.Phone, 
                color = Color(0xFFFF3B30)
            ) { 
                 BrowserUtils.openURL(context, "https://www.google.com/search?q=reverse+phone+lookup")
            }
        )
    }

    // Combine Core Tools and Hot List
    val allTools = remember(hotList, appLanguage) {
        val mappedHotList = hotList.mapIndexed { index, item ->
            ToolkitItem(
                name = when {
                    isSpanish -> if (item.spanishName.isNotEmpty()) item.spanishName else item.englishName
                    isEnglish -> item.englishName
                    else -> item.chineseName
                },
                shortName = item.englishName,
                icon = mapHotIcon(item.icon),
                color = getCycleColor(index),
                action = { 
                    BrowserUtils.openURL(context, item.url) 
                }
            )
        }
        coreTools + mappedHotList
    }

    if (selectedTopic == com.quantumproperty.qcai.data.AITopic.STOCK) {
        StockScreen(
            onBack = { viewModel.setTopic(com.quantumproperty.qcai.data.AITopic.NONE) }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
             // High-Tech Ambient Blurs (Cyan/Blue for Tech)
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                 drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.1f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        radius = 800f
                    ),
                    radius = 800f
                )
            }
    
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = when {
                                isSpanish -> "Caja de Herramientas"
                                isEnglish -> "AI Toolkit"
                                else -> "AI 工具箱"
                            },
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                isSpanish -> "Utilidades de Usuario Avanzado"
                                isEnglish -> "Power User Utilities"
                                else -> "高级用户工具"
                            },
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
    
                // Unified Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allTools) { tool ->
                         ToolCard(tool)
                    }
                }
            }
        }
    }
}

// Helper functions for mapping dynamic items
fun mapHotIcon(iconName: String): ImageVector {
    return when(iconName.lowercase()) {
        "chart", "stock", "showchart" -> Icons.Default.ShowChart
        "home", "estate" -> Icons.Default.Home
        "phone", "call" -> Icons.Default.Phone
        "web", "language" -> Icons.Default.Language
        "business" -> Icons.Default.BusinessCenter
        "star" -> Icons.Default.Star
        "map" -> Icons.Default.Map
        "search" -> Icons.Default.Search
        "person" -> Icons.Default.Person
        "build", "tools" -> Icons.Default.Build
        else -> Icons.Default.Apps // Default fallback
    }
}

fun getCycleColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF9C27B0), // Purple
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFFC107), // Amber
        Color(0xFF673AB7)  // Deep Purple
    )
    return colors[index % colors.size]
}

data class ToolkitItem(
    val name: String,
    val shortName: String,
    val icon: ImageVector,
    val color: Color,
    val action: () -> Unit
)

@Composable
fun ToolCard(tool: ToolkitItem) {
    Surface(
        color = tool.color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tool.color.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { tool.action() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = tool.color,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = tool.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun HotToolCard(item: HotToolItem, isEnglish: Boolean, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon handling could be improved with Coil if URLs provided, or use generic
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                 // Try to map icon name to generic icon or text
                 val iconText = item.icon.take(1).uppercase()
                 Text(iconText, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isEnglish) item.englishName else item.chineseName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "External Link",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.OpenInNew, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        }
    }
}
