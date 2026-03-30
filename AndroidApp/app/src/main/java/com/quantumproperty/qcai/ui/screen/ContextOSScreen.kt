@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.quantumproperty.qcai.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumproperty.qcai.data.DataDomain
import com.quantumproperty.qcai.ui.viewmodel.ContextOSViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextOSScreen(
    viewModel: ContextOSViewModel = viewModel(),
    onBack: () -> Unit
) {

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.toggleDomain(DataDomain.LOCATION)
        }
    }
    
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleDomain(DataDomain.TEMPORAL)
        }
    }
    
    val motionPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleDomain(DataDomain.MOTION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Context OS", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.showSetupGuide = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Setup Guide", tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("Setup Guide", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    IconButton(onClick = { viewModel.manualSync() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Now", tint = Color.Black)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Card (Pulse)
            item {
                ContextPulseHeader()
            }

            // AI Insight
            item {
                val insight by viewModel.latestInsight.collectAsState()
                AIInsightCard(recommendation = insight ?: "Awaiting AI Insight from Gateway...")
            }

            // Data Domains
            item {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
                    DomainRow(
                        title = "Temporal & Intent",
                        icon = Icons.Default.Schedule,
                        color = Color(0xFFFF9800),
                        enabled = viewModel.temporalEnabled,
                        description = "Upcoming calendar events, reminders, and daily schedule analysis.",
                        onToggle = { 
                            if (!viewModel.temporalEnabled) {
                                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            } else {
                                viewModel.toggleDomain(DataDomain.TEMPORAL)
                            }
                        }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFEEEEEE))
                    DomainRow(
                        title = "Focus & Attention",
                        icon = Icons.Default.Adjust,
                        color = Color(0xFF9C27B0),
                        enabled = viewModel.attentionEnabled,
                        description = "Real-time attention state (Deep Work, Commuting, etc.) derived from device behavior.",
                        onToggle = { viewModel.toggleDomain(DataDomain.ATTENTION) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFEEEEEE))
                    DomainRow(
                        title = "Health & Vitality",
                        icon = Icons.Default.Favorite,
                        color = Color(0xFFF44336),
                        enabled = viewModel.healthEnabled,
                        description = "Vital signs (HRV, Heart Rate) and physical activity trends (steps, calories).",
                        onToggle = { viewModel.toggleDomain(DataDomain.BIOMETRICS) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFEEEEEE))
                    DomainRow(
                        title = "Motion & Env",
                        icon = Icons.Default.DirectionsWalk,
                        color = Color(0xFF4CAF50),
                        enabled = viewModel.motionEnabled,
                        description = "Physical activity tracking (walking, running) and environmental context.",
                        onToggle = { 
                            if (!viewModel.motionEnabled) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    motionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                } else {
                                    viewModel.toggleDomain(DataDomain.MOTION)
                                }
                            } else {
                                viewModel.toggleDomain(DataDomain.MOTION)
                            }
                        }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFEEEEEE))
                    DomainRow(
                        title = "Visual Entities",
                        icon = Icons.Default.Visibility,
                        color = Color(0xFF673AB7),
                        enabled = viewModel.visionEnabled,
                        description = "On-device image and text recognition to understand what you're seeing.",
                        onToggle = { viewModel.toggleDomain(DataDomain.VISION) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFEEEEEE))
                    DomainRow(
                        title = "Location & Travel",
                        icon = Icons.Default.LocationOn,
                        color = Color(0xFF00BCD4),
                        enabled = viewModel.locationEnabled,
                        description = "Semantic place analysis (Home, Work, Transit) without sending raw GPS.",
                        onToggle = { 
                            if (!viewModel.locationEnabled) {
                                locationPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            } else {
                                viewModel.toggleDomain(DataDomain.LOCATION)
                            }
                        }
                    )
                    
                    // Exact GPS Precision (v5.6 Enhancement)
                    AnimatedVisibility(visible = viewModel.locationEnabled) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White
                        ) {
                            Column {
                                Divider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFEEEEEE))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(Modifier.width(40.dp)) // Align with domain text
                                        Column {
                                            Text("Exact GPS Precision", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("Submit exact coordinates to AI", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    Switch(
                                        checked = viewModel.locationExactEnabled,
                                        onCheckedChange = { viewModel.toggleLocationExact() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00BCD4))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (viewModel.showSetupGuide) {
        ContextOSSetupGuideDialog(viewModel = viewModel, onDismiss = { viewModel.showSetupGuide = false })
    }
}

@Composable
fun ContextPulseHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE3F2FD))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Context Pulse", fontSize = 12.sp, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Spacer(Modifier.width(8.dp))
                Text("LIVE SENSORS", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color(0xFF2196F3)
        )
    }
}

