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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import com.quantumproperty.qcai.data.AppLanguage
import com.quantumproperty.qcai.utils.BrowserUtils
import androidx.compose.ui.platform.LocalContext

@Composable
fun NewsLocalLifeView(viewModel: TeacherViewModel, onBack: () -> Unit) {
    val menuItems by viewModel.topMenuItems.collectAsState()
    val gridItems by remember(menuItems) {
        derivedStateOf {
            menuItems.filter { 
                it.topic != com.quantumproperty.qcai.data.AITopic.STOCK && 
                it.topic != com.quantumproperty.qcai.data.AITopic.CLT_VIBE &&
                it.topic != com.quantumproperty.qcai.data.AITopic.COLLEGE
            }
        }
    }
    
    val language by viewModel.appLanguage.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopNewsSummaryGracefully()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                val isAutoPlay by viewModel.isAutoPlayNews.collectAsState()

                // Auto Play Toggle
                Button(
                    onClick = { viewModel.setAutoPlayNews(!isAutoPlay) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoPlay) Color(0xFF007AFF).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isAutoPlay) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Auto Play",
                        tint = if (isAutoPlay) Color(0xFF007AFF) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when(language) {
                            AppLanguage.CHINESE -> if (isAutoPlay) "自动播放: 开" else "自动播放: 关"
                            AppLanguage.SPANISH -> if (isAutoPlay) "Auto: ON" else "Auto: OFF"
                            else -> if (isAutoPlay) "Auto Play: ON" else "Auto Play: OFF"
                        },
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.width(12.dp))
                
                Text(
                    text = when(language) {
                        AppLanguage.CHINESE -> "新闻与生活"
                        AppLanguage.SPANISH -> "Noticias y Vida"
                        else -> "News & Life" // Simplified title
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.width(80.dp)) // Make room for global top-right icons
            }

            if (gridItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                LaunchedEffect(Unit) {
                    viewModel.fetchTopMenu()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(gridItems) { item ->
                        NewsTile(item, language) {
                            val url = when(language) {
                                AppLanguage.CHINESE -> item.chineseUrl
                                AppLanguage.SPANISH -> if (item.spanishUrl.isNotEmpty()) item.spanishUrl else item.englishUrl
                                else -> item.englishUrl
                            }
                            BrowserUtils.openURL(context, url)
                        }
                    }

                    // Rentals Item (Moved to End)
                    item {
                        NewsTile(
                            item = com.quantumproperty.qcai.data.TopMenuItem(
                                topic = com.quantumproperty.qcai.data.AITopic.NONE,
                                englishName = "Housing & Rentals",
                                chineseName = "房屋租赁",
                                icon = "house",
                                englishUrl = "",
                                chineseUrl = ""
                            ),
                            language = language
                        ) {
                            viewModel.showRentalsView()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsTile(item: com.quantumproperty.qcai.data.TopMenuItem, language: AppLanguage, onClick: () -> Unit) {
    val title = when(language) {
        AppLanguage.CHINESE -> item.chineseName
        AppLanguage.SPANISH -> item.englishName // Fallback as item doesn't have spanish name property yet, only URL
        else -> item.englishName
    }

    val color = getColorForIcon(item.icon)

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = mapIconVector(item.icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2
            )
        }
    }
}

fun getColorForIcon(icon: String): Color {
    return when(icon) {
        "newspaper", "newspaper.fill" -> Color(0xFFFF3B30) // Red
        "dollarsign.circle", "dollarsign.circle.fill" -> Color(0xFF34C759) // Green
        "brain", "brain.head.profile" -> Color(0xFFAF52DE) // Purple
        "fork.knife" -> Color(0xFFFF9500) // Orange
        "house", "house.fill" -> Color(0xFF007AFF) // Blue
        "life", "figure.walk" -> Color(0xFF5AC8FA) // Teal
        else -> Color(0xFF007AFF)
    }
}

fun mapIconVector(iconName: String): ImageVector {
    return when(iconName) {
        "newspaper" -> Icons.Default.Newspaper
        "dollarsign.circle" -> Icons.Default.AttachMoney
        "brain" -> Icons.Default.Psychology
        "life", "figure.walk" -> Icons.Default.DirectionsWalk
        "fork.knife" -> Icons.Default.Restaurant
        "house" -> Icons.Default.Home
        "hammer" -> Icons.Default.Build
        else -> Icons.Default.Link
    }
}
