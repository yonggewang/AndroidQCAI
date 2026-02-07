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
    val showHotToolWebView by viewModel.showHotToolWebView.collectAsState()
    val hotToolWebUrl by viewModel.hotToolWebUrl.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()
    val currentWebUrl by viewModel.currentWebUrl.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    // val showAddressInput by viewModel.showAddressInput.collectAsState()
    val showAPIKeySetup by viewModel.showAPIKeySetup.collectAsState()
    val context = LocalContext.current

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

    if (showHotToolWebView && hotToolWebUrl != null) {
        Dialog(
            onDismissRequest = { viewModel.closeHotTool() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Surface(
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             IconButton(onClick = { viewModel.closeHotTool() }) {
                                 Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                             }
                             Text(
                                 text = if (isEnglish) "Tool Viewer" else "工具浏览",
                                 style = MaterialTheme.typography.titleMedium,
                                 modifier = Modifier.weight(1f).padding(start = 16.dp)
                             )
                             IconButton(onClick = { viewModel.closeHotTool() }) {
                                 Icon(Icons.Default.Close, contentDescription = "Close")
                             }
                        }
                    }
                    
                    Box(modifier = Modifier.weight(1f)) {
                    // AndroidView is now inside the Box
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString()
                                        if (url != null) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                view?.context?.startActivity(intent)
                                                return true
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        return false
                                    }
                                }
                                loadUrl(hotToolWebUrl!!)
                            }
                        },
                        update = { view ->
                             // Ensure we can go back if user navigates within the webview
                             view.tag = "HotToolWebView" 
                        }
                    )
                    } // End Box
                }
            }
        }
    }
    
    if (showAPIKeySetup) {
        APIKeySetupDialog(
            onDismiss = { viewModel.dismissAPIKeySetup() },
            onSave = { viewModel.dismissAPIKeySetup() }
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
                    isEnglish = isEnglish
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
            isEnglish = isEnglish
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
            isEnglish = isEnglish
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
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "AI API Key Settings" else "AI 设置") },
                            onClick = {
                                showAIAndToolsMenu = false
                                viewModel.openAPIKeySetup()
                            },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) }
                        )
                        
                        Divider()
                        
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
                        
                        if (userTools.isNotEmpty()) {
                            Text(
                                text = if (isEnglish) " Tools" else " 常用工具",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            userTools.forEach { item ->
                                val label = if (isEnglish) item.englishName else item.chineseName
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        showAIAndToolsMenu = false
                                        viewModel.openHotTool(item.url)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) }
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
                var showUserProfileMenu by remember { mutableStateOf(false) }
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

                Box {
                    IconButton(
                        onClick = { showUserProfileMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Profile",
                            tint = if (isLoggedIn) PrimaryPurple else Color.Gray
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showUserProfileMenu,
                        onDismissRequest = { showUserProfileMenu = false }
                    ) {
                        if (isLoggedIn) {
                            DropdownMenuItem(
                                text = { Text(userName) },
                                onClick = { showUserProfileMenu = false },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Reset Conversation" else "重置对话") },
                                onClick = { 
                                    showUserProfileMenu = false
                                    viewModel.resetConversation() 
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Logout" else "退出登录") },
                                onClick = { 
                                    showUserProfileMenu = false
                                    viewModel.logout() 
                                },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Login" else "登录") },
                                onClick = { 
                                    showUserProfileMenu = false
                                    viewModel.openLogin() 
                                },
                                leadingIcon = { Icon(Icons.Default.Login, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Register" else "注册") },
                                onClick = { 
                                    showUserProfileMenu = false
                                    viewModel.openRegister()
                                },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
                            )
                        }
                        
                        Divider()
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Settings" else "设置") },
                            onClick = { 
                                showUserProfileMenu = false
                                    viewModel.openSettings() 
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
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
                                isEnglish = isEnglish,
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
                        CLTVibeView(viewModel, isEnglish)
                    }
                    selectedTopic == AITopic.STOCK -> {
                        StockScreen(onOpenSettings = { viewModel.openAPIKeySetup() })
                    }
                    displayMode == DisplayMode.SETTINGS -> {
                        SettingsScreen(viewModel)
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
                                                if (url != null) {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        view?.context?.startActivity(intent)
                                                        return true
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                                return false
                                            }
                                        }
                                        loadUrl(currentWebUrl!!)
                                    }
                                },
                                update = { view ->
                                    val url = currentWebUrl
                                    if (url != null && view.url != url) {
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
fun SettingsScreen(viewModel: TeacherViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // System Grouped Background
            .padding(16.dp)
    ) {
        Text(
            if (isEnglish) "Settings" else "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // User Center Section
        Text(if (isEnglish) "User Center" else "用户中心", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isLoggedIn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = PrimaryPurple.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp),
                                tint = PrimaryPurple
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val userProfile by viewModel.userProfile.collectAsState()
                            val vipLevel = userProfile?.vipLevel ?: 1
                            val vipText = if (vipLevel >= 99) "Root Admin" else "VIP Level $vipLevel"
                            Text(vipText, style = MaterialTheme.typography.bodySmall, color = PrimaryPurple)
                        }
                        TextButton(onClick = { viewModel.logout() }) {
                            Text(if (isEnglish) "Logout" else "退出登录", color = Color.Red)
                        }
                    }
                } else {
                    Text(if (isEnglish) "Not Logged In" else "您尚未登录", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.openLogin() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isEnglish) "Login" else "用户登录")
                        }
                        OutlinedButton(
                            onClick = { viewModel.openRegister() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isEnglish) "Register" else "用户注册")
                        }
                    }
                }
            }
        }
        
        // VIP Settings (Speaker)
        val userProfile by viewModel.userProfile.collectAsState()
        val vipLevel = userProfile?.vipLevel ?: 0
        if (vipLevel >= 1) {
            Spacer(Modifier.height(24.dp))
            Text(if (isEnglish) "VIP Settings" else "VIP 设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val rate by viewModel.speechRate.collectAsState()
                    val pitch by viewModel.speechPitch.collectAsState()
                    
                    Text(if (isEnglish) "Speech Rate: ${String.format("%.1f", rate)}x" else "语速: ${String.format("%.1f", rate)}x")
                    Slider(
                        value = rate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.5f..2.0f
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(if (isEnglish) "Speech Pitch: ${String.format("%.1f", pitch)}" else "语调: ${String.format("%.1f", pitch)}")
                    Slider(
                        value = pitch,
                        onValueChange = { viewModel.setSpeechPitch(it) },
                        valueRange = 0.5f..2.0f
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputArea(
    onSend: (String) -> Unit, 
    onCameraClick: () -> Unit, 
    onMicClick: () -> Unit,
    isRecording: Boolean,
    placeholder: String
) {
    var text by remember { mutableStateOf("") }
    
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCameraClick,
                colors = IconButtonDefaults.iconButtonColors(contentColor = PrimaryPurple)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
            }
            Spacer(Modifier.width(4.dp))

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    } else {
                        onMicClick()
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isRecording) Color.Red else Color.Transparent,
                    contentColor = if (isRecording) Color.White else PrimaryPurple
                )
            ) {
                Icon(
                    imageVector = when {
                        isRecording -> Icons.Default.Stop
                        text.isNotBlank() -> Icons.Default.Send
                        else -> Icons.Default.Mic
                    }, 
                    contentDescription = if (isRecording) "Stop" else "Send/Mic"
                )
            }
        }
    }
}

