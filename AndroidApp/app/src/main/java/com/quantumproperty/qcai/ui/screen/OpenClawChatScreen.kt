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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenClawChatScreen(viewModel: TeacherViewModel, onBack: () -> Unit) {
    val textState by viewModel.gatewayInputText.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val messages by viewModel.gatewayMessages.collectAsState()
    val streamingText by viewModel.gatewayStreamingText.collectAsState()
    val isEnglish = viewModel.appLanguage.collectAsState().value == AppLanguage.ENGLISH
    val isSpanish = viewModel.appLanguage.collectAsState().value == AppLanguage.SPANISH
    
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                @Suppress("DEPRECATION")
                val bitmap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
                
                selectedImageBitmap = bitmap
                selectedImageBase64 = viewModel.processImageForOpenClaw(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
                color = Color(0xFF1C1C1E),
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Image Preview
                    selectedImageBitmap?.let { bitmap ->
                        Box(
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { 
                                    selectedImageBitmap = null
                                    selectedImageBase64 = null
                                },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Multimedia Buttons
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo", tint = Color.Gray)
                        }
                        
                        IconButton(onClick = { viewModel.toggleRecording("OpenClawChat") }) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.Mic, 
                                contentDescription = "Voice Input", 
                                tint = if (isRecording) Color.Red else Color.Gray
                            )
                        }

                        TextField(
                            value = textState,
                            onValueChange = { viewModel.setGatewayInputText(it) },
                            placeholder = { 
                                Text(
                                    if (isEnglish) "Type a message..." else if (isSpanish) "Escribe un mensaje..." else "输入消息...",
                                    color = Color.Gray 
                                ) 
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
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
                        
                        FloatingActionButton(
                            onClick = {
                                if (textState.isNotBlank() || selectedImageBase64 != null) {
                                    viewModel.sendGatewayChat(textState, selectedImageBase64?.let { listOf(it) })
                                    viewModel.setGatewayInputText("")
                                    selectedImageBitmap = null
                                    selectedImageBase64 = null
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
                    Spacer(Modifier.height(12.dp))
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
