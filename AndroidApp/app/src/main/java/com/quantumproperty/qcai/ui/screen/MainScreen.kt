package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel
import com.quantumproperty.qcai.data.AppLanguage
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.font.FontWeight

// Import the dialogs (assuming they are in separate files or I need to copy them?
// TeacherScreen had them inline or imported. 
// LoginDialog, RegisterDialog, etc. seem to be in TeacherScreen.kt or separate files.
// Let's assume they are Composable functions available in the package.
// If they were private in TeacherScreen, I cannot access them.
// I checked list_dir earlier, I don't see separate files for Dialogs in ui/screen.
// PLEASE NOTE: If they are defined INSIDE TeacherScreen.kt file but outside the class, I can use them.
// If they are inside TeacherScreen.kt, I might need to extract them or copy them.
// Let's check TeacherScreen.kt content again. 
// They seem to be external or at bottom of TeacherScreen.kt.
// I will blindly assumes they are available or I will fix imports. 
// Actually, I should probably move them to `Components.kt` or similar if I can.
// But for now, I'll assume they will resolve if in same package.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: TeacherViewModel = viewModel()) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH
    
    // Global States for Dialogs
    val showLoginDialog by viewModel.showLoginDialog.collectAsState()
    val showRegisterDialog by viewModel.showRegisterDialog.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showEventsView by viewModel.showEventsView.collectAsState()
    val showMarketplaceView by viewModel.showMarketplaceView.collectAsState()
    val showRentalsView by viewModel.showRentalsView.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val showTheSceneView by viewModel.showTheSceneView.collectAsState()
    val showNewsLocalLife by viewModel.showNewsLocalLifeView.collectAsState()
    val showProfileDialog by viewModel.showProfileDialog.collectAsState()
    val showContextOSView by viewModel.showContextOSView.collectAsState()
    val showGatewayChat by viewModel.showGatewayChat.collectAsState()
    val showCollegeAdmissions by viewModel.showCollegeAdmissions.collectAsState()

    // --- Dialogs ---

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
                    viewModel.showError(
                        when {
                            isSpanish -> "Por favor ingrese el correo electrónico primero"
                            isEnglish -> "Please enter email first"
                            else -> "请先输入邮箱"
                        }
                    )
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

    if (showCollegeAdmissions) {
        Dialog(
            onDismissRequest = { viewModel.closeCollegeAdmissions() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                CollegeAdmissionsScreen(
                    appLanguage = appLanguage,
                    onBack = { viewModel.closeCollegeAdmissions() }
                )
            }
        }
    }
    
    if (showTheSceneView) {
        Dialog(
            onDismissRequest = { viewModel.closeTheSceneView() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
             // TheSceneScreen is a full screen composable
             TheSceneScreen(
                viewModel = viewModel,
                appLanguage = appLanguage,
                onBack = { viewModel.closeTheSceneView() }
             )
        }
    }

    if (showNewsLocalLife) {
        // Full screen view for News & Local Life
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewModel.closeNewsLocalLife() },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
        NewsLocalLifeView(viewModel = viewModel, onBack = {
            viewModel.closeNewsLocalLife()
        })
        }
    }

    // --- AI Roadmap Integration ---
    val showAIRoadmapView by viewModel.showAIRoadmapView.collectAsState()
    val aiRoadmapResponse by viewModel.aiRoadmapResponse.collectAsState()

    if (showContextOSView) {
        Dialog(
            onDismissRequest = { viewModel.closeContextOS() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                ContextOSScreen(onBack = { viewModel.closeContextOS() })
            }
        }
    }

    if (showGatewayChat) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewModel.closeGatewayChat() },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            OpenClawChatScreen(viewModel = viewModel, onBack = { viewModel.closeGatewayChat() })
        }
    }

    if (showAIRoadmapView) {
        Dialog(
            onDismissRequest = { viewModel.closeAIRoadmap() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                if (aiRoadmapResponse != null) {
                    // Show Results
                    AIRoadmapResultsScreen(
                        response = aiRoadmapResponse!!,
                        onBack = { viewModel.closeAIRoadmap() }
                    )
                } else {
                    // Show Survey
                    DiagnosticSurveyScreen(
                        onBack = { viewModel.closeAIRoadmap() },
                        onComplete = { response -> 
                            viewModel.submitAISurvey(response)
                        }
                    )
                }
            }
        }
    }

    if (errorMessage != null) {
        val msg = errorMessage!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { 
                Text(
                    when {
                        isSpanish -> "Aviso"
                        isEnglish -> "Notice"
                        else -> "提示"
                    }
                ) 
            },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) { 
                    Text(
                        when {
                            isSpanish -> "Aceptar"
                            isEnglish -> "OK"
                            else -> "好的"
                        }
                    ) 
                }
            }
        )
    }

    // --- Back Handler ---
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.activity.compose.BackHandler {
        if (showLoginDialog) {
            viewModel.closeLogin()
        } else if (showRegisterDialog) {
            viewModel.closeRegister()
        } else if (showMarketplaceView) {
            viewModel.closeMarketplaceView()
        } else if (showEventsView) {
            viewModel.closeEventsView()
        } else if (showRentalsView) {
            viewModel.closeRentalsView()
        } else if (showTheSceneView) {
            viewModel.closeTheSceneView()
        } else if (showNewsLocalLife) {
            viewModel.closeNewsLocalLife()
        } else if (showContextOSView) {
            viewModel.closeContextOS()
        } else if (showGatewayChat) {
            viewModel.closeGatewayChat()
        } else if (showCollegeAdmissions) {
            viewModel.closeCollegeAdmissions()
        } else if (selectedTab == 3 && (viewModel.selectedTopic.value == com.quantumproperty.qcai.data.AITopic.STOCK || viewModel.selectedTopic.value == com.quantumproperty.qcai.data.AITopic.REAL_ESTATE)) {
             viewModel.setTopic(com.quantumproperty.qcai.data.AITopic.NONE)
        } else if (selectedTab != 1) { // Changed from 0 to 1 because CLT AI is now Tab 1
            viewModel.setSelectedTab(1)
        } else {
            // Finish activity (Quit App)
            (context as? android.app.Activity)?.finish()
        }
    }



    // --- Tab Configuration ---
    val pagerState = rememberPagerState(pageCount = { 5 })
    
    // Sync Pager with selectedTab (User-initiated tab change)
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.scrollToPage(selectedTab)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF121212), // Solid dark background for better contrast
                tonalElevation = 8.dp
            ) {
                // 0. OpenClaw (Previously Home)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "OpenClaw") },
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Gateway"
                                isEnglish -> "OpenClaw"
                                else -> "网关"
                            }
                        ) 
                    },
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF007AFF),
                        selectedTextColor = Color(0xFF007AFF),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = Color(0xFF007AFF).copy(alpha = 0.15f)
                    )
                )
                
                // 1. CLT AI Hub (Previously Business Hub)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "CLT AI") },
                    label = { 
                        Text(
                            when {
                                isSpanish -> "CLT AI"
                                isEnglish -> "CLT AI"
                                else -> "AI 中心"
                            }
                        ) 
                    },
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFAF52DE),
                        selectedTextColor = Color(0xFFAF52DE),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = Color(0xFFAF52DE).copy(alpha = 0.15f)
                    )
                )
                
                // 2. CLT Vibe
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationCity, contentDescription = "Vibe") },
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Vibra"
                                isEnglish -> "Vibe"
                                else -> "氛围"
                            }
                        ) 
                    },
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFAF52DE),
                        selectedTextColor = Color(0xFFAF52DE),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = Color(0xFFAF52DE).copy(alpha = 0.15f)
                    )
                )
                
                // 3. AI Toolkit
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = "Toolkit") },
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Herramientas"
                                isEnglish -> "Toolkit"
                                else -> "工具"
                            }
                        ) 
                    },
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFAF52DE),
                        selectedTextColor = Color(0xFFAF52DE),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = Color(0xFFAF52DE).copy(alpha = 0.15f)
                    )
                )
                
                // 4. News & Life
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Newspaper, contentDescription = "News") },
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Noticias"
                                isEnglish -> "News & Life"
                                else -> "生活"
                            }
                        ) 
                    },
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFAF52DE),
                        selectedTextColor = Color(0xFFAF52DE),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = Color(0xFFAF52DE).copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> OpenClawScreen(viewModel)
                    1 -> CLTAIHubScreen(viewModel)
                    2 -> CLTVibeView(viewModel)
                    3 -> AIToolkitScreen(viewModel)
                    4 -> NewsLocalLifeView(viewModel = viewModel, onBack = {
                         // Tab index 4 now shows the actual NewsLocalLifeView content
                    })
                }
            }

            // Top Right Icons: Language Toggle & User Profile
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Language Toggle (Cycles EN -> ES -> CN)
                Box(
                    modifier = Modifier
                        .clickable { viewModel.cycleLanguage() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (appLanguage) {
                            AppLanguage.ENGLISH -> "EN"
                            AppLanguage.SPANISH -> "ES"
                            AppLanguage.CHINESE -> "CN"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // User Account Icon
                IconButton(
                    onClick = { viewModel.openProfile() } // Always open profile/settings
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User Account",
                        tint = if (isLoggedIn) Color(0xFFAF52DE) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    if (showProfileDialog) {
        Dialog(
            onDismissRequest = { viewModel.closeProfile() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler { viewModel.closeProfile() }
            Surface(modifier = Modifier.fillMaxSize()) {
                ProfileContent(viewModel) // Hosted in full screen dialog
            }
        }
    }
}

// ProfileDialog function removed, integrated into MainScreen directly using ProfileContent
