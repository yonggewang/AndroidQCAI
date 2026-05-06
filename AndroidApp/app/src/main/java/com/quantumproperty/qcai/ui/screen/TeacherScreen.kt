package com.quantumproperty.qcai.ui.screen

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumproperty.qcai.data.AIEngine
import com.quantumproperty.qcai.data.AITopic
import com.quantumproperty.qcai.data.AppLanguage
import com.quantumproperty.qcai.data.ChatMessage
import com.quantumproperty.qcai.data.PreferenceManager
import com.quantumproperty.qcai.ui.viewmodel.DisplayMode
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import com.quantumproperty.qcai.utils.BrowserUtils

// Premium Color Palette
val PrimaryPurple = Color(0xFF6200EE)
val PrimaryTeal = Color(0xFF03DAC6)
val AccentOrange = Color(0xFFFF9800)
val SoftBlue = Color(0xFFE3F2FD)
val SoftGreen = Color(0xFFE8F5E9)
val DarkGrey = Color(0xFF121212)
val LightGrey = Color(0xFFF5F5F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(viewModel: TeacherViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()
    val currentWebUrl by viewModel.currentWebUrl.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    // val showAddressInput by viewModel.showAddressInput.collectAsState()
    val showAPIKeySetup by viewModel.showAPIKeySetup.collectAsState()
    val context = LocalContext.current
    
    var showUserSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var showDeleteAccountConfirmation by remember { mutableStateOf(false) }

    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH

    
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
            viewModel.helpWithDIY(bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                cameraLauncher.launch()
            } catch (e: Exception) {
                val errorMsg = if (isEnglish) "Cannot start camera: ${e.localizedMessage}" else "无法启动相机: ${e.localizedMessage}"
                viewModel.showError(errorMsg)
            }
        } else {
            val errorMsg = if (isEnglish) "Camera permission required" else "需要相机权限才能拍照"
            viewModel.showError(errorMsg)
        }
    }


    if (showAPIKeySetup) {
        APIKeySetupDialog(
            onDismiss = { viewModel.dismissAPIKeySetup() }
        )
    }

    val showAIRealEstateTools by viewModel.showAIRealEstateTools.collectAsState()
    if (showAIRealEstateTools) {
        Dialog(onDismissRequest = { viewModel.dismissAddressInput() }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                 RealEstateInputArea(
                    onDismiss = { viewModel.dismissAddressInput() },
                    onConfirm = { address -> viewModel.analyzeRealEstate(address) },
                    onChatWithAI = { viewModel.startAIChat() },
                    modifier = Modifier.padding(16.dp),
                    appLanguage = appLanguage
                )
            }
        }
    }

    val showLoginDialog by viewModel.showLoginDialog.collectAsState()
    val showRegisterDialog by viewModel.showRegisterDialog.collectAsState()
    
    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { viewModel.closeLogin() },
            onLogin = { email, pass -> viewModel.performLogin(email, pass) },
            onRegisterClick = { 
                viewModel.closeLogin()
                viewModel.openRegister()
            },
            onForgotPassword = { email ->
                if (email.isNotBlank()) {
                    viewModel.performPasswordReset(email)
                } else {
                    viewModel.showError(if (isEnglish) "Please enter email first" else "请先输入邮箱")
                }
            },
            appLanguage = appLanguage
        )
    }
    
    if (showRegisterDialog) {
        RegisterDialog(
            onDismiss = { viewModel.closeRegister() },
            onRegister = { email, pass, name, user, phone -> viewModel.performRegister(email, pass, name, user, phone) },
            onLoginClick = {
                viewModel.closeRegister()
                viewModel.openLogin()
            },
            appLanguage = appLanguage
        )
    }
    
    // Community Feature Screens
    val showEventsView by viewModel.showEventsView.collectAsState()
    val showMarketplaceView by viewModel.showMarketplaceView.collectAsState()
    val showRentalsView by viewModel.showRentalsView.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    
    if (showEventsView) {
        Dialog(
            onDismissRequest = { viewModel.closeEventsView() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                EventsScreen(
                    userProfile = userProfile,
                    onBack = { viewModel.closeEventsView() }
                )
            }
        }
    }
    
    if (showMarketplaceView) {
        Dialog(
            onDismissRequest = { viewModel.closeMarketplaceView() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                MarketplaceScreen(
                    userProfile = userProfile,
                    onBack = { viewModel.closeMarketplaceView() }
                )
            }
        }
    }
    
    if (showRentalsView) {
        Dialog(
            onDismissRequest = { viewModel.closeRentalsView() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                RentalsScreen(
                    userProfile = userProfile,
                    onBack = { viewModel.closeRentalsView() }
                )
            }
        }
    }

    val showTheSceneView by viewModel.showTheSceneView.collectAsState()
    if (showTheSceneView) {
        Dialog(
            onDismissRequest = { viewModel.closeTheSceneView() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                TheSceneScreen(
                    viewModel = viewModel,
                    appLanguage = appLanguage,
                    onBack = { viewModel.closeTheSceneView() }
                )
            }
        }
    }
    
    // AddressInputDialog removed
    
    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text(if (isEnglish) "Notice" else "提示") },
            text = {
                Column {
                    Text(msg)
                    if (msg.contains("Error 7")) {
                        Spacer(Modifier.height(8.dp))
                        val hint = if (isEnglish) 
                            "💡 Emulator Hint: Since emulators often lack Google Voice Services, it's recommended to use the text input below for testing."
                        else 
                            "💡 模拟器提示：由于模拟器通常缺乏 Google 语音服务，建议使用屏幕下方的文本框进行输入测试。"
                        Text(
                            hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryPurple
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) { Text(if (isEnglish) "OK" else "好的") }
            }
        )
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Fix for "Strange Look": Push content down below status bar/camera cutout
                    .statusBarsPadding() 
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .heightIn(min = 60.dp), // Ensure minimal touch target height
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val userName by viewModel.userName.collectAsState()

                // Language Switcher
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF0F0F0),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguageOption(
                            text = "中",
                            isSelected = !isEnglish,
                            onClick = { viewModel.setLanguage(AppLanguage.CHINESE) }
                        )
                        LanguageOption(
                            text = "EN",
                            isSelected = isEnglish,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))

                // Listen Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryPurple,
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { 
                            // Immediate visual feedback for "Nothing happens" issue
                            Toast.makeText(context, if (isEnglish) "Starting audio..." else "开始播放...", Toast.LENGTH_SHORT).show()
                            viewModel.startSequentialListen() 
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Listen",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                         Text(
                             text = if (isEnglish) "Listen" else "播",
                             color = Color.White,
                             fontSize = 12.sp,
                             fontWeight = FontWeight.Bold
                         )
                    }
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                
                // Removed User Profile Block from here

                // Merged AI Tools Dropdown
                var showAIAndToolsMenu by remember { mutableStateOf(false) }
                // val selectedEngine by viewModel.selectedEngine.collectAsState()

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showAIAndToolsMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isEnglish) "Tools" else "工具", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI and Tools Menu",
                            modifier = Modifier.size(20.dp),
                            tint = PrimaryPurple
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showAIAndToolsMenu,
                        onDismissRequest = { showAIAndToolsMenu = false }
                    ) {
                        // Category 1: AI API Key Settings
                        // Category 1: AI API Key Settings (Removed as backend handles keys)
                        

                        
                        // Category: Community Features
                        Text(
                            text = if (isEnglish) " Community" else " 社区服务",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Community Events" else "社区活动") },
                            onClick = {
                                showAIAndToolsMenu = false
                                viewModel.showEventsView()
                            },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                        )
                        
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Marketplace" else "二手市场") },
                            onClick = {
                                showAIAndToolsMenu = false
                                viewModel.showMarketplaceView()
                            },
                            leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                        )
                        
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Rentals" else "房屋租赁") },
                            onClick = {
                                showAIAndToolsMenu = false
                                viewModel.showRentalsView()
                            },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) }
                        )
                        
                        Divider()

                        // Category: Property Tools
                        Text(
                            text = if (isEnglish) " Property Tools" else " 房产工具",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Property AI Analysis" else "房产AI分析") },
                            onClick = {
                                showAIAndToolsMenu = false
                                viewModel.showAIRealEstateTools()
                            },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) }
                        )

                        Divider()
                        
                        // Category 3: Tools
                        val userTools by viewModel.userSpecificTools.collectAsState(initial = emptyList())
                        
                        // Static "Caller ID Lookup" (Native-like web tool for now, or new screen? User asked for native look.
                        // In iOS we used TwilioLookupView. In Android we don't have that screen yet.
                        // I will add it as a link for now but clearly marked, or better yet, I can't build a full screen now.
                        // I'll add it as a top item in the Tools list with a red icon.
                        
                        Text(
                            text = if (isEnglish) " Tools" else " 常用工具",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        // Caller ID Lookup (New)
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Caller ID Lookup" else "来电身份查询") },
                            onClick = {
                                showAIAndToolsMenu = false
                                //viewModel.openTwilioLookup() // Need to implement this or just open web
                                BrowserUtils.openURL(context, "https://qcai-net.github.io/tools/callerid.html") // Placeholder URL or feature
                            },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Person, // Closest to ID card
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Red
                                ) 
                            }
                        )
                        
                        if (userTools.isNotEmpty()) {
                            userTools.forEachIndexed { index, item ->
                                val vibrantColors = listOf(
                                    Color(0xFFAF52DE), // Purple
                                    Color(0xFF007AFF), // Blue
                                    Color(0xFFFF9500), // Orange
                                    Color(0xFF34C759), // Green
                                    Color(0xFFFF2D55), // Pink
                                    Color(0xFF30B0C7), // Teal
                                    Color(0xFF5856D6), // Indigo
                                    Color(0xFFFF3B30)  // Red
                                )
                                val iconColor = vibrantColors[index % vibrantColors.size]

                                DropdownMenuItem(
                                    text = { Text(if (isEnglish) item.englishName else item.chineseName) },
                                    onClick = {
                                        showAIAndToolsMenu = false
                                        BrowserUtils.openURL(context, item.url)
                                    },
                                    leadingIcon = { 
                                        // Use Globe as default if no icon mapping available, or map string to vector if possible.
                                        // For now, simple colored icon.
                                        Icon(
                                            imageVector = Icons.Default.Build, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(16.dp),
                                            tint = iconColor
                                        ) 
                                    }
                                )
                            }
                        } else if (!isLoggedIn) {
                            Text(
                                text = if (isEnglish) " More Tools" else " 更多工具",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            DropdownMenuItem(
                                text = { 
                                     Column {
                                         Text(if (isEnglish) "Register to unlock valuable tools" else "注册账号以解锁更多超值工具", fontSize = 14.sp)
                                         Text(if (isEnglish) "Exclusive to registered users" else "注册用户专享", fontSize = 10.sp, color = Color.Gray)
                                     }
                                },
                                onClick = { 
                                    showAIAndToolsMenu = false
                                    viewModel.openRegister()
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))
                
                // User Profile Menu (Previously Left, Moved to Right)
                // var showUserProfileMenu by remember { mutableStateOf(false) } (replaced by showUserSheet)
                // val isLoggedIn by viewModel.isLoggedIn.collectAsState() // Moved to top
                // val userName by viewModel.userName.collectAsState() // Moved to top

                if (isLoggedIn) {
                   Text(
                       text = userName,
                       style = MaterialTheme.typography.bodyMedium,
                       fontWeight = FontWeight.Bold,
                       color = PrimaryPurple
                   )
                   Spacer(Modifier.width(8.dp))
                }

                if (showUserSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showUserSheet = false },
                        sheetState = sheetState,
                        containerColor = Color.White,
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        UserQuickSettingsSheet(
                            viewModel = viewModel,
                            appLanguage = appLanguage,
                            onDismiss = { showUserSheet = false },
                            onDeleteClick = { showDeleteAccountConfirmation = true }
                        )
                    }
                }

                if (showDeleteAccountConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showDeleteAccountConfirmation = false },
                        title = { Text(if (isEnglish) "Delete Account?" else "确认注销？") },
                        text = { Text(if (isEnglish) "This action cannot be undone. All your data will be permanently deleted." else "此操作无法撤销。您的所有数据将被永久删除。") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteAccount()
                                    showDeleteAccountConfirmation = false
                                    showUserSheet = false
                                }
                            ) {
                                Text(if (isEnglish) "Delete" else "确认注销", color = Color.Red)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteAccountConfirmation = false }) {
                                Text(if (isEnglish) "Cancel" else "取消")
                            }
                        }
                    )
                }

                Box {
                    IconButton(
                        onClick = { showUserSheet = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Profile",
                            tint = if (isLoggedIn) PrimaryPurple else Color.Gray
                        )
                    }
                }


            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Divider()
            
            // Configuration View
            Column(modifier = Modifier.padding(16.dp)) {
                // Deleted Engine Switch from here
                
                // Refresh button removed

                Spacer(modifier = Modifier.height(12.dp))
                
                // Topic Grid
                val topMenuItems by viewModel.topMenuItems.collectAsState()
                val currentTopics = if (topMenuItems.isNotEmpty()) {
                    topMenuItems.map { it.topic }
                } else {
                    listOf(
                        AITopic.WORLD_NEWS, AITopic.FINANCE_NEWS, AITopic.AI_ANALYSIS, AITopic.FOOD,
                        AITopic.DIY, AITopic.REAL_ESTATE, AITopic.LIFE, AITopic.MISC, AITopic.CLT_VIBE
                    )
                }
                
                // Group into rows of 4
                val topicRows = currentTopics.chunked(4)
                
                topicRows.forEachIndexed { rowIndex, rowTopics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTopics.forEachIndexed { colIndex, topic ->
                            val itemIndex = rowIndex * 4 + colIndex
                            val dynamicItem = if (topMenuItems.size > itemIndex) topMenuItems[itemIndex] else null
                            
                            TopicButton(
                                topic = topic,
                                dynamicItem = dynamicItem,
                                isSelected = selectedTopic == topic,
                                appLanguage = appLanguage,
                                onClick = { viewModel.setTopic(topic) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                when {
                    selectedTopic == AITopic.CLT_VIBE -> {
                        CLTVibeView(viewModel)
                    }
                    selectedTopic == AITopic.COLLEGE -> {
                        CollegeAdmissionsScreen(
                            appLanguage = appLanguage,
                            onBack = { viewModel.setTopic(AITopic.CLT_VIBE) }
                        )
                    }
                    selectedTopic == AITopic.STOCK -> {
                        StockScreen()
                    }
                    displayMode == DisplayMode.WEB && currentWebUrl != null -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                         webViewClient = object : WebViewClient() {
                                             override fun shouldOverrideUrlLoading(
                                                 view: WebView?,
                                                 request: WebResourceRequest?
                                             ): Boolean {
                                                 val url = request?.url?.toString()
                                                 if (url != null && view != null) {
                                                     val currentHost = Uri.parse(currentWebUrl ?: "").host
                                                     val targetHost = request.url?.host
                                                     
                                                     // Only intercept if it's a different host (external link)
                                                     if (targetHost != null && targetHost != currentHost) {
                                                         BrowserUtils.openURL(view.context, url)
                                                         return true
                                                     }
                                                 }
                                                 return false
                                             }
                                         }
                                        currentWebUrl?.let { loadUrl(it) }
                                    }
                                },
                                update = { view ->
                                    val url = currentWebUrl
                                    // Robust check to avoid reload loops: 
                                    // only load if the URL is fundamentally different and not already loading.
                                    if (url != null && view.url != url && !url.equals(view.originalUrl, ignoreCase = true)) {
                                        view.loadUrl(url)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                    else -> {
                        // Regular Chat View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF2F2F7))
                        ) {
                            Text(
                                text = if (isEnglish) "AI Chat" else "AI 对话",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                            
                            Box(modifier = Modifier.weight(1f)) {
                                MessageList(messages = messages, selectedTopic = selectedTopic)
                            }
                            
                            if (isRecording) {
                                Surface(
                                    color = PrimaryPurple.copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(if (isEnglish) "Listening..." else "正在聆听...", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.stopRecordingAndSend() },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(if (isEnglish) "Send" else "发送")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Buttons Row Removed

            // Text Input Fallback Area
            // Text Input Area - Always Visible
            TextInputArea(
                onSend = { viewModel.sendMessage(it) },
                onCameraClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            cameraLauncher.launch()
                        } catch (e: Exception) {
                            val errorMsg = if (isEnglish) "Cannot start camera: ${e.localizedMessage}" else "无法启动相机: ${e.localizedMessage}"
                            viewModel.showError(errorMsg)
                        }
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
                placeholder = if (isEnglish) "Type or speak..." else "输入或语音..."
            )
        }
    }
}

@Composable
fun UserQuickSettingsSheet(
    viewModel: TeacherViewModel,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isEnglish = appLanguage == AppLanguage.ENGLISH

    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val isSpeechEnabled by viewModel.isSpeechEnabled.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val speechPitch by viewModel.speechPitch.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isEnglish) "Account" else "账户信息",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoggedIn) {
            // User info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = PrimaryPurple
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val vipLevel = userProfile?.vipLevel ?: 1
                        val levelText = if (vipLevel >= 99) "Root Admin" else "VIP Level $vipLevel"
                        Text(levelText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Settings Group (Visible for all)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Auto-Read Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isSpeechEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = if (isSpeechEnabled) PrimaryPurple else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(if (isEnglish) "Auto-Read" else "自动朗读", fontWeight = FontWeight.Medium)
                    }
                Switch(
                    checked = isSpeechEnabled,
                    onCheckedChange = { viewModel.toggleSpeech() },
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
                )
            }

            Divider(Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Speech Rate
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEnglish) "Rate" else "语速", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("%.1f".format(speechRate), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.updateSpeechConfig(rate = it, pitch = speechPitch) },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryPurple, activeTrackColor = PrimaryPurple)
                    )
                }

                // Speech Pitch
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEnglish) "Pitch" else "语调", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("%.1f".format(speechPitch), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechPitch,
                        onValueChange = { viewModel.updateSpeechConfig(rate = speechRate, pitch = it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryPurple, activeTrackColor = PrimaryPurple)
                    )
                }
            }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (isLoggedIn) {
            // Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                SettingsActionItem(
                    icon = Icons.Default.Refresh,
                    text = if (isEnglish) "Reset Conversation" else "清空对话",
                    color = Color.Red,
                    onClick = {
                        viewModel.resetConversation()
                        onDismiss()
                    }
                )
                Divider()
                SettingsActionItem(
                    icon = Icons.Default.PersonRemove,
                    text = if (isEnglish) "Delete Account" else "注销账号",
                    color = Color.Red,
                    onClick = onDeleteClick
                )
                Divider()
                SettingsActionItem(
                    icon = Icons.Default.QuestionMark,
                    text = if (isEnglish) "Help & Support" else "帮助与支持",
                    onClick = {
                        val helpUrl = if (isEnglish) "https://queencityai.net" else "https://queencityai.net/static/index_cn.html"
                        BrowserUtils.openURL(context, helpUrl)
                        onDismiss()
                    }
                )
                Divider()
                SettingsActionItem(
                    icon = Icons.Default.ExitToApp,
                    text = if (isEnglish) "Logout" else "退出登录",
                    color = Color.Red,
                    onClick = {
                        viewModel.logout()
                        onDismiss()
                    }
                )
            }
        } else {
            // Guest View
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isEnglish) "You are a guest. Please log in to view more content." else "您是访客。请登录以查看更多有价值的内容。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        onDismiss()
                        viewModel.openLogin()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEnglish) "Log In" else "登录账户")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        viewModel.openRegister()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEnglish) "Create Account" else "立即注册")
                }
            }
        }
    }
}

