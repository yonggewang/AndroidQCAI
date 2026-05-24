package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.quantumproperty.qcai.data.AppLanguage
import com.quantumproperty.qcai.utils.BrowserUtils
import androidx.compose.ui.platform.LocalContext

@Composable
fun MoreScreen(viewModel: TeacherViewModel) {
    // This is the old full-screen tab. We will eventually remove this from MainScreen pager
    // and use NewsLocalLifeView instead. For now, let's just make it a wrapper.
    ProfileContent(viewModel)
}

@Composable
fun ProfileContent(viewModel: TeacherViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH
    val context = LocalContext.current

    // State for dialogs
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Profile Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when {
                                isSpanish -> "Perfil"
                                isEnglish -> "Profile"
                                else -> "个人资料"
                            },
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                isSpanish -> "Cuenta y Configuración"
                                isEnglish -> "Account & Settings"
                                else -> "账户与设置"
                            },
                            color = Color(0xFFAF52DE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoggedIn) {
                            IconButton(onClick = { viewModel.logout() }) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                        IconButton(onClick = { viewModel.closeProfile() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }

            // 2. USER CARD
            item {
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFAF52DE).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFFAF52DE),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = userName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isLoggedIn) (userProfile?.email ?: "") else (
                                        when {
                                            isSpanish -> "Modo Invitado"
                                            isEnglish -> "Guest Member"
                                            else -> "访客模式"
                                        }
                                    ),
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        if (!isLoggedIn) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { 
                                        viewModel.closeProfile()
                                        viewModel.openLogin() 
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAF52DE))
                                ) {
                                    Text(
                                        when {
                                            isSpanish -> "Acceso"
                                            isEnglish -> "Login"
                                            else -> "登录"
                                        }
                                    )
                                }
                                OutlinedButton(
                                    onClick = { 
                                        viewModel.closeProfile()
                                        viewModel.openRegister() 
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFAF52DE)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAF52DE))
                                ) {
                                    Text(
                                        when {
                                            isSpanish -> "Registro"
                                            isEnglish -> "Sign Up"
                                            else -> "注册"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. SETTINGS GROUP
            item {
                SettingsGroup(
                    when {
                        isSpanish -> "Configuración"
                        isEnglish -> "App Settings"
                        else -> "应用设置"
                    }
                ) {
                    SettingsItem(
                        icon = Icons.Default.Language,
                        label = when {
                            isSpanish -> "Idioma"
                            isEnglish -> "Language"
                            else -> "语言"
                        },
                        value = when(appLanguage) {
                            AppLanguage.CHINESE -> "中文"
                            AppLanguage.SPANISH -> "Español"
                            else -> "English"
                        },
                        onClick = { showLanguageDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        label = when {
                            isSpanish -> "Notificaciones"
                            isEnglish -> "Notifications"
                            else -> "通知"
                        },
                        value = if (isEnglish || isSpanish) "On" else "开启",
                        onClick = { }
                    )
                }
            }
            
            // 4. SUPPORT GROUP
            item {
                SettingsGroup(
                    when {
                        isSpanish -> "Soporte"
                        isEnglish -> "Support"
                        else -> "支持"
                    }
                ) {
                      SettingsItem(
                        icon = Icons.Default.Help,
                        label = when {
                            isSpanish -> "Centro de Ayuda"
                            isEnglish -> "Help Center"
                            else -> "帮助中心"
                        },
                        onClick = { BrowserUtils.openURL(context, "https://qcai-help.com") }
                    )
                    SettingsItem(
                        icon = Icons.Default.PrivacyTip,
                        label = when {
                            isSpanish -> "Privacidad"
                            isEnglish -> "Privacy Policy"
                            else -> "隐私政策"
                        },
                        onClick = { BrowserUtils.openURL(context, "https://qcai-privacy.com") }
                    )
                    
                    if (isLoggedIn) {
                        SettingsItem(
                            icon = Icons.Default.DeleteForever,
                            label = when {
                                isSpanish -> "Eliminar Cuenta"
                                isEnglish -> "Delete Account"
                                else -> "注销账号"
                            },
                            color = Color.Red,
                            onClick = { showDeleteConfirm = true }
                        )
                    }
                }
            }
            
            // 5. ADVANCED SETTINGS GROUP
            item {
                val apiKeySetupReason by viewModel.apiKeySetupReason.collectAsState()
                var localOpenAIKey by remember { mutableStateOf(com.quantumproperty.qcai.data.PreferenceManager.userOpenAIKey) }
                var localGeminiKey by remember { mutableStateOf(com.quantumproperty.qcai.data.PreferenceManager.userGeminiKey) }
                val selectedEngine by viewModel.selectedEngine.collectAsState()

                SettingsGroup(
                    when {
                        isSpanish -> "Ajustes Avanzados"
                        isEnglish -> "Advanced Settings"
                        else -> "高级设置"
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (apiKeySetupReason != null) {
                            Surface(
                                color = Color.Red.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = apiKeySetupReason ?: "",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Text(
                            text = when {
                                isSpanish -> "Esta aplicación se ejecuta con una clave API de Gemini limitada por velocidad. Si desea traer su propia clave de IA, complete lo siguiente:"
                                isEnglish -> "This app runs using a rate-limited Gemini API key. If you want to bring your own AI key, please fill them in below:"
                                else -> "本应用使用速率受限的 Gemini API 密钥运行。如果您想使用自己的 AI 密钥，请在下方填写："
                            },
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Engine selection: Row of buttons
                        Text(
                            text = when {
                                isSpanish -> "Seleccionar motor de IA predeterminado"
                                isEnglish -> "Select Default AI Engine"
                                else -> "选择默认 AI 引擎"
                            },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.setEngine(com.quantumproperty.qcai.data.AIEngine.CHATGPT)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedEngine == com.quantumproperty.qcai.data.AIEngine.CHATGPT) Color(0xFFAF52DE) else Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("ChatGPT")
                            }
                            Button(
                                onClick = {
                                    viewModel.setEngine(com.quantumproperty.qcai.data.AIEngine.GEMINI)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedEngine == com.quantumproperty.qcai.data.AIEngine.GEMINI) Color(0xFFAF52DE) else Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Gemini")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // OpenAI key textfield
                        OutlinedTextField(
                            value = localOpenAIKey,
                            onValueChange = {
                                localOpenAIKey = it
                                com.quantumproperty.qcai.data.PreferenceManager.userOpenAIKey = it
                            },
                            label = { Text("OpenAI API Key", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFAF52DE),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gemini key textfield
                        OutlinedTextField(
                            value = localGeminiKey,
                            onValueChange = {
                                localGeminiKey = it
                                com.quantumproperty.qcai.data.PreferenceManager.userGeminiKey = it
                            },
                            label = { Text("Gemini API Key", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFAF52DE),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }

             item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Version 1.1.0 (Build 945)",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { 
                Text(
                    when {
                        isSpanish -> "¿Eliminar cuenta?"
                        isEnglish -> "Delete Account?"
                        else -> "确认注销？"
                    }
                )
            },
            text = { 
                Text(
                    when {
                        isSpanish -> "Esta acción no se puede deshacer."
                        isEnglish -> "This action cannot be undone."
                        else -> "此操作无法撤销。"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.deleteAccount()
                    showDeleteConfirm = false 
                }) {
                    Text(
                        when {
                            isSpanish -> "Eliminar"
                            isEnglish -> "Delete"
                            else -> "确认注销"
                        }, 
                        color = Color.Red
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(
                        when {
                            isSpanish -> "Cancelar"
                            isEnglish -> "Cancel"
                            else -> "取消"
                        }
                    )
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { 
                Text(
                    when {
                        isSpanish -> "Seleccionar Idioma"
                        isEnglish -> "Select Language"
                        else -> "选择语言"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "English",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setLanguage(AppLanguage.ENGLISH)
                                showLanguageDialog = false
                            }
                            .padding(8.dp)
                    )
                    Divider()
                    Text(
                        "中文 (Chinese)",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setLanguage(AppLanguage.CHINESE)
                                showLanguageDialog = false
                            }
                            .padding(8.dp)
                    )
                    Divider()
                    Text(
                        "Español (Spanish)",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setLanguage(AppLanguage.SPANISH)
                                showLanguageDialog = false
                            }
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(
                        when {
                            isSpanish -> "Cancelar"
                            isEnglish -> "Cancel"
                            else -> "取消"
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String? = null,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = label, color = color, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(text = value, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
    }
    Divider(color = Color.White.copy(alpha = 0.05f))
}
