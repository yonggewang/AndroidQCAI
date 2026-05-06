package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumproperty.qcai.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeDetailScreen(
    university: University,
    student: StudentProfile,
    isEnglish: Boolean,
    onBack: () -> Unit
) {
    val result = CollegeAdmissionEngine.shared.analyze(student, university)
    val resultColor = when (result.category) {
        AdmissionCategory.SAFETY -> Color(0xFF4CAF50)
        AdmissionCategory.MATCH -> Color(0xFF2196F3)
        AdmissionCategory.REACH -> Color(0xFFF44336)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
        // App Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = university.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${university.meta.city}, ${university.state}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (university.type == SchoolType.PUBLIC) "Public Institution" else "Private Institution",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tier: ${university.selectivityTier.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6200EE)
                        )
                    }
                }
            }

            // AI Assessment
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(resultColor)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = result.category.label.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Based on your profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = result.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                    
                    if (result.confidence == AdmissionsData.DataQuality.ESTIMATED) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Some academic data is estimated.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            // Academic Profile
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEnglish) "Recent Freshman Profile" else "新生录取概况",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val rankStr = university.academics.nationalRanking?.let { "#$it" } ?: "200+"
                    DetailRow(if (isEnglish) "National Ranking" else "全美排名", rankStr)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // GPA
                    val gpaStr = if (university.academics.gpa25 != null && university.academics.gpa75 != null) {
                        "${university.academics.gpa25} - ${university.academics.gpa75}"
                    } else if (university.academics.gpaAvg != null) {
                        "${university.academics.gpaAvg} (Avg)"
                    } else {
                        if (isEnglish) "Not reported" else "未公布"
                    }
                    DetailRow(if (isEnglish) "GPA Range" else "平均分区间", gpaStr)
                    
                    // SAT
                    val satStr = if (university.scores.sat25 != null && university.scores.sat75 != null) {
                        "${university.scores.sat25} - ${university.scores.sat75}"
                    } else {
                        if (isEnglish) "Not reported" else "未公布"
                    }
                    DetailRow("SAT Range", satStr)
// ACT
                    val actStr = if (university.scores.act25 != null && university.scores.act75 != null) {
                        "${university.scores.act25} - ${university.scores.act75}"
                    } else {
                        if (isEnglish) "Not reported" else "未公布"
                    }
                    DetailRow("ACT Range", actStr)
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    Text(
                        text = if (isEnglish) "Class Rank Data" else "排名参考数据",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val top10 = university.academics.rankTop10Percent
                    val top25 = university.academics.rankTop25Percent

                    DetailRow(
                        if (isEnglish) "Top 10% of Class" else "班级前 10% 占比", 
                        if (top10 != null) "${(top10 * 100).toInt()}%" else "N/A"
                    )
                    DetailRow(
                        if (isEnglish) "Top 25% of Class" else "班级前 25% 占比", 
                        if (top25 != null) "${(top25 * 100).toInt()}%" else "N/A"
                    )
                }
            }
            
            // Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Data Cycle: ${university.meta.reportingYear ?: "2024"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "Source: ${university.admissions.scoreSource.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}
