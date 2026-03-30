package com.quantumproperty.qcai.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quantumproperty.qcai.data.ContextEngine
import com.quantumproperty.qcai.data.ContextObject
import com.quantumproperty.qcai.data.DataDomain
import com.quantumproperty.qcai.data.OpenClawService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContextOSViewModel(application: Application) : AndroidViewModel(application) {
    private val contextEngine = ContextEngine.getInstance(application)
    val currentContext: StateFlow<ContextObject?> = contextEngine.currentContext

    var healthEnabled by mutableStateOf(contextEngine.healthEnabled)
    var temporalEnabled by mutableStateOf(contextEngine.temporalEnabled)
    var attentionEnabled by mutableStateOf(contextEngine.attentionEnabled)
    var motionEnabled by mutableStateOf(contextEngine.motionEnabled)
    var visionEnabled by mutableStateOf(contextEngine.visionEnabled)
    var locationEnabled by mutableStateOf(contextEngine.locationEnabled)
    var locationExactEnabled by mutableStateOf(contextEngine.locationExactEnabled)
    var showSetupGuide by mutableStateOf(false)

    // Remote Intelligence Content
    var soulContent by mutableStateOf<String?>(null)
    var heartbeatContent by mutableStateOf<String?>(null)
    var toolsContent by mutableStateOf<String?>(null)        // Windows version
    var unixToolsContent by mutableStateOf<String?>(null)   // Linux/macOS version
    var logicPyContent by mutableStateOf<String?>(null)
    var isFetchingIntelligence by mutableStateOf(false)

    init {
        // Request permissions for all domains once at startup
        contextEngine.requestPermissions()
    }

    fun toggleDomain(domain: DataDomain) {
        when (domain) {
            DataDomain.BIOMETRICS -> {
                healthEnabled = !healthEnabled
                contextEngine.healthEnabled = healthEnabled
            }
            DataDomain.TEMPORAL -> {
                temporalEnabled = !temporalEnabled
                contextEngine.temporalEnabled = temporalEnabled
            }
            DataDomain.ATTENTION -> {
                attentionEnabled = !attentionEnabled
                contextEngine.attentionEnabled = attentionEnabled
            }
            DataDomain.MOTION -> {
                motionEnabled = !motionEnabled
                contextEngine.motionEnabled = motionEnabled
            }
            DataDomain.VISION -> {
                visionEnabled = !visionEnabled
                contextEngine.visionEnabled = visionEnabled
            }
            DataDomain.LOCATION -> {
                locationEnabled = !locationEnabled
                contextEngine.locationEnabled = locationEnabled
            }
        }
    }

    fun toggleLocationExact() {
        locationExactEnabled = !locationExactEnabled
        contextEngine.locationExactEnabled = locationExactEnabled
    }

    fun manualSync() {
        viewModelScope.launch {
            val context = contextEngine.ingest()
            OpenClawService.instance.syncContext(context)
        }
    }

    val isGatewayLinked: Boolean
        get() = OpenClawService.instance.isConnected
        
    val latestInsight: StateFlow<String?>
        get() = OpenClawService.instance.latestInsight

    var isInstallingQCAI by mutableStateOf(false)
    var installationError by mutableStateOf<String?>(null)
    var installationSuccess by mutableStateOf(false)

    fun fetchIntelligenceFiles() {
        if (isFetchingIntelligence) return
        
        viewModelScope.launch {
            isFetchingIntelligence = true
            val files = listOf(
                "SOUL.md"       to "https://qcai-net.github.io/openclaw/SOUL.md",
                "HEARTBEAT.md" to "https://qcai-net.github.io/openclaw/HEARTBEAT.md",
                "TOOLS.md"      to "https://qcai-net.github.io/openclaw/TOOLS.md",
                "unixTOOLS.md" to "https://qcai-net.github.io/openclaw/unixTOOLS.md",
                "logic.py"      to "https://qcai-net.github.io/openclaw/logic.py"
            )
            
            withContext(Dispatchers.IO) {
                files.forEach { (name, urlString) ->
                    try {
                        val content = java.net.URL(urlString).readText()
                        when (name) {
                            "SOUL.md"       -> soulContent = content
                            "HEARTBEAT.md" -> heartbeatContent = content
                            "TOOLS.md"      -> toolsContent = content
                            "unixTOOLS.md" -> unixToolsContent = content
                            "logic.py"      -> logicPyContent = content
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            isFetchingIntelligence = false
        }
    }

    private suspend fun fetchIntelligenceFilesAsync() {
        val files = listOf(
            "SOUL.md"       to "https://qcai-net.github.io/openclaw/SOUL.md",
            "HEARTBEAT.md" to "https://qcai-net.github.io/openclaw/HEARTBEAT.md",
            "TOOLS.md"      to "https://qcai-net.github.io/openclaw/TOOLS.md",
            "unixTOOLS.md" to "https://qcai-net.github.io/openclaw/unixTOOLS.md",
            "logic.py"      to "https://qcai-net.github.io/openclaw/logic.py"
        )
        
        withContext(Dispatchers.IO) {
            files.forEach { (name, urlString) ->
                try {
                    val content = java.net.URL(urlString).readText()
                    when (name) {
                        "SOUL.md"       -> soulContent = content
                        "HEARTBEAT.md" -> heartbeatContent = content
                        "TOOLS.md"      -> toolsContent = content
                        "unixTOOLS.md" -> unixToolsContent = content
                        "logic.py"      -> logicPyContent = content
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /** Install for Windows gateway (uses TOOLS.md). */
    fun installQCAIGateway() = installQCAIGateway(forLinux = false)

    /** Install intelligence files with platform selection.
     * @param forLinux If true, installs unixTOOLS.md (as TOOLS.md) for Linux/macOS gateways. */
    fun installQCAIGateway(forLinux: Boolean) {
        if (!isGatewayLinked) {
            installationError = "Not connected to Gateway"
            return
        }
        
        isInstallingQCAI = true
        installationError = null
        installationSuccess = false

        viewModelScope.launch {
            // Ensure we have the latest content from the web
            if (soulContent == null || heartbeatContent == null ||
                toolsContent == null || unixToolsContent == null) {
                fetchIntelligenceFilesAsync()
            }
            
            val soul = soulContent
            val heartbeat = heartbeatContent

            if (soul == null || heartbeat == null) {
                installationError = "Could not retrieve setup files from web."
                isInstallingQCAI = false
                return@launch
            }

            // Choose correct TOOLS.md: unixTOOLS for Linux/Mac, TOOLS.md for Windows
            val tools = if (forLinux) unixToolsContent else toolsContent
            if (tools == null) {
                installationError = if (forLinux)
                    "Could not retrieve Linux/macOS TOOLS file."
                else
                    "Could not retrieve Windows TOOLS file."
                isInstallingQCAI = false
                return@launch
            }

            try {
                OpenClawService.instance.autoInstallQCAI(
                    soul = soul,
                    heartbeat = heartbeat,
                    tools = tools
                )
                installationSuccess = true
            } catch (e: Exception) {
                installationError = e.message ?: "Installation failed"
            } finally {
                isInstallingQCAI = false
            }
        }
    }
}
