package com.quantumproperty.qcai.ui.screen

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import com.quantumproperty.qcai.data.SceneEvent
import com.quantumproperty.qcai.data.SceneVenue
import com.quantumproperty.qcai.data.SceneResponse

import com.quantumproperty.qcai.data.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheSceneScreen(
    viewModel: TeacherViewModel,
    appLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val sceneData by viewModel.sceneData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH
    
    var selectedCategory by remember { mutableStateOf("Tech") }
    val categories = remember(appLanguage) {
        listOf(
            when {
                isSpanish -> "Tecnología"
                isEnglish -> "Tech"
                else -> "科技"
            },
            when {
                isSpanish -> "Música"
                isEnglish -> "Music"
                else -> "音乐"
            },
            when {
                isSpanish -> "Arte"
                isEnglish -> "Art"
                else -> "艺术"
            },
            when {
                isSpanish -> "Social"
                isEnglish -> "Social"
                else -> "社交"
            },
            when {
                isSpanish -> "Familia"
                isEnglish -> "Family"
                else -> "家庭"
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when {
                            isSpanish -> "El Escenario"
                            isEnglish -> "The Scene"
                            else -> "社群氛围"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF050505)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(
                        label = category,
                        isSelected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            viewModel.fetchScene(category)
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF9C27B0)
                    )
                } else if (sceneData != null) {
                    val data = sceneData!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Narrative
                        item {
                            Surface(
                                color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = data.narrative,
                                    modifier = Modifier.padding(20.dp),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        // Events Section
                        if (data.events.isNotEmpty()) {
                            item {
                                Text(
                                    text = when {
                                        isSpanish -> "Próximos Eventos"
                                        isEnglish -> "Upcoming Events"
                                        else -> "近期活动"
                                    },
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(data.events) { event ->
                                AndroidEventRow(event)
                            }
                        }

                        // Venues Section
                        if (data.venues.isNotEmpty()) {
                            item {
                                Text(
                                    text = when {
                                        isSpanish -> "Lugares Populares"
                                        isEnglish -> "Hot Venues"
                                        else -> "热门地点"
                                    },
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(data.venues) { venue ->
                                AndroidVenueRow(venue)
                            }
                        }
                        
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                } else {
                    Text(
                        text = when {
                            isSpanish -> "Escanea la escena para comenzar."
                            isEnglish -> "Scan the scene to start."
                            else -> "扫描氛围以开始。"
                        },
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isSelected) Color(0xFF9C27B0) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF9C27B0) else Color.White.copy(alpha = 0.1f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AndroidEventRow(event: SceneEvent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Box (Simplified)
            Surface(
                color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(20.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = event.date, color = Color(0xFF9C27B0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(text = event.location, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Text(
                    text = event.description,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun AndroidVenueRow(venue: SceneVenue) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF9C27B0),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9C27B0).copy(alpha = 0.1f))
                    .padding(8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = venue.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = venue.description, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1)
            }

            Text(
                text = "${(venue.vibeMatchScore * 100).toInt()}%",
                color = Color(0xFF9C27B0),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
