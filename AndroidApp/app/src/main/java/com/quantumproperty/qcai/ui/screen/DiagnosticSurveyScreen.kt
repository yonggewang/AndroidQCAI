package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumproperty.qcai.data.DataPrivacy
import com.quantumproperty.qcai.data.SurveyResponse

@Composable
fun DiagnosticSurveyScreen(
    onBack: () -> Unit,
    onComplete: (SurveyResponse) -> Unit
) {
    // State
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val response = remember { mutableStateOf(SurveyResponse()) }
    
    // Questions (Ported from iOS)
    val questions = listOf(
        Question(1, "Business Type", "What best describes your organization?", listOf("Freelancer / Solopreneur", "Small Business (2-10)", "Agency / Firm (11-50)", "Enterprise (>50)")),
        Question(2, "Primary Goal", "What do you want AI to do first?", listOf("Marketing & Content", "Coding & Development", "Legal/Document Review", "Data Analysis", "Image Generation")),
        Question(3, "Data Privacy", "How sensitive is your data?", listOf("Extremely - Local Only (Top Secret)", "Moderate - Cloud is OK (Standard)", "Unsure")),
        Question(4, "Usage Intensity", "How many people will use this system?", listOf("Solo User", "Small Team (2-10)", "Mid-Size (11-50)", "Enterprise Multi-User")),
        Question(5, "Input Size", "How much data will you process at once?", listOf("Small (Emails, Blogs)", "Medium (Books, Reports)", "Large (Entire Codebases, Legal Discovery)")),
        Question(6, "Speed or Accuracy?", "Choose your performance priority.", listOf("Creative / Fast (4-bit)", "Balanced (6-bit)", "High-Precision (8-bit)")),
        Question(7, "Platform Preference", "Which OS do you prefer?", listOf("macOS (Apple Silicon)", "Windows (NVIDIA)", "Windows (AMD / ROCm)", "Linux")),
        Question(8, "Multi-User Needs", "Will multiple people use this simultaneously?", listOf("Yes", "No")),
        Question(9, "Vision / OCR", "Do you need to analyze images or PDFs?", listOf("Yes, I need Vision", "No, text-only is fine"))
    )

    // Dynamic Skipping Logic
    fun shouldSkip(index: Int): Boolean {
        // iOS Logic: 
        // Q6 (Budget) is already removed from list access if we just iterate index
        // But we kept IDs in Question object.
        // Let's rely on list index for navigation, but careful with "Question 7" logic.
        
        val q = questions.getOrNull(index) ?: return false
        
        // Skip Platform (7) and Multi-User (8) if Cloud OK
        if (response.value.dataPrivacy == DataPrivacy.CLOUD_OK) {
             if (q.id == 7 || q.id == 8) return true
        }
        
        return false
    }
    
    // Navigation
    fun nextQuestion() {
        var nextIndex = currentQuestionIndex + 1
        while (nextIndex < questions.size && shouldSkip(nextIndex)) {
            nextIndex++
        }
        
        if (nextIndex >= questions.size) {
            onComplete(response.value)
        } else {
            currentQuestionIndex = nextIndex
        }
    }
    
    fun updateResponse(questionId: Int, answer: String) {
        val r = response.value
        when (questionId) {
            1 -> {
                r.businessType = answer
                r.teamSize = if (answer.contains("Freelancer")) "solo" 
                             else if (answer.contains("Small")) "smallTeam"
                             else "enterprise"
            }
            2 -> {
                val key = when {
                    answer.contains("Marketing") -> "marketing"
                    answer.contains("Coding") -> "coding"
                    answer.contains("Legal") -> "legal"
                    answer.contains("Data") -> "data_analysis"
                    answer.contains("Image") -> "image_gen"
                    else -> "general"
                }
                r.aiGoals = setOf(key)
            }
            3 -> {
                r.dataPrivacy = if (answer.contains("Local")) DataPrivacy.LOCAL_ONLY else if (answer.contains("Cloud")) DataPrivacy.CLOUD_OK else DataPrivacy.UNSURE
            }
            4 -> {
                r.teamSize = if (answer.contains("Enterprise")) "enterprise" else if (answer.contains("Small")) "smallTeam" else "solo"
            }
            5 -> {
                r.inputSize = if (answer.contains("Large")) "largeDocs" else "standard"
            }
            6 -> {
                r.accuracyLevel = when {
                    answer.contains("High-Precision") -> "High-Precision"
                    answer.contains("Fast") -> "Creative/Fast"
                    else -> "Balanced"
                }
            }
            7 -> r.platformPreference = answer.lowercase()
            8 -> r.multiUserNeeds = (answer == "Yes")
            9 -> r.visionNeeds = (answer.contains("Yes"))
        }
        // Save back
        response.value = r
    }

    val currentQuestion = questions[currentQuestionIndex]
    val progress = (currentQuestionIndex + 1).toFloat() / questions.size

    // Back Navigation Logic
    fun handleBack() {
        if (currentQuestionIndex > 0) {
            var prevIndex = currentQuestionIndex - 1
            while (prevIndex >= 0 && shouldSkip(prevIndex)) {
                prevIndex--
            }
            if (prevIndex >= 0) {
                currentQuestionIndex = prevIndex
            } else {
                onBack()
            }
        } else {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7)) // Light Gray bg
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
             IconButton(onClick = { handleBack() }) {
                 Icon(Icons.Default.ArrowBack, contentDescription = "Back")
             }
             Text("AI Strategy Survey", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Progress
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF007AFF),
            trackColor = Color.LightGray.copy(alpha = 0.3f)
        )
        Text(
             text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
             color = Color.Gray,
             fontSize = 12.sp,
             modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Question Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = currentQuestion.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = currentQuestion.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black.copy(alpha = 0.8f)
                )
                
                Spacer(Modifier.height(32.dp))
                
                // Options
                currentQuestion.options.forEach { option ->
                    SurveyOptionRow(
                        text = option,
                        isSelected = false // We just click to proceed
                    ) {
                        updateResponse(currentQuestion.id, option)
                        nextQuestion()
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun SurveyOptionRow(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF007AFF).copy(alpha = 0.1f) else Color(0xFFF0F0F5),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF007AFF)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF007AFF))
            }
        }
    }
}

data class Question(
    val id: Int,
    val title: String,
    val subtitle: String,
    val options: List<String>
)