@Composable
fun TopicButton(
    topic: AITopic, 
    dynamicItem: com.quantumproperty.qcai.data.TopMenuItem? = null,
    isSelected: Boolean, 
    isEnglish: Boolean,
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (msg.isUser) (if (isEnglish) "User" else "用户") else (if (isEnglish) "AI Answer" else "AI 回答"),
            style = MaterialTheme.typography.labelSmall,
            color = if (msg.isUser) Color.Blue else Color(0xFF4CAF50),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            color = if (msg.isUser) Color(0xFFE3F2FD) else Color(0xFFE8F5E9),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(12.dp),
                color = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun APIKeySetupDialog(onDismiss: () -> Unit, onSave: () -> Unit) {
    var openAIKey by remember { mutableStateOf(PreferenceManager.openAIKey) }
    var geminiKey by remember { mutableStateOf(PreferenceManager.geminiKey) }
    val viewModel: TeacherViewModel = viewModel()
    val selectedEngine by viewModel.selectedEngine.collectAsState()
    val apiKeySetupReason by viewModel.apiKeySetupReason.collectAsState()
    val uriHandler = LocalUriHandler.current

    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                item {
                    Text(if (isEnglish) "API Settings" else "API 设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    apiKeySetupReason?.let { reason ->
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red)
                                Spacer(Modifier.width(8.dp))
                                Text(reason, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    // AI Engine Selection
                    Text(if (isEnglish) "AI Platform" else "AI 平台选择", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setEngine(AIEngine.CHATGPT) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedEngine == AIEngine.CHATGPT) PrimaryPurple else Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ChatGPT", color = if (selectedEngine == AIEngine.CHATGPT) Color.White else Color.DarkGray)
                        }
                        Button(
                            onClick = { viewModel.setEngine(AIEngine.GEMINI) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedEngine == AIEngine.GEMINI) PrimaryPurple else Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Gemini", color = if (selectedEngine == AIEngine.GEMINI) Color.White else Color.DarkGray)
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    Divider()
                    Spacer(Modifier.height(20.dp))
                    
                    // Instructions
                    Text(if (isEnglish) "How to get API Key" else "如何获取 API Key", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    val instructions = if (isEnglish) listOf(
                        "1. Click the button below to visit the API Key page",
                        "2. Log in or sign up",
                        "3. Generate a new API Key",
                        "4. Copy and paste it into the field below"
                    ) else listOf(
                        "1. 点击下方按钮访问对应平台的 API Key 生成页面",
                        "2. 登录或注册账号",
                        "3. 生成新的 API Key",
                        "4. 复制 API Key 并粘贴到下方输入框中"
                    )

                    instructions.forEach { step ->
                        Text(
                            step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    val noteText = if (isEnglish) 
                        "Note: Using free tier API Keys from OpenAI and Gemini should be sufficient for the daily use of this app for most users."
                    else 
                        "注意：使用 OpenAI 和 Gemini 的免费层级 API Key 通常足以满足大多数用户的日常使用需求。"
                        
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Links to get API keys
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { uriHandler.openUri("https://platform.openai.com/api-keys") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isEnglish) "Get OpenAI Key" else "获取 OpenAI Key", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { uriHandler.openUri("https://aistudio.google.com") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isEnglish) "Get Gemini Key" else "获取 Gemini Key", fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    Divider()
                    Spacer(Modifier.height(20.dp))
                    
                    // API Key Input Fields
                    Text(if (isEnglish) "API Key Input" else "API Key 输入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = openAIKey,
                        onValueChange = { openAIKey = it },
                        label = { Text("OpenAI API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = PrimaryPurple) }
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = PrimaryPurple) }
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(if (isEnglish) "Cancel" else "取消", color = Color.Gray)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                PreferenceManager.openAIKey = openAIKey
                                PreferenceManager.geminiKey = geminiKey
                                onSave()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isEnglish) "Save" else "保存")
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealEstateInputArea(
    onDismiss: () -> Unit, 
    onConfirm: (String) -> Unit, 
    onChatWithAI: () -> Unit,
    isEnglish: Boolean, 
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isEnglish) "Property Assistant" else "房产助手",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                if (isEnglish) "Enter address for a full AI analysis" else "输入房产地址，AI 将为您全面分析该房产信息",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(if (isEnglish) "e.g. 123 Main St, Charlotte, NC" else "例如：123 Main St, Charlotte, NC") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = Color.LightGray
                ),
                singleLine = true
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        if (text.isNotBlank()) {
                            onConfirm(text) 
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp),
                    enabled = text.isNotBlank()
                ) {
                    Text(if (isEnglish) "Analysis" else "开始分析", color = Color.White)
                }
                
                OutlinedButton(
                    onClick = onChatWithAI,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEnglish) "Chat with AI" else "与 AI 交流")
                }
            }
        }
    }
}

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPassword: (String) -> Unit,
    isEnglish: Boolean
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEnglish) "Login" else "登录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isEnglish) "Password" else "密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                TextButton(onClick = { 
                    onDismiss()
                    onRegisterClick()
                }) {
                    Text(if (isEnglish) "No account? Register" else "没有账号？注册")
                }
                TextButton(onClick = {
                    onForgotPassword(email)
                }) {
                   Text(if (isEnglish) "Forgot Password?" else "忘记密码？", color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLogin(email, password) },
                enabled = email.isNotBlank() && password.isNotBlank()
            ) {
                Text(if (isEnglish) "Login" else "登录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Cancel" else "取消")
            }
        }
    )
}

