package com.quantumproperty.qcai.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quantumproperty.qcai.ui.component.QRScanner
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumproperty.qcai.data.AppLanguage
import com.quantumproperty.qcai.data.ConnectionState
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpenClawScreen(viewModel: TeacherViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH

    val connectionState by viewModel.openClawState.collectAsState()
    val isConnected = connectionState == ConnectionState.CONNECTED

    // Tailscale states from VM
    val isGatewayLinked by viewModel.isGatewayLinked.collectAsState()
    val isTunnelConnected by viewModel.isTunnelConnected.collectAsState()
    val gatewayCommand by viewModel.gatewayCommand.collectAsState()
    val tunnelIP by viewModel.tunnelIP.collectAsState()
    val showPortalSetup by viewModel.showPortalSetup.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val gatewayAuthKey by viewModel.gatewayAuthKey.collectAsState()
    val isPairingRequired by viewModel.isPairingRequired.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()
    val showQRScanner by viewModel.showQRScanner.collectAsState()
    val showJoinGatewayDialog by viewModel.showJoinGatewayDialog.collectAsState()
    val showSetupGuide by viewModel.showSetupGuide.collectAsState()
    val showConfigPreview by viewModel.showConfigPreview.collectAsState()
    val autoConnectGateway by viewModel.autoConnectGateway.collectAsState()

    var isGatewayExpanded by remember { mutableStateOf(!isConnected) }
    val context = LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleQRScanner(true)
        }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) isGatewayExpanded = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray.copy(alpha = 0.02f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Language Toggles (Simplified for space)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = appLanguage == lang
                        val shortName = when (lang) {
                            AppLanguage.ENGLISH -> "EN"
                            AppLanguage.CHINESE -> "CN"
                            AppLanguage.SPANISH -> "ES"
                        }
                        Text(
                            text = shortName,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF007AFF) else Color.Gray,
                            modifier = Modifier
                                .background(if (isSelected) Color(0xFF007AFF).copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .clickable { viewModel.setLanguage(lang) }
                        )
                    }
                }

                Text(
                    text = "OpenClaw",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                // User Profile
                Box(
                    modifier = Modifier.size(32.dp).background(Color.LightGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                
                // 2. Status Line
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp).background(if (isConnected) Color.Green else Color.Red, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) {
                                if (isEnglish) "Online" else if (isSpanish) "En Línea" else "在线"
                            } else {
                                if (isEnglish) "Offline" else if (isSpanish) "Fuera de Línea" else "离线"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isConnected) Color.Green else Color.Red
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Refresh", 
                            tint = Color(0xFF007AFF), 
                            modifier = Modifier.size(16.dp).clickable { /* Refresh logic */ }
                        )
                    }
                }

                // 3. Control Center
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (isEnglish) "Control Center" else if (isSpanish) "Centro de Control" else "控制中心",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = if (isEnglish) "Server Management & Automation" else if (isSpanish) "Gestión y Automatización de Servidores" else "服务器管理与自动化",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(Icons.Default.Tune, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ManagementCard(
                                title = if (isEnglish) "Dashboard" else if (isSpanish) "Panel" else "仪表板",
                                subtitle = if (isEnglish) "Monitor & Manage" else if (isSpanish) "Monitorear y Gestionar" else "监控与管理",
                                icon = Icons.Default.Public,
                                color = Color(0xFF34C759), // Green
                                modifier = Modifier.weight(1f),
                                isDisabled = !isConnected,
                                onClick = { viewModel.openWebConsole() }
                            )
                            ManagementCard(
                                title = if (isEnglish) "Portal" else if (isSpanish) "Portal" else "门户",
                                subtitle = if (isEnglish) "Node.js System" else if (isSpanish) "Sistema Node.js" else "Node.js 系统",
                                icon = Icons.Default.Kitchen,
                                color = Color(0xFFFF9500), // Orange
                                modifier = Modifier.weight(1f),
                                isDisabled = !isConnected,
                                onInfoClick = { viewModel.setShowPortalSetup(true) },
                                onClick = { viewModel.openPortal(context) }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ManagementCard(
                                title = if (isEnglish) "Gateway Chat" else if (isSpanish) "Chat de Pasarela" else "网关对话",
                                subtitle = if (isEnglish) "Direct LLM Link" else if (isSpanish) "Enlace LLM Directo" else "LLM 直接链接",
                                icon = Icons.Default.Chat,
                                color = Color(0xFFAF52DE), // Purple
                                modifier = Modifier.weight(1f),
                                isDisabled = !isConnected,
                                onClick = { viewModel.openGatewayChat() }
                            )
                            ManagementCard(
                                title = if (isEnglish) "Context OS" else if (isSpanish) "Contexto OS" else "系统环境",
                                subtitle = if (isEnglish) "AI Sensors" else if (isSpanish) "Sensores de IA" else "AI 传感器",
                                icon = Icons.Default.Psychology,
                                color = Color(0xFF007AFF), // Blue
                                modifier = Modifier.weight(1f),
                                isDisabled = !isConnected,
                                onClick = { viewModel.openContextOS() }
                            )
                        }
                    }
                }

                // 4. Gateway Connection Section
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { isGatewayExpanded = !isGatewayExpanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEnglish) "Gateway Connection" else if (isSpanish) "Conexión de Pasarela" else "网关连接",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Expand",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(12.dp).rotate(if (isGatewayExpanded) 90f else 0f)
                                )
                                Spacer(Modifier.weight(1f))
                                if (isGatewayExpanded) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                                        viewModel.toggleSetupGuide(true)
                                    }) {
                                        Text(if (isEnglish) "Full Setup Guide" else if (isSpanish) "Guía Completa" else "完整设置指南", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                                        Icon(Icons.Default.ChevronRight, contentDescription = "", tint = Color(0xFF007AFF), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            if (isGatewayExpanded) {
                                Text(
                                    text = if (isEnglish) "Follow these steps to securely link your app to your OpenClaw." else if (isSpanish) "Siga estos pasos para vincular su aplicación de forma segura." else "按照以下步骤安全连接网关。",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        if (isGatewayExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                
                                // Persistent Connection Toggle
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.White,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(if (isEnglish) "Persistent Connection" else if (isSpanish) "Conexión Persistente" else "持续连接", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Text(if (isEnglish) "Auto-reconnect to Gateway" else if (isSpanish) "Autoconectar a la Pasarela" else "自动重新连接网关", fontSize = 12.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = autoConnectGateway,
                                            onCheckedChange = { viewModel.autoConnectGateway.value = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Black)
                                        )
                                    }
                                }

                                // Step 1
                                ConnectivityStepCard(
                                    step = 1,
                                    title = if (isEnglish) "Start OpenClaw Gateway" else if (isSpanish) "Iniciar Pasarela OpenClaw" else "启动 OpenClaw 网关",
                                    isDone = isGatewayLinked || isTunnelConnected,
                                    isCollapsed = isGatewayLinked || isTunnelConnected
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        if (isGatewayLinked) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(if (isEnglish) "Gateway is running" else if (isSpanish) "La pasarela está en ejecución" else "网关正在运行", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                                                }
                                                Text("Done", fontSize = 10.sp, modifier = Modifier.background(Color.Green.copy(0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp), color = Color.Green)
                                            }
                                        } else {
                                            Text(
                                                text = if (isEnglish) "Inside your gateway terminal, use pm2 to ensure the server automatically restarts if it crashes:" else if (isSpanish) "Abra la terminal en su pasarela y use pm2 para reiniciar si falla:" else "在网关终端上，请使用 pm2 以确保崩溃时自动重启：",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth().background(Color.Gray.copy(0.05f), RoundedCornerShape(8.dp)).padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(gatewayCommand, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.Black)
                                                Icon(
                                                    Icons.Default.ContentCopy, null, tint = Color(0xFF007AFF), 
                                                    modifier = Modifier.clickable { 
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("cmd", gatewayCommand))
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Step 2
                                ConnectivityStepCard(
                                    step = 2,
                                    title = if (isEnglish) "Connect Tailscale VPN" else if (isSpanish) "Conectar VPN de Tailscale" else "连接 Tailscale VPN",
                                    isDone = isTunnelConnected,
                                    isCollapsed = isTunnelConnected
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        if (isTunnelConnected) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Security, null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Column {
                                                        Text(if (isEnglish) "VPN Online" else if (isSpanish) "VPN en Línea" else "VPN 在线", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                                                        Text(tunnelIP, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                }
                                                Text(
                                                    text = if (isEnglish) "Disconnect" else if (isSpanish) "Desconectar" else "断开",
                                                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Red,
                                                    modifier = Modifier.background(Color.Red.copy(0.1f), RoundedCornerShape(6.dp)).clickable { viewModel.disconnectTunnel() }.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = if (isEnglish) "A secure encrypted tunnel connects your phone directly." else if (isSpanish) "Un túnel cifrado seguro conecta su teléfono directamente." else "加密隧道会将您的手机直接连接到网关。",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            OutlinedTextField(
                                                value = gatewayAuthKey,
                                                onValueChange = { viewModel.gatewayAuthKey.value = it },
                                                placeholder = { Text("tskey-auth-...", fontFamily = FontFamily.Monospace, fontSize = 14.sp) },
                                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                                singleLine = true
                                            )
                                            Button(
                                                onClick = { viewModel.connectTunnel() },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                                            ) {
                                                Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(if (isConnecting) (if (isEnglish) "Connecting..." else if (isSpanish) "Conectando..." else "连接中...") else (if (isEnglish) "Connect VPN" else if (isSpanish) "Conectar VPN" else "连接 VPN"), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Step 3
                                ConnectivityStepCard(
                                    step = 3,
                                    title = if (isEnglish) "Link App to Gateway" else if (isSpanish) "Vincular App a la Pasarela" else "链接应用至网关",
                                    isLocked = !isTunnelConnected,
                                    isDone = isGatewayLinked,
                                    isCollapsed = isGatewayLinked
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        if (isGatewayLinked) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.VerifiedUser, null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(if (isEnglish) "Gateway Linked" else if (isSpanish) "Pasarela Vinculada" else "网关已链接", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                                                }
                                                Text(
                                                    text = if (isEnglish) "Unlink" else if (isSpanish) "Desvincular" else "取消链接",
                                                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Red,
                                                    modifier = Modifier.background(Color.Red.copy(0.1f), RoundedCornerShape(6.dp)).clickable { viewModel.unlinkGateway() }.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                                Button(
                                                    onClick = {
                                                        val permission = Manifest.permission.CAMERA
                                                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                                            viewModel.toggleQRScanner(true)
                                                        } else {
                                                            cameraPermissionLauncher.launch(permission)
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f).height(48.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.QrCodeScanner, null)
                                                }
                                                Button(
                                                    onClick = { viewModel.toggleJoinGatewayDialog(true) },
                                                    modifier = Modifier.weight(1f).height(48.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(0.1f), contentColor = Color.Black),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentPaste, null)
                                                }
                                                Button(
                                                    onClick = { viewModel.linkGateway() },
                                                    modifier = Modifier.weight(1f).height(48.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    enabled = isTunnelConnected
                                                ) {
                                                    Icon(Icons.Default.Link, null)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Step 4
                                ConnectivityStepCard(
                                    step = 4,
                                    title = if (isEnglish) "Approve Pairing" else if (isSpanish) "Aprobar Emparejamiento" else "批准配对",
                                    isLocked = !isTunnelConnected || (!isPairingRequired && !isGatewayLinked),
                                    isDone = isGatewayLinked
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = if (isEnglish) {
                                                if (isPairingRequired) "Pairing is required. Run these commands on your OpenClaw gateway to authorize this device."
                                                else "Run these commands on your OpenClaw gateway to authorize this device."
                                            } else if (isSpanish) {
                                                if (isPairingRequired) "Se requiere emparejamiento. Ejecute estos comandos en su pasarela OpenClaw para autorizar este dispositivo."
                                                else "Ejecute estos comandos en su pasarela OpenClaw para autorizar este dispositivo."
                                            } else {
                                                if (isPairingRequired) "需要配对。在您的 OpenClaw 网关上运行这些命令以授权此设备。"
                                                else "在您的 OpenClaw 网关上运行这些命令以授权此设备。"
                                            },
                                            fontSize = 12.sp, color = if (isPairingRequired) Color(0xFF007AFF) else Color.Gray,
                                            fontWeight = if (isPairingRequired) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Column(
                                            modifier = Modifier.fillMaxWidth().background(Color.Gray.copy(0.05f), RoundedCornerShape(8.dp)).padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("openclaw devices list", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF34C759))
                                            Text("openclaw devices approve ${deviceId.take(8)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                                        }
                                        
                                        if (deviceId.isNotEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (isEnglish) "Full Device ID: " else if (isSpanish) "ID de dispositivo completo: " else "完整设备 ID: ",
                                                    fontSize = 10.sp, color = Color.Gray
                                                )
                                                Text(
                                                    text = deviceId,
                                                    fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray,
                                                    modifier = Modifier.clickable {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // QR Scanner Overlay
    if (showQRScanner) {
        Dialog(
            onDismissRequest = { viewModel.toggleQRScanner(false) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            QRScanner(
                onCodeScanned = { code ->
                    viewModel.toggleQRScanner(false)
                    viewModel.applySetupCode(code)
                    viewModel.toggleJoinGatewayDialog(true)
                },
                onClose = { viewModel.toggleQRScanner(false) }
            )
        }
    }

    // Join Gateway Dialog
    if (showJoinGatewayDialog) {
        JoinGatewayDialog(
            viewModel = viewModel,
            isEnglish = isEnglish,
            onDismiss = { viewModel.toggleJoinGatewayDialog(false) }
        )
    }

    // Full Setup Guide Dialog
    if (showSetupGuide) {
        GatewaySetupGuideDialog(
            viewModel = viewModel,
            isEnglish = isEnglish,
            isSpanish = isSpanish,
            onDismiss = { viewModel.toggleSetupGuide(false) }
        )
    }

    // Config Preview Dialog
    if (showConfigPreview) {
        ConfigPreviewDialog(
            viewModel = viewModel,
            isEnglish = isEnglish,
            isSpanish = isSpanish,
            onDismiss = { viewModel.toggleConfigPreview(false) }
        )
    }

    // Portal Setup Dialog
    if (showPortalSetup) {
        PortalSetupDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowPortalSetup(false) }
        )
    }
}

@Composable
fun ManagementCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isDisabled: Boolean = false,
    onInfoClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .alpha(if (isDisabled) 0.6f else 1.0f)
            .clickable(enabled = !isDisabled) { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isDisabled) Color.Gray else color, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                
                if (onInfoClick != null) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Portal Info",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onInfoClick() }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(subtitle, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ConnectivityStepCard(step: Int, title: String, isLocked: Boolean = false, isDone: Boolean = false, isCollapsed: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().then(if (isLocked) Modifier.alpha(0.5f) else Modifier),
        border = if (isDone) BorderStroke(1.dp, Color.Green.copy(0.2f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).background(if (isDone) Color.Green else if (isLocked) Color.Gray.copy(0.2f) else Color(0xFF007AFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("$step", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isLocked) Color.Gray else Color.White)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isLocked) Color.Gray else Color.Black)
                Spacer(Modifier.weight(1f))
            }
            AnimatedVisibility(visible = !isCollapsed) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    content()
                    if (isLocked) {
                        // Invisible overlay to block clicks when locked
                        Box(modifier = Modifier.matchParentSize().clickable(enabled = false) {})
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGatewayDialog(
    viewModel: TeacherViewModel,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    val hostnameState by viewModel.gatewayHostname.collectAsState()
    val portState by viewModel.gatewayPort.collectAsState()
    val tokenState by viewModel.gatewayToken.collectAsState()
    val isPairingRequired by viewModel.isPairingRequired.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1C1C1E),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEnglish) "Join Gateway" else "加入网关",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }

                // Instructions (Simplified)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isEnglish) "Enter your gateway details below. You can paste a full Setup Code into the Token field."
                        else "在下方输入您的网关详细信息。您可以将完整的设置代码粘贴到令牌字段中。",
                        fontSize = 12.sp, color = Color.Gray
                    )
                }

                // Input Fields
                OutlinedTextField(
                    value = hostnameState,
                    onValueChange = { viewModel.gatewayHostname.value = it },
                    label = { Text(if (isEnglish) "Gateway Hostname / IP" else "网关主机名 / IP") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = portState,
                    onValueChange = { viewModel.gatewayPort.value = it },
                    label = { Text(if (isEnglish) "Port" else "端口") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = tokenState,
                    onValueChange = { 
                        viewModel.gatewayToken.value = it
                        // Try to auto-parse if it looks like a setup code
                        if (it.length > 20) {
                            viewModel.applySetupCode(it)
                        }
                    },
                    label = { Text(if (isEnglish) "Auth Token / Setup Code" else "身份验证令牌 / 设置代码") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF007AFF),
                        focusedLabelColor = Color(0xFF007AFF),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                if (isPairingRequired) {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFFF9500).copy(0.1f), RoundedCornerShape(12.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (isEnglish) "Pairing Required" else "需要配对", fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                        Text(
                            if (isEnglish) "Run this on gateway: openclaw devices approve ${deviceId.take(8)}"
                            else "在网关上运行：openclaw devices approve ${deviceId.take(8)}",
                            fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFFF9500)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.linkGateway()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isEnglish) "Link via Secure Tunnel" else "通过安全隧道链接", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewaySetupGuideDialog(
    viewModel: TeacherViewModel,
    isEnglish: Boolean,
    isSpanish: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF9F9F9)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEnglish) "Address Guide" else if (isSpanish) "Guía de Direcciones" else "地址指南",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Title & Desc
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (isEnglish) "OpenClaw Remote Control Setup Guide" else if (isSpanish) "Guía de Configuración de Control Remoto OpenClaw" else "OpenClaw 远程控制设置指南",
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black
                            )
                            Text(
                                if (isEnglish) "Follow these steps to enable remote access. Verified for OpenClaw 2026.3.13." 
                                else if (isSpanish) "Siga estos pasos para habilitar el acceso remoto. Verificado para OpenClaw 2026.3.13."
                                else "按照以下步骤启用远程访问。已针对 OpenClaw 2026.3.13 验证。",
                                fontSize = 13.sp, color = Color.Gray
                            )

                            // Config Note
                            val configNote = if (isEnglish) "Note: Once configured, your configuration should match the structure of this openclaw.json example. This specific template is optimized for a locally running LLM."
                                            else if (isSpanish) "Nota: Una vez configurado, su configuración debe coincidir con la estructura de este ejemplo de openclaw.json. Esta plantilla específica está optimizada para un LLM que se ejecuta localmente."
                                            else "注意：配置完成后，您的配置应与此 openclaw.json 示例的结构匹配。此特定模板针对本地运行的 LLM 进行了优化。"
                            
                            Row(modifier = Modifier.padding(top = 4.dp).clickable { viewModel.toggleConfigPreview(true) }) {
                                Text(
                                    configNote,
                                    fontSize = 11.sp,
                                    color = Color(0xFF007AFF),
                                    lineHeight = 16.sp,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                )
                            }
                        }

                        // Steps
                        SetupStepRow(
                            step = "1",
                            title = if (isEnglish) "Setup Gateway Computer" else if (isSpanish) "Configurar la Computadora Pasarela" else "设置网关计算机",
                            subtitle = if (isEnglish) "Install Tailscale on gateway and get an auth key." else if (isSpanish) "Instale Tailscale en la pasarela y obtenga una clave." else "在网关上安装 Tailscale 并获取授权密钥。",
                            code = "curl -fsSL https://tailscale.com/install.sh | sh"
                        )

                        SetupStepRow(
                            step = "2",
                            title = if (isEnglish) "Configure Gateway (One-Step Script)" else if (isSpanish) "Configurar Pasarela (Script de un paso)" else "配置网关（一键脚本）",
                            subtitle = if (isEnglish) "Pro Tip: Run this automation script on your gateway to configure trust, binding, and startup settings instantly. If you complete this step, you can skip Steps 3 and 4 and proceed directly to Step 5."
                                      else if (isSpanish) "Consejo: ejecute este script de automatización en su puerta de enlace para configurar el acceso, el enlace y el inicio al instante. Si completa este paso, puede omitir los pasos 3 y 4 y continuar directamente al paso 5."
                                      else "专家提示：在您的网关上立即运行此自动化脚本以配置信任、绑定和启动设置。如果您完成了此步骤，可以跳过第 3 步和第 4 步，直接进入第 5 步。",
                            code = "openclaw config set gateway.trustedProxies '[\"127.0.0.1\", \"::1\", \"100.0.0.0/8\"]' && openclaw config set gateway.bind loopback && openclaw config set gateway.auth.mode token && openclaw config set gateway.controlUi.allowedOrigins '[\"*\"]' && openclaw config set gateway.controlUi.dangerouslyAllowHostHeaderOriginFallback true && openclaw gateway stop && openclaw gateway --tailscale serve"
                        )

                        SetupStepRow(
                            step = "3",
                            title = if (isEnglish) "Trust & Network Binding" else if (isSpanish) "Confianza y Vinculación de Red" else "信任与网络绑定",
                            subtitle = if (isEnglish) "Allow Tailscale secure proxy and internal IP range. Usually handled by the script above." else if (isSpanish) "Permitir el proxy seguro de Tailscale y el rango de IP interno. Normalmente manejado por el script anterior." else "允许 Tailscale 安全代理和内部 IP 范围。通常由上面的脚本处理。",
                            code = "openclaw config set gateway.trustedProxies '[\"127.0.0.1\", \"::1\", \"100.0.0.0/8\"]' && openclaw config set gateway.bind loopback && openclaw config set gateway.auth.mode token"
                        )

                        SetupStepRow(
                            step = "4",
                            title = if (isEnglish) "Initialize Secure Path" else if (isSpanish) "Inicializar Ruta Segura" else "初始化安全路径",
                            subtitle = if (isEnglish) "Restart the gateway with the Tailscale serve flag active:" else if (isSpanish) "Reinicie la puerta de enlace con el indicador Tailscale serve activo:" else "带 Tailscale serve 标志重启网关：",
                            code = "openclaw gateway stop\nopenclaw gateway --tailscale serve"
                        )

                        SetupStepRow(
                            step = "5",
                            title = if (isEnglish) "Find MagicDNS Hostname" else if (isSpanish) "Buscar el nombre de host de MagicDNS" else "查找 MagicDNS 主机名",
                            subtitle = if (isEnglish) "Locate the Public URL in your terminal output after completing Step 4. It will end in .ts.net (e.g., https://mbp.tailnet-abc.ts.net)." 
                                      else if (isSpanish) "Busque la \"URL pública\" en la salida de su terminal después de completar el Paso 4. Terminará en .ts.net (por ejemplo, https://mbp.tailnet-abc.ts.net)."
                                      else "在完成第 4 步后，在终端输出中查找到公关 URL。 它将以 .ts.net 结尾（例如：https://mbp.tailnet-abc.ts.net）。"
                        )

                        SetupStepRow(
                            step = "6",
                            title = if (isEnglish) "Generate Connection QR" else if (isSpanish) "Generar QR de Conexión" else "生成连接二维码",
                            subtitle = if (isEnglish) "Run this command (replace MagicDNSHostName) to show the QR code. The QCAI app will automatically secure the connection (WSS) when Port 443 is detected." 
                                      else if (isSpanish) "Ejecute este comando (reemplace MagicDNSHostName) para mostrar el código QR. La aplicación QCAI asegurará automáticamente la conexión (WSS) cuando se detecte el Puerto 443."
                                      else "运行此命令（替换 MagicDNSHostName）以显示二维码。提示：当检测到端口 443 时，QCAI 应用程序将自动保护连接 (WSS)。",
                            code = "openclaw qr --url wss://MagicDNSHostName:443"
                        )

                        SetupStepRow(
                            step = "7",
                            title = if (isEnglish) "Approve Connection" else if (isSpanish) "Aprobar Conexión" else "批准连接",
                            subtitle = if (isEnglish) "Run these commands on your OpenClaw gateway to authorize this device." 
                                      else if (isSpanish) "Ejecute estos comandos en su puerta de enlace OpenClaw para autorizar este dispositivo."
                                      else "在您的 OpenClaw 网关上运行这些命令以授权此设备。",
                            code = "openclaw devices list\nopenclaw devices approve <pending request ID>"
                        )

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigPreviewDialog(
    viewModel: TeacherViewModel,
    isEnglish: Boolean,
    isSpanish: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E) // Dark background for code
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "openclaw.json",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (isEnglish) "Example Template" else if (isSpanish) "Ejemplo de Plantilla" else "示例模板",
                            fontSize = 12.sp, color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                    ) {
                        Text(
                            viewModel.openClawConfigJson,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFD4D4D4),
                            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.3f), RoundedCornerShape(12.dp)).padding(16.dp)
                        )
                    }
                }

                // Footer with Copy Button
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("openclaw.json", viewModel.openClawConfigJson))
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEnglish) "Copy Configuration" else if (isSpanish) "Copiar Configuración" else "复制配置", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SetupStepRow(
    step: String,
    title: String,
    subtitle: String,
    code: String? = null
) {
    val context = LocalContext.current
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).background(Color(0xFF007AFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(step, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(subtitle, fontSize = 12.sp, color = Color.Gray)
                }
            }

            if (code != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        code, 
                        fontFamily = FontFamily.Monospace, 
                        fontSize = 11.sp, 
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Icon(
                        Icons.Default.ContentCopy, 
                        null, 
                        tint = Color(0xFF007AFF), 
                        modifier = Modifier.size(16.dp).clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Setup Code", code))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PortalSetupDialog(viewModel: TeacherViewModel, onDismiss: () -> Unit) {
    val portalHost by viewModel.portalHost.collectAsState()
    val portalPort by viewModel.portalPort.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isEnglish = viewModel.appLanguage.collectAsState().value == com.quantumproperty.qcai.data.AppLanguage.ENGLISH
    val isSpanish = viewModel.appLanguage.collectAsState().value == com.quantumproperty.qcai.data.AppLanguage.SPANISH

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFF9500), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Kitchen, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            if (isEnglish) "Portal Setup" else if (isSpanish) "Configuración de Portal" else "门户设置",
                            fontWeight = FontWeight.Bold, fontSize = 18.sp
                        )
                        Text(
                            if (isEnglish) "Node.js System Configuration" else if (isSpanish) "Configuración del Sistema Node.js" else "Node.js 系统配置",
                            fontSize = 12.sp, color = Color.Gray
                        )
                    }
                }

                Divider(color = Color(0xFFF5F5F5))

                // Configuration Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (isEnglish) "Custom Portal Host" else if (isSpanish) "Nombre de Host del Portal" else "自定义门户主机",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray
                        )
                        OutlinedTextField(
                            value = portalHost,
                            onValueChange = { viewModel.updatePortalHost(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { 
                                Text(
                                    if (isEnglish) "e.g. 100.x.x.x (Empty for default)" else if (isSpanish) "ej. 100.x.x.x (Vacío por defecto)" else "例如 100.x.x.x (为空则使用默认值)",
                                    fontSize = 13.sp
                                ) 
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (isEnglish) "Custom Portal Port" else if (isSpanish) "Puerto del Portal" else "自定义门户端口",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray
                        )
                        OutlinedTextField(
                            value = portalPort,
                            onValueChange = { viewModel.updatePortalPort(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { 
                                Text(
                                    if (isEnglish) "Default: 18790" else if (isSpanish) "Por defecto: 18790" else "默认: 18790",
                                    fontSize = 13.sp
                                ) 
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                }

                // Reset Button
                val resetText = if (isEnglish) "Reset to Smart Defaults" else if (isSpanish) "Restablecer Valores Predeterminados" else "恢复默认设置"
                TextButton(
                    onClick = {
                        viewModel.updatePortalHost("")
                        viewModel.updatePortalPort("")
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF9500))
                    Spacer(Modifier.width(8.dp))
                    Text(resetText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                }

                Text(
                    if (isEnglish) "If left empty, the app will default to your currently linked Gateway IP and port 18790." 
                    else if (isSpanish) "Si se deja vacío, la aplicación se conectará por defecto a la IP de su Pasarela vinculada y al puerto 18790."
                    else "如果留空，应用程序将默认连接到当前绑定的网关 IP 和端口 18790。",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )

                // Documentation Button
                Button(
                    onClick = {
                        viewModel.openPortalInfo(context)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) {
                    Icon(Icons.Default.Book, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEnglish) "View Documentation" else if (isSpanish) "Ver Documentación" else "查看文档",
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(if (isEnglish) "Close" else if (isSpanish) "Cerrar" else "关闭", color = Color.Gray)
                }
            }
        }
    }
}