@Composable
fun SettingsActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String,
    color: Color = Color.Black,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
            }
            Text(text, color = color, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

// TextInputArea moved to SharedInput.kt

@Composable
fun TopicButton(
    topic: AITopic, 
    dynamicItem: com.quantumproperty.qcai.data.TopMenuItem? = null,
    isSelected: Boolean, 
    appLanguage: AppLanguage,
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
    val isEnglish = appLanguage == AppLanguage.ENGLISH

    val icon = if (dynamicItem != null) {
        when (dynamicItem.icon.lowercase()) {
            "newspaper" -> Icons.Default.Public
            "dollarsign.circle" -> Icons.Default.AttachMoney
            "brain" -> Icons.Default.Psychology // Better match if available, else Face
            "hammer" -> Icons.Default.Build
            "fork.knife" -> Icons.Default.Restaurant
            "house" -> Icons.Default.Home
            "heart" -> Icons.Default.Favorite
            "square.grid.2x2" -> Icons.Default.Dashboard
            "directions car" -> Icons.Default.DirectionsCar
            "bus.doubledecker" -> Icons.Default.DirectionsBus
            "graduationcap" -> Icons.Default.School
            "crown.fill" -> Icons.Default.AutoAwesome
            else -> Icons.Default.Dashboard
        }
    } else {
        when (topic) {
            AITopic.AI_ANALYSIS -> Icons.Default.Psychology
            AITopic.DIY -> Icons.Default.Build
            AITopic.FOOD -> Icons.Default.Restaurant
            AITopic.REAL_ESTATE -> Icons.Default.Home
            AITopic.WORLD_NEWS -> Icons.Default.Public
            AITopic.LIFE -> Icons.Default.Favorite
            AITopic.FINANCE_NEWS -> Icons.Default.AttachMoney
            AITopic.MISC -> Icons.Default.Dashboard
            AITopic.CLT_VIBE -> Icons.Default.AutoAwesome
            AITopic.STOCK -> Icons.Default.ShowChart
            AITopic.COLLEGE -> Icons.Default.School
            AITopic.NONE -> Icons.Default.Dashboard
            AITopic.BUSINESS -> Icons.Default.BusinessCenter
        }
    }
    
    val displayName = if (dynamicItem != null) {
        if (!isEnglish) dynamicItem.chineseName else dynamicItem.englishName
    } else {
        if (isEnglish) topic.englishName else topic.chineseName
    }
    
    Button(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f), // Make it square-ish for better grid look
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryPurple else Color(0xFFF5F5F5),
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (isSelected) Color.White else PrimaryPurple
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun LanguageOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .width(36.dp)
            .fillMaxHeight()
            .clickable { onClick() },
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) PrimaryPurple else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun MessageList(messages: List<ChatMessage>, selectedTopic: AITopic) {
    val viewModel: TeacherViewModel = viewModel()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val listState = rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    


    if (messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.LightGray
                )
                val startChatLabel = if (isEnglish) "Start Chat" else "开始对话"
                Text(startChatLabel, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                val topicLabel = if (isEnglish) selectedTopic.englishName else selectedTopic.chineseName
                val promptText = if (isEnglish) 
                    "Please ask questions related to 【$topicLabel】.\nClick 'Send' once finished."
                else 
                    "请说出与【${topicLabel}】相关的问题，\n说完后点击「发送」按钮。"
                Text(
                     promptText,
                     style = MaterialTheme.typography.bodyMedium,
                     color = Color.Gray,
                     textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages.filter { !it.isHidden }) { msg ->
                MessageBubble(msg)
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val viewModel: TeacherViewModel = viewModel()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (msg.isUser) {
                when {
                    isSpanish -> "Usuario"
                    isEnglish -> "User"
                    else -> "用户"
                }
            } else {
                when {
                    isSpanish -> "Respuesta de IA"
                    isEnglish -> "AI Answer"
                    else -> "AI 回答"
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (msg.isUser) Color.Blue else Color(0xFF4CAF50),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            color = if (msg.isUser) Color(0xFFE3F2FD) else Color(0xFFE8F5E9),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = msg.text,
                    color = Color.Black
                )

                // Render Decision Engine Cards if extraData is present
                msg.extraData?.let { extra ->
                    val type = extra["type"] as? String
                    Divider(color = Color.Black.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    
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



// Duplicate dialogs removed (moved to SharedComponents.kt)


