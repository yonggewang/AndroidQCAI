package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.quantumproperty.qcai.data.AppLanguage
import com.quantumproperty.qcai.data.ChatMessage
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenClawChatScreen(viewModel: TeacherViewModel, onBack: () -> Unit) {
    val messages by viewModel.gatewayMessages.collectAsState()
    val streamingText by viewModel.gatewayStreamingText.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH
    
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText != null) {
            listState.animateScrollToItem((messages.size + if (streamingText != null) 1 else 0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (isEnglish) "Gateway Chat" else if (isSpanish) "Chat de Pasarela" else "网关对话",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            if (isEnglish) "Direct OpenClaw link" else if (isSpanish) "Enlace directo OpenClaw" else "由于网关直接链接",
                            fontSize = 11.sp,
                            color = Color(0xFF007AFF)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearGatewayChat() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                color = Color(0xFF1C1C1E),
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.ime)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .padding(bottom = 28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        placeholder = { 
                            Text(
                                if (isEnglish) "Type a message..." else if (isSpanish) "Escribe un mensaje..." else "输入消息...",
                                color = Color.Gray 
                            ) 
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                            disabledContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (textState.isNotBlank()) {
                                viewModel.sendGatewayChat(textState)
                                textState = ""
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = Color(0xFF007AFF),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { paddingValues ->
        // --- Message List ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            
            streamingText?.let {
                item {
                    ChatBubble(ChatMessage(text = it, isUser = false), isStreaming = true)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isStreaming: Boolean = false) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) Color(0xFF007AFF) else Color(0xFF1C1C1E)
    val shape = if (isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bgColor,
            shape = shape,
            border = if (!isUser) androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)) else null
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Column {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                    if (isStreaming) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                            color = Color(0xFF007AFF).copy(alpha = 0.5f),
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }
    }
}
