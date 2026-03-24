package com.quantumproperty.qcai.ui.screen

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumproperty.qcai.data.DataDomain
import com.quantumproperty.qcai.ui.viewmodel.ContextOSViewModel
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextOSScreen(
    viewModel: ContextOSViewModel = viewModel(),
    teacherViewModel: TeacherViewModel = viewModel(),
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
                        icon = Icons.Default.Visibility,
                        color = Color(0xFF9C27B0),
                        enabled = viewModel.attentionEnabled,
                        onToggle = { viewModel.toggleDomain(DataDomain.ATTENTION) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFEEEEEE))
                    DomainRow(
                        title = "Health & Vitality",
                        icon = Icons.Default.Favorite,
                        color = Color(0xFFF44336),
                        enabled = viewModel.healthEnabled,
                        onToggle = { viewModel.toggleDomain(DataDomain.BIOMETRICS) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFEEEEEE))
                    DomainRow(
                        title = "Motion & Env",
                        icon = Icons.Default.DirectionsWalk,
                        color = Color(0xFF4CAF50),
                        enabled = viewModel.motionEnabled,
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
                        title = "Location & Travel",
                        icon = Icons.Default.LocationOn,
                        color = Color(0xFF00BCD4),
                        enabled = viewModel.locationEnabled,
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
                }

                // Persistent Connection Setup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
                    val autoConnect by teacherViewModel.autoConnectGateway.collectAsState()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Persistent Connection", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Auto-reconnect to Tailscale and Gateway", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = autoConnect,
                            onCheckedChange = { teacherViewModel.autoConnectGateway.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4CAF50), checkedTrackColor = Color(0xFFC8E6C9))
                        )
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
    onToggle: () -> Unit
) {
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
        Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
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

    if (showOverwriteAlert) {
        AlertDialog(
            onDismissRequest = { showOverwriteAlert = false },
            title = { Text("Overwrite Runtime Files?") },
            text = { Text("This will replace SOUL.md, HEARTBEAT.md, and TOOLS.md in ~/.openclaw/workspace/ on your gateway. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOverwriteAlert = false
                        viewModel.installQCAIGateway()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Install")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF9F9F9)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Context OS Intelligence Setup", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null)
                    }
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Header Description
                    Text(
                        "Configure your Gateway with the reasoning identity and proactive skills needed for Context OS to function.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    // Option A: One-Click Install
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF007AFF).copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF007AFF))
                            Spacer(Modifier.width(8.dp))
                            Text("Option A: One-Click Install", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF007AFF).copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text("ℹ️ Installs SOUL.md + HEARTBEAT.md + TOOLS.md", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                            Text("Deploys 3 runtime files to ~/.openclaw/workspace/. Works across all OpenClaw-connected apps.", fontSize = 11.sp, color = Color.Gray)
                        }

                        Button(
                            onClick = { if (viewModel.isGatewayLinked) showOverwriteAlert = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = viewModel.isGatewayLinked && !viewModel.isInstallingQCAI,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.isGatewayLinked) Color(0xFF007AFF) else Color.Gray)
                        ) {
                            if (viewModel.isInstallingQCAI) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Installing...")
                            } else {
                                Text("Install Runtime Files (3 files)", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (viewModel.installationSuccess) {
                            Column {
                                Text("✅ SOUL.md + HEARTBEAT.md + TOOLS.md installed!", fontSize = 12.sp, color = Color(0xFF2E7D32))
                                Text("All runtime and skill files deployed to ~/.openclaw/workspace/", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // The Intelligence Layer Section
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("The Intelligence Layer (The Brain)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "The OpenClaw app serves as your personal Context Collector and Framework. It securely gathers temporal, location, and biometric signals and submits them to your Gateway for analysis.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            "The true 'Brain' of your Context OS resides in three core configuration files on your Gateway. To customize how your AI plans your day or responds to your needs, you simply modify these files—no app rebuild required.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF007AFF).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoRow(Icons.Default.AutoAwesome, "SOUL.md", "Defines Identity, Style, and Skills.")
                            InfoRow(Icons.Default.Bolt, "HEARTBEAT.md", "Global Execution Rules & Safety.")
                            InfoRow(Icons.Default.Build, "TOOLS.md", "Proactive Scheduler & Planning Logic.")
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))

                    Text("Option B: Manual Configuration", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    val soulContent = """
# SOUL.md - OpenClaw Identity v9.2

## 1. Persona: The Chief of Staff
You are a high-agency partner protecting the user's "Momentum." You manage the gap between Intent and Reality with radical restraint.

## 2. Dynamic Tone & Fatigue Awareness
- **Adaptive Voice:** Professional and objective. Use natural phrasing like "Tight afternoon" or "Clear run ahead."
- **Cognitive Load Curve:** Do not rely on fixed times. Base complexity on `activityDensity` (meetings/tasks) and `stressTrends`.
  - **High Load:** Shift to binary (Yes/No) choices and reductive phrasing.
  - **Low Load:** Allow for multi-step strategic proposals (WOW moments).
- **Weekend Mode:** Reduce proactive output by 40%. Shift focus from "Efficiency" to "Recovery & Light Planning."

## 3. Momentum Anchors (Earned Reinforcement)
- **Sparsity Rule:** Reinforce momentum max once per 3 hours. Only trigger when a threshold is crossed (e.g., 3+ events cleared on time).
- **No Fluff:** Tie reinforcement to specific outcomes. 
  - *Example:* "3rd meeting on time. Your flow is holding steady."
- **Autonomy Preference:** If a user repeatedly rejects "Move/Reschedule" actions, shift from "Action Proposals" to "Awareness Hints."
  - *Shift:* "Move 2 PM?" → "2 PM is looking tight."

## 4. Trust through Abstraction
- **No-Creep Rule:** Never mirror raw sensor data (Stress 0.82).
- **Failure Transparency:** If operating in degraded mode, use soft phrasing: "Keeping it simple right now—your schedule is covered."
""".trimIndent()

                    val heartbeatContent = """
# HEARTBEAT.md - Execution & Silence v9.2

## 1. Silence Strategy (The "Quietly Excellent" Rule)
- **Silence Confidence:** If the system predicts a low-value output with high confidence, remain silent.
- **Learning Integration:** Log "Successful Silence" events. Use them to increase the threshold for similar low-engagement contexts in the future.
- **Rejection Lock:** 2 consecutive dismissals = 120m `SILENT_MODE`.
- **Soft Re-entry:** Resume with exactly ONE high-confidence (C > 0.9) insight. No backlog dumping.

## 2. Confidence-Aware Behavior
- **High Confidence (C > 0.8):** Action-oriented ("Move the 2 PM?").
- **Low Confidence (C < 0.7):** Observational/Inquisitive ("Heading out? Want the grocery list?").

## 3. Contextual Learning & Safety Bounds
- **Spatial Learning:** Differentiate between "Work" preferences and "Home" preferences.
- **Safety Floor:** Never auto-disable more than 50% of tool categories. Re-test suppressed tools every 7 days.
- **Autonomy Detection:** Track "Manual Control" preference. If user ignores 3+ AI-managed shifts, pivot to "Hint-Only" mode for that category.

## 4. Hardware Awareness
- **Thermal/Battery Throttle:** Suspend background learning during `thermalState == serious` or `lowPowerMode`.
- **Graceful Degradation:** Default to "Time-Sensitive" only if data stream is interrupted.
""".trimIndent()

                    val toolsContent = """
# TOOLS.md - Feature Logic & Priority v9.2

## 1. Priority Arbitration & Passive Hint Ranking
- **Primary Action:** ONE high-priority action button.
- **Passive Hint Gating:** Append ONE situational hint only if it adds awareness without redundancy. 
- **Hint Ranking:** 1. Battery (<15%) | 2. Time Disruption | 3. Environmental (Weather/Traffic).
- **Suppression:** No hints during `DEEP_WORK` or `SILENT_MODE`.

## 2. Transition Anticipation (Robust Logic)
- **Predictive Layer:** Pre-brief 10-15m before a state change.
- **Cancellation Rule:** Suppress/Cancel brief if:
  - User is already in motion (`activity` matches transition).
  - Meeting start time shifts.
  - Confidence < 0.7.

## 3. Tiered WOW Engine
- **Mini-WOW (P > 0.75):** Lightweight 2-step optimization.
- **Full WOW (P > 0.90):** Full afternoon restructuring proposal.
- **Rule:** Always present as a proposal. Respect the user's "Autonomy Preference" in the phrasing.

## 4. Long-Term Memory (Pattern Confidence)
- **Promotion Criteria:** Only promote a routine (e.g., "Gym at 17:30") if consistency > 70% over 10+ occurrences.
- **Decay:** Patterns not observed for 7 days are demoted to avoid "Stale Intelligence."
""".trimIndent()

                    GuideStepView(
                        number = 1,
                        title = "SOUL.md (Identity + Style + Skills)",
                        description = "Path: ~/.openclaw/workspace/SOUL.md\nThe unified constitution — identity, visual standard, and execution permissions.",
                        files = listOf("SOUL.md" to soulContent)
                    )
                    GuideStepView(
                        number = 2,
                        title = "HEARTBEAT.md (Execution Logic)",
                        description = "Path: ~/.openclaw/workspace/HEARTBEAT.md\nThe orchestration engine — triggers, safeguards, and adaptive learning.",
                        files = listOf("HEARTBEAT.md" to heartbeatContent)
                    )
                    GuideStepView(
                        number = 3,
                        title = "TOOLS.md (Proactive Skill)",
                        description = "Path: ~/.openclaw/workspace/TOOLS.md\nAlso installed via One-Click. Tells the AI how to respond to context syncs.",
                        files = listOf("TOOLS.md" to toolsContent)
                    )
                    GuideStepView(
                        number = 4,
                        title = "Restart Gateway",
                        description = "Restart to apply changes:",
                        code = "openclaw gateway restart"
                    )
                    
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color(0xFF007AFF))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(detail, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun GuideStepView(
    number: Int,
    title: String,
    description: String,
    code: String? = null,
    files: List<Pair<String, String>>? = null
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Blue.copy(alpha = 0.02f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Blue),
                contentAlignment = Alignment.Center
            ) {
                Text(number.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Text(description, fontSize = 14.sp, color = Color.Gray)
        
        if (code != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    code,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .padding(8.dp)
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { 
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                }) {
                    Icon(Icons.Default.ContentCopy, null, tint = Color.Blue)
                }
            }
        }
        
        if (files != null) {
            files.forEach { (filename, content) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(filename, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(content))
                        }) {
                            Text("Copy Content", fontSize = 12.sp)
                        }
                    }
                    Text(
                        content,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.05f))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