@Composable
fun RegisterDialog(
    onDismiss: () -> Unit,
    onRegister: (String, String, String, String, String) -> Unit,
    onLoginClick: () -> Unit,
    isEnglish: Boolean
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }
    var showEULA by remember { mutableStateOf(false) }
    

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    // EULA Dialog
    if (showEULA) {
        AlertDialog(
            onDismissRequest = { showEULA = false },
            title = { 
                Text(
                    "Generative AI & Content EULA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "End User License Agreement (EULA)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    Text(
                        """
                        1. Agreement to Terms
                        By using this application, you agree to these terms. If you do not agree, do not use the application.

                        2. User Generated Content (UGC)
                        Users may post content (Events, Marketplace Items, Rentals). You agree that you will not post content that is:
                        • Illegal, harmful, or fraudulent
                        • Hateful, harassing, or bullying
                        • Pornographic or sexually explicit
                        • Infringing on intellectual property rights

                        3. Zero Tolerance Policy
                        We have a zero-tolerance policy for objectionable content. Content found to be in violation will be removed immediately, and the user's account may be banned without warning.

                        4. Reporting
                        You agree to report any content that violates these terms using the 'Report' feature provided on content.

                        5. Disclaimer
                        The developers are not responsible for the content posted by users. Use the marketplace and rental sections at your own risk.
                        
                        6. Privacy Notice
                        Your data is collected solely for account management and is securely stored. We do not share your personal information with third parties for marketing purposes.
                        """.trimIndent(),
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showEULA = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Full Screen Dialog for Registration
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding() 
                    .imePadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Register" else "注册",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email, 
                        onValueChange = { email = it },
                        label = { Text("Email (Verify Link will be sent)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isEnglish) "Password" else "密码") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                         keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
                    )
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(if (isEnglish) "Full Name" else "全名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(if (isEnglish) "Username" else "用户名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isEnglish) "Phone Number" else "电话号码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                    )
                    
                    // Terms and EULA Agreement
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryPurple.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { agreedToTerms = !agreedToTerms }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = agreedToTerms,
                                onCheckedChange = { agreedToTerms = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PrimaryPurple
                                )
                            )
                            
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                androidx.compose.ui.text.AnnotatedString.Builder().apply {
                                    if (isEnglish) {
                                        append("I agree to the ")
                                    } else {
                                        append("我同意 ")
                                    }
                                }
                                
                                Text(
                                    text = if (isEnglish) 
                                        "I agree to the Terms of Service and EULA" 
                                    else 
                                        "我同意服务条款和最终用户许可协议",
                                    fontSize = 13.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Terms of Service" else "服务条款",
                                        color = PrimaryPurple,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            uriHandler.openUri("https://cyberpandaapp.com/terms")
                                        },
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    )
                                    
                                    Text(
                                        text = "EULA",
                                        color = PrimaryPurple,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            showEULA = true
                                        },
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    )
                                    
                                    Text(
                                        text = if (isEnglish) "Privacy Policy" else "隐私政策",
                                        color = PrimaryPurple,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            uriHandler.openUri("https://cyberpandaapp.com/privacy")
                                        },
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Read Full EULA Button
                                TextButton(
                                    onClick = { showEULA = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = PrimaryPurple
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isEnglish) "Read Full EULA & Policies" else "阅读完整的服务条款",
                                        fontSize = 11.sp,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onRegister(email, password, fullName, username, phone) },
                        enabled = email.isNotBlank() && password.isNotBlank() && username.isNotBlank() && agreedToTerms,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isEnglish) "Register" else "注册", fontSize = 16.sp)
                    }

                    TextButton(
                        onClick = {
                            onDismiss()
                            onLoginClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEnglish) "Already have account? Login" else "已有账号？登录")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}


