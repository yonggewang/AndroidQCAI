package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import android.content.Intent
import android.net.Uri
import com.quantumproperty.qcai.data.*
import com.quantumproperty.qcai.utils.BrowserUtils
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIRoadmapResultsScreen(
    response: SurveyResponse,
    onBack: () -> Unit
) {
    val engine = remember { AIRecommendationEngine() }
    val context = LocalContext.current
    
    // Generate Recommendation once
    val recommendation = remember { engine.generateRecommendation(response) }
    val (model, hardware, vramNeeded, justification) = recommendation
    
    // Disclaimer & Install Dialog State
    var showDisclaimer by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { showDisclaimer = false },
            title = { Text("Affiliate Disclosure") },
            text = { Text("As an Amazon Associate, I earn from qualifying purchases.") },
            confirmButton = {
                TextButton(onClick = { showDisclaimer = false }) {
                    Text("OK", color = Color(0xFFAF52DE))
                }
            },
            icon = { Icon(Icons.Default.Info, null, tint = Color(0xFFAF52DE)) }
        )
    }

    if (showInstallDialog) {
        ProfessionalInstallDialog(
            onDismiss = { showInstallDialog = false },
            onSendRequest = { name, company, phone ->
                showInstallDialog = false
                val subject = "Professional Install Request: $company"
                val body = """
                    New Installation Request
                    
                    Client: $name
                    Company: $company
                    Phone: $phone
                    
                    Results Context:
                    - Industry: ${response.businessType}
                    - Privacy: ${response.dataPrivacy}
                    - Team Size: ${response.teamSize}
                    
                    Recommended Hardware:
                    - Name: ${hardware.name}
                    - GPU: ${hardware.gpu}
                    - Price: $${hardware.price}
                    
                    Recommended Model:
                    - ${model.name} (${model.parameterSize})
                    
                    Please contact the client to arrange installation.
                """.trimIndent()
                
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("admin@queencityai.net"))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle case where no email app is available
                }
            }
        )
    }
    
    // Calculate ROI
    val roi = remember { 
        engine.calculateROI(
            // currentLaborCost removed
            // Let's use defaults for display or calculate internally if we had inputs. Only hardware cost is known.
            // Wait, iOS had an ROICalculatorView. Android might need one too.
            // For now, let's just use defaults to show *something* or skip if inputs missing.
            // iOS defaults: Labor $40/hr, 10 hrs/week, 30% automation. 
            hourlyRate = 40.0,
            hoursPerWeek = 10.0,
            automationPercent = 30.0,
            hardwareCost = hardware.price
        ) 
    }
    
    // Alternatives
    val alternatives = remember { engine.getAllCompatibleHardware(vramNeeded, HardwareCatalogService.shared.bundles, response.platformPreference) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        // Header
        TopAppBar(
            title = { Text("Your AI Roadmap", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Privacy Badge
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (response.dataPrivacy == DataPrivacy.LOCAL_ONLY) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (response.dataPrivacy == DataPrivacy.LOCAL_ONLY) Icons.Default.Lock else Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (response.dataPrivacy == DataPrivacy.LOCAL_ONLY) Color(0xFF2E7D32) else Color(0xFF1565C0),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (response.dataPrivacy == DataPrivacy.LOCAL_ONLY) "Local / Private AI Recommended" else "Cloud / Hybrid AI Recommended",
                        color = if (response.dataPrivacy == DataPrivacy.LOCAL_ONLY) Color(0xFF2E7D32) else Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // 1.5 Technical Evaluation / Justification
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), // Light Orange
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Technical Evaluation", style = MaterialTheme.typography.titleSmall, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = justification,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // 2. Recommended Model
            item {
                Text("Recommended AI Model", style = MaterialTheme.typography.titleMedium, color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), // Light Blue
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(model.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(model.family, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoBadge(icon = Icons.Default.Memory, text = model.parameterSize)
                            InfoBadge(icon = Icons.Default.Description, text = "${model.contextWindow / 1000}k Context")
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        if (model.isOpenSource) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Free & Open Source", color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }
                        }
                        
                        // Model Docs Link
                        TextButton(onClick = { BrowserUtils.openURL(context, model.modelUrl) }) {
                            Text("View Model Details", fontSize = 12.sp)
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
            
            // 3. Recommended Hardware
            item {
                Text("Recommended Hardware", style = MaterialTheme.typography.titleMedium, color = Color(0xFFAF52DE), fontWeight = FontWeight.Bold)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)), // Light Purple
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(hardware.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(hardware.gpu, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                            Text(
                                NumberFormat.getCurrencyInstance(Locale.US).format(hardware.price),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFFAF52DE),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text("VRAM: ${hardware.vram} GB (Needs ${vramNeeded} GB)", fontSize = 12.sp, color = Color.Gray)
                        
                        // Suitable AI Models Section
                        hardware.aiCompatibility?.let { compatibility ->
                            if (compatibility.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Suitable AI Models",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                                Spacer(Modifier.height(8.dp))
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(compatibility.size) { index ->
                                        val ac = compatibility[index]
                                        Surface(
                                            color = if (ac.recommended) Color(0xFFE8F5E9) else Color(0x11AF52DE),
                                            shape = RoundedCornerShape(16.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp, 
                                                if (ac.recommended) Color(0xFF2E7D32) else Color(0xFFAF52DE).copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (ac.recommended) {
                                                    Icon(
                                                        Icons.Default.Star, 
                                                        null, 
                                                        tint = Color(0xFF2E7D32), 
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = ac.modelId.replace("-", " ").uppercase(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (ac.recommended) Color(0xFF2E7D32) else Color(0xFFAF52DE)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { BrowserUtils.openURL(context, hardware.purchaseUrl) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAF52DE)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ShoppingCart, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Buy from ${hardware.partnerName}")
                            }
                            
                            IconButton(onClick = { showDisclaimer = true }) {
                                Icon(Icons.Default.Info, "Affiliate Disclosure", tint = Color.Gray)
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Professional Install Button
                        Button(
                            onClick = { showInstallDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Green for service
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Build, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Get it Installed Professionally")
                        }
                        Text(
                            "Connect with a Charlotte-based AI specialist to set up your system.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                        
                        androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // DIY Guide Button
                        val diyTemplateId = hardware.diyTemplate
                        Button(
                            onClick = { BrowserUtils.openURL(context, "https://quantumpropertyllc.github.io/aihardware/diy.html?template=$diyTemplateId") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)), // Blue for info/guide
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Build, null)
                            Spacer(Modifier.width(8.dp))
                            Text("DIY your AI Workstation")
                        }
                    }
                }
            }
            
            // 4. ROI Estimate
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Light Green
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ROI Estimate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Annual Savings", fontSize = 12.sp, color = Color.Gray)
                                Text(NumberFormat.getCurrencyInstance(Locale.US).format(roi.annualSavings), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Break-Even", fontSize = 12.sp, color = Color.Gray)
                                Text("${roi.breakEvenMonths.toInt()} Months", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
            
            // 5. Alternative Options
            item {
                Text("Other Compatible Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            items(alternatives.size) { index ->
                val alt = alternatives[index]
                if (alt.id != hardware.id) { // Don't show primary again? iOS showed it if it was in list, but usually logic excluded it or handled it. 
                    // Let's filter out if it IS the primary recommendation to avoid dupe, or just show all sorted.
                    // The engine returns ALL compatible. 
                    // We'll show it if it's NOT the exact same ID, or just show everything as "Options".
                    // But usually "Other" implies distinct from primary.
                    
                    AlternativeHardwareCard(alt, hardware.price, vramNeeded, engine, context, onShowDisclaimer = { showDisclaimer = true })
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun InfoBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun AlternativeHardwareCard(
    hardware: HardwareBundle, 
    primaryPrice: Double, 
    vramNeeded: Int, 
    engine: AIRecommendationEngine,
    context: android.content.Context,
    onShowDisclaimer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(hardware.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(hardware.gpu, fontSize = 12.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        NumberFormat.getCurrencyInstance(Locale.US).format(hardware.price),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAF52DE)
                    )
                    val diff = hardware.price - primaryPrice
                    if (diff > 0) {
                        Text("+${NumberFormat.getCurrencyInstance(Locale.US).format(diff)}", fontSize = 12.sp, color = Color(0xFFFF9800))
                    } else if (diff < 0) {
                         Text("${NumberFormat.getCurrencyInstance(Locale.US).format(diff)}", fontSize = 12.sp, color = Color(0xFF4CAF50))
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            val justification = engine.getUpgradeJustification(hardware, vramNeeded)
            Text("✓ $justification", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
            
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        BrowserUtils.openURL(context, hardware.purchaseUrl)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAF52DE))
                ) {
                    Text("Buy from ${hardware.partnerName}")
                }
                
                IconButton(onClick = onShowDisclaimer) {
                    Icon(Icons.Default.Info, "Affiliate Disclosure", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ProfessionalInstallDialog(
    onDismiss: () -> Unit,
    onSendRequest: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Professional Installation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter your details to connect with a certified installer.")
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendRequest(name, company, phone) },
                enabled = name.isNotBlank() && company.isNotBlank() && phone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Request Info")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        icon = { Icon(Icons.Default.Build, null, tint = Color(0xFF2E7D32)) }
    )
}