@Composable
fun AIInsightCard(recommendation: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF3E0))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("AI Smart Insight", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        }
        Text(recommendation, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.8f))
    }
}

@Composable
fun DomainRow(
    title: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    description: String,
    onToggle: () -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(description) },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text("Close")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Spacer(Modifier.width(16.dp))
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = { },
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    Icons.Default.HelpOutline, 
                    contentDescription = "Help",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = color
            )
        )
    }
}

@Composable
fun ContextOSSetupGuideDialog(
    viewModel: ContextOSViewModel,
    onDismiss: () -> Unit
) {
    var showOverwriteAlert by remember { mutableStateOf(false) }
    var installForLinux by remember { mutableStateOf(false) }
    var showLogicPyPopup by remember { mutableStateOf(false) }
    var showDiscoveryGuide by remember { mutableStateOf(false) }

    if (showOverwriteAlert) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Overwrite Runtime Files?", fontSize = 15.sp) },
            text = {
                Text(
                    if (installForLinux)
                        "This will install unixTOOLS.md (as TOOLS.md), SOUL.md, and HEARTBEAT.md in ~/.openclaw/workspace/ on your gateway. Continue?"
                    else
                        "This will install TOOLS.md, SOUL.md, and HEARTBEAT.md in ~/.openclaw/workspace/ on your gateway. Continue?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.installQCAIGateway(forLinux = installForLinux)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Install", fontSize = 13.sp) }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
        )
    }

    if (showLogicPyPopup) {
        FileContentPopup(
            title = "logic.py",
            content = viewModel.logicPyContent ?: "Loading..."
        ) { }
    }

    if (showDiscoveryGuide) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discovery Mode Guide", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                    val ctx = LocalContext.current
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = {
                            android.webkit.WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                loadUrl("https://qcai-net.github.io/openclaw/qcai.html")
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchIntelligenceFiles()
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Context OS Intelligence Setup", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Configure your Gateway with the reasoning identity and proactive skills needed for Context OS to function.",
                        fontSize = 13.sp, color = Color.Gray
                    )

                    // ── Intelligence Layer ──
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("The Intelligence Layer (The Brain)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "The true 'Brain' of your Context OS resides in four core files on your Gateway. Modify them anytime — no app rebuild required.",
                            fontSize = 13.sp, color = Color.Gray
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF007AFF).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InfoRow(Icons.Default.AutoAwesome, "SOUL.md",      "Defines Identity, Style, and Skills.")
                            InfoRow(Icons.Default.Bolt,        "HEARTBEAT.md", "Global Execution Rules & Safety.")
                            InfoRow(Icons.Default.Build,       "TOOLS.md",     "Proactive Scheduler & Planning Logic.")
                            InfoRow(Icons.Default.Settings,    "logic.py",     "Momentum skill decision engine.")
                        }
                    }

                    // ── Option A: One-Click Install ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF007AFF).copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF007AFF))
                            Spacer(Modifier.width(8.dp))
                            Text("Option A: One-Click Install", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF007AFF).copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text("ℹ️ Recommendation: Automatic Update", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Automatically fetch the latest intelligence files and install them to your gateway's workspace (~/.openclaw/workspace/). Choose the platform that matches your gateway machine.",
                                fontSize = 13.sp, color = Color.Gray
                            )
                        }

                        // Linux/Mac button
                        Button(
                            onClick = { if (viewModel.isGatewayLinked) { installForLinux = true; } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = viewModel.isGatewayLinked && !viewModel.isInstallingQCAI && !viewModel.isFetchingIntelligence,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isGatewayLinked) Color(0xFF2E7D32) else Color.Gray
                            )
                        ) {
                            if (viewModel.isInstallingQCAI || viewModel.isFetchingIntelligence) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Working...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Terminal, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Install Intelligence For Linux/Mac Gateway", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Windows button
                        Button(
                            onClick = { if (viewModel.isGatewayLinked) { installForLinux = false; } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = viewModel.isGatewayLinked && !viewModel.isInstallingQCAI && !viewModel.isFetchingIntelligence,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isGatewayLinked) Color(0xFF007AFF) else Color.Gray
                            )
                        ) {
                            if (viewModel.isInstallingQCAI || viewModel.isFetchingIntelligence) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Working...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.DesktopWindows, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Install Intelligence For Windows Gateway", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (viewModel.installationSuccess) {
                            Column {
                                Text("✅ Gateway Intelligence Updated!", fontSize = 13.sp, color = Color(0xFF2E7D32))
                                Text("SOUL, HEARTBEAT, and TOOLS are now running in your workspace.", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                        viewModel.installationError?.let {
                            Text("❌ Error: $it", fontSize = 13.sp, color = Color.Red)
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))

                    // ── Option B: Manual Configuration ──
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Option B: Manual Configuration", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Manually install these files in your gateway folder ~/.openclaw/workspace/. Tap a file name to view and copy its content.",
                            fontSize = 13.sp, color = Color.Gray
                        )
                        GuideStepViewSimple("SOUL.md",               Icons.Default.AutoAwesome,   viewModel.soulContent ?: "Loading...")
                        GuideStepViewSimple("HEARTBEAT.md",          Icons.Default.Bolt,          viewModel.heartbeatContent ?: "Loading...")
                        GuideStepViewSimple("TOOLS.md (Windows)",    Icons.Default.DesktopWindows, viewModel.toolsContent ?: "Loading...")
                        GuideStepViewSimple("TOOLS.md (Linux/macOS)", Icons.Default.Terminal,     viewModel.unixToolsContent ?: "Loading...")
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))

                    // ── logic.py Section ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF7B1FA2).copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, null, tint = Color(0xFF7B1FA2))
                            Spacer(Modifier.width(8.dp))
                            Text("Install the Momentum Skill Engine", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text(
                            "After deploying the Intelligence brain files (TOOLS.md, HEARTBEAT.md and SOUL.md), install the logic engine that powers the Momentum skill:",
                            fontSize = 13.sp, color = Color.Gray
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF7B1FA2).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "~/.openclaw/skills/momentum/logic.py",
                                fontSize = 13.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF7B1FA2).copy(alpha = 0.1f))
                                .clickable { }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("View logic.py", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = Color(0xFF7B1FA2), modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(18.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Linux/macOS only — run after placing the file:", fontSize = 13.sp, color = Color.Gray)
                            }
                            Text(
                                "chmod +x ~/.openclaw/skills/momentum/logic.py",
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            )
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))

                    // ── Discovery Mode Section ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE65100).copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, null, tint = Color(0xFFE65100))
                            Spacer(Modifier.width(8.dp))
                            Text("Set Up Discovery Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text(
                            "Enable Discovery Mode in your OpenClaw gateway so that Context OS can automatically detect and activate new skills.",
                            fontSize = 13.sp, color = Color.Gray
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE65100).copy(alpha = 0.1f))
                                .clickable { }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("View Discovery Mode Setup Guide", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = Color(0xFFE65100), modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun FileContentPopup(title: String, content: String, onDismiss: () -> Unit) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(content, fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(content))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Copy to Clipboard", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
fun GuideStepViewSimple(
    title: String,
    icon: ImageVector,
    content: String
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        FileContentPopup(title = title, content = content) { }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color(0xFF007AFF))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(detail, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

