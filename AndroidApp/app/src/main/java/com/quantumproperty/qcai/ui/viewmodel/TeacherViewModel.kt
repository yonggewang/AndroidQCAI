package com.quantumproperty.qcai.ui.viewmodel

import com.quantumproperty.qcai.utils.BrowserUtils
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quantumproperty.qcai.data.AIEngine
import com.quantumproperty.qcai.data.AIService
import com.quantumproperty.qcai.data.AITopic
import com.quantumproperty.qcai.data.AppLanguage
import com.quantumproperty.qcai.data.ChatMessage
import com.quantumproperty.qcai.data.HotToolItem
import com.quantumproperty.qcai.data.TopMenuItem
import com.quantumproperty.qcai.data.Recommendation
import com.quantumproperty.qcai.data.PreferenceManager
import kotlinx.coroutines.Dispatchers
import com.quantumproperty.qcai.utils.SpeechManager
import com.quantumproperty.qcai.utils.TTSManager
import com.quantumproperty.qcai.data.UserManager
import com.quantumproperty.qcai.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import com.quantumproperty.qcai.data.OpenClawService
import com.quantumproperty.qcai.data.ConnectionState
import com.quantumproperty.qcai.data.GatewayMetrics
import android.content.Intent
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

enum class DisplayMode {
    WEB, CHAT
}

class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    private val aiService by lazy { AIService() }
    private var ttsManager: TTSManager? = null
    private var speechManager: SpeechManager? = null
    private var userManager: UserManager? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage = _appLanguage.asStateFlow()

    private val _selectedEngine = MutableStateFlow(AIEngine.GEMINI)
    val selectedEngine = _selectedEngine.asStateFlow()

    private val _topMenuItems = MutableStateFlow<List<TopMenuItem>>(emptyList())
    val topMenuItems = _topMenuItems.asStateFlow()

    private val _isAutoPlayNews = MutableStateFlow(false)
    val isAutoPlayNews = _isAutoPlayNews.asStateFlow()

    fun setAutoPlayNews(enabled: Boolean) {
        _isAutoPlayNews.value = enabled
        if (!enabled) {
            stopNewsSummary()
        }
    }
    private var lastPlaybackStartTime: Long = 0

    private val _selectedTopic = MutableStateFlow(AITopic.CLT_VIBE)
    val selectedTopic = _selectedTopic.asStateFlow()

    private val _displayMode = MutableStateFlow(DisplayMode.WEB)
    val displayMode = _displayMode.asStateFlow()

    // Tab Navigation State (0: Home, 1: Vibe, 2: Hub, 3: Toolkit, 4: More)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    private val _currentWebUrl = MutableStateFlow<String?>(null)
    val currentWebUrl = _currentWebUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _showAddressInput = MutableStateFlow(false)
    val showAddressInput = _showAddressInput.asStateFlow()

    private val _showAPIKeySetup = MutableStateFlow(false)
    val showAPIKeySetup = _showAPIKeySetup.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow("访客")
    val userName = _userName.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog = _showLoginDialog.asStateFlow()
    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog = _showProfileDialog.asStateFlow()

    private val _showRegisterDialog = MutableStateFlow(false)
    val showRegisterDialog = _showRegisterDialog.asStateFlow()

    private val _apiKeySetupReason = MutableStateFlow<String?>(null)
    val apiKeySetupReason = _apiKeySetupReason.asStateFlow()

    // OpenClaw States
    private val openClawService = OpenClawService.instance
    private val _openClawState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val openClawState = _openClawState.asStateFlow()
    
    val gatewayAuthKey = MutableStateFlow("")
    val gatewayHostname = MutableStateFlow("")
    val gatewayPort = MutableStateFlow("443")
    val gatewayToken = MutableStateFlow("")
    
    private val _openClawMetrics = MutableStateFlow(GatewayMetrics())
    val openClawMetrics = _openClawMetrics.asStateFlow()

    private val _openClawError = MutableStateFlow<String?>(null)
    val openClawError = _openClawError.asStateFlow()
    
    val autoConnectGateway = MutableStateFlow(false)
    
    val portalHost = MutableStateFlow(PreferenceManager.portalHost)
    val portalPort = MutableStateFlow(PreferenceManager.portalPort)
    
    fun updatePortalHost(host: String) {
        portalHost.value = host
        PreferenceManager.portalHost = host
    }

    fun updatePortalPort(port: String) {
        portalPort.value = port
        PreferenceManager.portalPort = port
    }

    private var reconnectAttempt = 0

    private val _isPairingRequired = MutableStateFlow(false)
    val isPairingRequired = _isPairingRequired.asStateFlow()

    private val _deviceId = MutableStateFlow("")
    val deviceId = _deviceId.asStateFlow()

    private val _showQRScanner = MutableStateFlow(false)
    val showQRScanner = _showQRScanner.asStateFlow()

    private val _showJoinGatewayDialog = MutableStateFlow(false)
    val showJoinGatewayDialog = _showJoinGatewayDialog.asStateFlow()

    private val _gatewayMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val gatewayMessages = _gatewayMessages.asStateFlow()

    private val _gatewayStreamingText = MutableStateFlow<String?>(null)
    val gatewayStreamingText = _gatewayStreamingText.asStateFlow()

    private val _showGatewayChat = MutableStateFlow(false)
    val showGatewayChat = _showGatewayChat.asStateFlow()

    // OpenClaw UI States for Screen Parity
    private val _isGatewayLinked = MutableStateFlow(false)
    val isGatewayLinked = _isGatewayLinked.asStateFlow()

    private val _isTunnelConnected = MutableStateFlow(false)
    val isTunnelConnected = _isTunnelConnected.asStateFlow()
    
    private val _gatewayCommand = MutableStateFlow("openclaw gateway --tailscale serve")
    val gatewayCommand = _gatewayCommand.asStateFlow()
    
    private val _tunnelIP = MutableStateFlow("100.x.x.x")
    val tunnelIP = _tunnelIP.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting = _isConnecting.asStateFlow()
    
    private val _gatewayLinkError = MutableStateFlow<String?>(null)
    val gatewayLinkError = _gatewayLinkError.asStateFlow()

    private val _showSetupGuide = MutableStateFlow(false)
    val showSetupGuide = _showSetupGuide.asStateFlow()

    private val _showConfigPreview = MutableStateFlow(false)
    val showConfigPreview = _showConfigPreview.asStateFlow()

    private val _hotListItems = MutableStateFlow<List<HotToolItem>>(emptyList())
    // Allow observing raw items if needed, but UI should likely use userSpecificTools
    val hotListItems = _hotListItems.asStateFlow()

    // Filtered list based on VIP level
    val userSpecificTools = combine(_hotListItems, _userProfile) { items, profile ->
        if (profile != null && profile.vipLevel > 0) {
            items
        } else {
            emptyList()
        }
    }

    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations = _recommendations.asStateFlow()

    private val _vibeHistory = MutableStateFlow<List<Recommendation>>(emptyList())
    val vibeHistory = _vibeHistory.asStateFlow()

    private val _showAIRealEstateTools = MutableStateFlow(false)
    val showAIRealEstateTools = _showAIRealEstateTools.asStateFlow()
    
    // Community Features - Events, Marketplace, Rentals
    private val _showEventsView = MutableStateFlow(false)
    val showEventsView = _showEventsView.asStateFlow()
    
    private val _showMarketplaceView = MutableStateFlow(false)
    val showMarketplaceView = _showMarketplaceView.asStateFlow()
    
    private val _showRentalsView = MutableStateFlow(false)
    val showRentalsView = _showRentalsView.asStateFlow()

    private val _showCollegeAdmissions = MutableStateFlow(false)
    val showCollegeAdmissions = _showCollegeAdmissions.asStateFlow()

    private val _showTheSceneView = MutableStateFlow(false)
    val showTheSceneView = _showTheSceneView.asStateFlow()
    
    private val _sceneData = MutableStateFlow<com.quantumproperty.qcai.data.SceneResponse?>(null)
    val sceneData = _sceneData.asStateFlow()

    // AI Roadmap
    private val _showAIRoadmapView = MutableStateFlow(false)
    val showAIRoadmapView = _showAIRoadmapView.asStateFlow()

    private val _aiRoadmapResponse = MutableStateFlow<com.quantumproperty.qcai.data.SurveyResponse?>(null)
    val aiRoadmapResponse = _aiRoadmapResponse.asStateFlow()

    fun openAIRoadmap(context: android.content.Context) {
        val url = when (_appLanguage.value) {
            AppLanguage.CHINESE -> "https://qcai-net.github.io/aihardware/aistrategy_CN.html"
            AppLanguage.SPANISH -> "https://qcai-net.github.io/aihardware/aistrategy_ES.html"
            else -> "https://qcai-net.github.io/aihardware/aistrategy.html"
        }
        com.quantumproperty.qcai.utils.BrowserUtils.openURL(context, url)
    }

    fun closeAIRoadmap() {
        _showAIRoadmapView.value = false
        _aiRoadmapResponse.value = null // Clear state to allow retaking survey next time
    }

    private val _showPortalSetup = MutableStateFlow(false)
    val showPortalSetup = _showPortalSetup.asStateFlow()

    fun setShowPortalSetup(show: Boolean) {
        _showPortalSetup.value = show
    }

    private val _showContextOSView = MutableStateFlow(false)
    val showContextOSView = _showContextOSView.asStateFlow()

    fun openContextOS() {
        _showContextOSView.value = true
    }

    fun closeContextOS() {
        _showContextOSView.value = false
    }

    fun openPortal(context: android.content.Context) {
        // On Android, Tailscale runs as a system VPN service, so Chrome Custom Tabs
        // can reach Tailscale hostnames directly (unlike iOS where we need a local proxy).
        // We mirror openWebConsole()'s approach: correct scheme + auth token.
        val customHost = portalHost.value.trim()
        val customPort = portalPort.value.trim()

        // Determine target host (custom override or gateway hostname)
        var host = if (customHost.isNotEmpty()) customHost else gatewayHostname.value.trim()

        // Strip any accidentally included scheme
        if (host.startsWith("https://", ignoreCase = true)) host = host.removePrefix("https://")
        else if (host.startsWith("http://", ignoreCase = true)) host = host.removePrefix("http://")

        if (host.isEmpty()) {
            android.util.Log.w("TeacherViewModel", "openPortal: No host configured, skipping.")
            return
        }

        // Determine port — default 18790 for the portal (Node.js system)
        val port = customPort.toIntOrNull() ?: 18790

        // Use https ONLY for port 443 (standard TLS), http for others like 18790
        val isTailscale = host.contains(".ts.net") || host.startsWith("100.")
        val scheme = if (port == 443) "https" else "http"
        val portPart = if (port == 443 || port == 80) "" else ":$port"

        // Include the auth token so the portal can authenticate the session
        val token = gatewayToken.value.trim()
        val tokenPart = if (token.isNotEmpty()) "?token=$token" else ""

        val url = "$scheme://$host$portPart/$tokenPart"
        android.util.Log.i("TeacherViewModel", "🚀 Portal: Opening $url (isTailscale=$isTailscale)")
        BrowserUtils.openURL(context, url)
    }

    fun openPortalInfo(context: android.content.Context) {
        com.quantumproperty.qcai.utils.BrowserUtils.openURL(context, "http://qcai-net.github.io/clawportal.html")
    }

    private val _gatewayInputText = MutableStateFlow("")
    val gatewayInputText = _gatewayInputText.asStateFlow()
    
    fun setGatewayInputText(text: String) {
        _gatewayInputText.value = text
    }

    private val _recordingTarget = MutableStateFlow<String?>(null)
    val recordingTarget = _recordingTarget.asStateFlow()

    fun submitAISurvey(response: com.quantumproperty.qcai.data.SurveyResponse) {
        _aiRoadmapResponse.value = response
    }



    private val _dailyBriefEN = MutableStateFlow<com.quantumproperty.qcai.data.DailyBriefResponse?>(null)
    private val _dailyBriefCN = MutableStateFlow<com.quantumproperty.qcai.data.DailyBriefResponse?>(null)
    private val _dailyBriefES = MutableStateFlow<com.quantumproperty.qcai.data.DailyBriefResponse?>(null)

    val currentDailyBrief = combine(_appLanguage, _dailyBriefEN, _dailyBriefCN, _dailyBriefES) { lang, en, cn, es ->
        when (lang) {
            AppLanguage.CHINESE -> cn
            AppLanguage.SPANISH -> es
            else -> en
        }
    }

    private val _dailyBrief = MutableStateFlow<com.quantumproperty.qcai.data.DailyBriefResponse?>(null)
    val dailyBrief = _dailyBrief.asStateFlow()

    private val _aiNewsArticles = MutableStateFlow<List<com.quantumproperty.qcai.data.AINewsArticle>>(emptyList())
    val aiNewsArticles = _aiNewsArticles.asStateFlow()

    private val _verifiedProfessionals = MutableStateFlow<List<com.quantumproperty.qcai.data.Professional>>(emptyList())
    val verifiedProfessionals = _verifiedProfessionals.asStateFlow()

    fun fetchHardwareCatalog() {
        viewModelScope.launch {
            com.quantumproperty.qcai.data.HardwareCatalogService.shared.fetchCatalog()
            _verifiedProfessionals.value = com.quantumproperty.qcai.data.HardwareCatalogService.shared.professionals
        }
    }

    fun openProfessionalProfile(context: android.content.Context, id: String? = null) {
        val url = if (id != null) {
            "https://qcai-net.github.io/aihardware/pro.html?id=$id"
        } else {
            "https://qcai-net.github.io/aihardware/pro.html"
        }
        com.quantumproperty.qcai.utils.BrowserUtils.openURL(context, url)
    }

    fun fetchDailyBrief(force: Boolean = false) {
        viewModelScope.launch {
            try {
                // Fetch all 3 in parallel
                launch {
                    try {
                        _dailyBriefEN.value = com.quantumproperty.qcai.data.CityOSService.instance.fetchDailyBrief(language = "en", forceRefresh = force)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                launch {
                    try {
                        _dailyBriefCN.value = com.quantumproperty.qcai.data.CityOSService.instance.fetchDailyBrief(language = "cn", forceRefresh = force)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                launch {
                    try {
                        _dailyBriefES.value = com.quantumproperty.qcai.data.CityOSService.instance.fetchDailyBrief(language = "es", forceRefresh = force)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun fetchAINewsArticles() {
        viewModelScope.launch {
            try {
                val langCode = when (_appLanguage.value) {
                    AppLanguage.CHINESE -> "zh"
                    AppLanguage.SPANISH -> "es"
                    else -> "en"
                }
                _aiNewsArticles.value = com.quantumproperty.qcai.data.CityOSService.instance.fetchAINewsArticles(langCode)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private val _showNewsLocalLifeView = MutableStateFlow(false)
    val showNewsLocalLifeView = _showNewsLocalLifeView.asStateFlow()

    fun openNewsLocalLife() {
        _showNewsLocalLifeView.value = true
    }

    fun closeNewsLocalLife() {
        _showNewsLocalLifeView.value = false
    }

    fun openTheScene() {
        _showTheSceneView.value = true
    }

    fun closeTheScene() {
        _showTheSceneView.value = false
    }

    // Play summary for specific items (News & Local Life)
    private var newsSummaryItems: List<TopMenuItem> = emptyList()
    private var newsSummaryIndex = 0
    private val _isNewsSummaryReading = MutableStateFlow(false)
    val isNewsSummaryReading = _isNewsSummaryReading.asStateFlow()

    fun playNewsSummary(items: List<TopMenuItem>) {
        if (_isNewsSummaryReading.value) {
            stopNewsSummary()
            return
        }

        if (ttsManager?.isReady() != true) {
             val isChinese = _appLanguage.value == AppLanguage.CHINESE
             showError(if (isChinese) "语音服务未就绪" else "TTS Service Not Ready")
             return
        }

        stopSequentialListen() // Stop generic listener if active
        ttsManager?.stop()

        newsSummaryItems = items
        newsSummaryIndex = 0
        _isNewsSummaryReading.value = true
        
        // Listen for completion
        ttsManager?.onSpeechCompleted = { id ->
            if (_isNewsSummaryReading.value) {
                 if (id == "NEWS_ITEM_DONE_$newsSummaryIndex") {
                    viewModelScope.launch {
                        newsSummaryIndex++
                        if (newsSummaryIndex < newsSummaryItems.size) {
                             readNextNewsSummaryItem()
                        } else {
                            _isNewsSummaryReading.value = false
                        }
                    }
                 }
            }
        }
        
        readNextNewsSummaryItem()
        lastPlaybackStartTime = System.currentTimeMillis()
    }

    fun stopNewsSummaryGracefully() {
        val now = System.currentTimeMillis()
        if (now - lastPlaybackStartTime > 3000) {
            stopNewsSummary()
        }
    }

    private fun stripHTML(html: String): String {
        return try {
            // First replace typical block wrappers with newline markers so Jsoup text() doesn't merge them
            val preprocessed = html
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
                .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
                
            val doc = org.jsoup.Jsoup.parse(preprocessed)
            // Remove scripts, styles, etc.
            doc.select("script, style, head, iframe, noscript, svg").remove()
            
            // Jsoup.wholeText() preserves existing text nodes' newlines
            val text = doc.wholeText()
                .replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ")
                .replace(Regex("[ \\t]+"), " ")
                .replace(Regex("\\n\\s+"), "\n")
                .replace(Regex("\\n{3,}"), "\n\n")
                .trim()
            text
        } catch (e: Exception) {
            html.replace(Regex("<!--[\\s\\S]*?-->"), " ")
                .replace(Regex("<script[\\s\\S]*?<\\/script>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<style[\\s\\S]*?<\\/style>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }

    fun stopNewsSummary() {
        _isNewsSummaryReading.value = false
        ttsManager?.stop()
        _isLoading.value = false
    }

    private fun readNextNewsSummaryItem() {
        if (newsSummaryIndex >= newsSummaryItems.size) return
        
        val item = newsSummaryItems[newsSummaryIndex]
        val isChinese = _appLanguage.value == AppLanguage.CHINESE
        val isSpanish = _appLanguage.value == AppLanguage.SPANISH
        val url = if (isChinese) item.chineseUrl else if (isSpanish && item.spanishUrl.isNotEmpty()) item.spanishUrl else item.englishUrl
        
        if (url.isEmpty()) {
             // Skip if no URL
             newsSummaryIndex++
             readNextNewsSummaryItem()
             return
        }

        val title = if (isChinese) item.chineseName else item.englishName
        val intro = "$title. \n\n"
        
        _isLoading.value = true
        ttsManager?.speak(intro, null, android.speech.tts.TextToSpeech.QUEUE_FLUSH) // Flush to start fresh

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                    .timeout(10000)
                    .get()
                
                // Improved summary extraction
                val summaryText = stripHTML(doc.html())
                
                // Take first 1000 chars for a meaningful but not infinite summary
                val summary = if (summaryText.length > 1000) {
                    summaryText.substring(0, 1000) + "..."
                } else {
                    summaryText
                }
                
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    if (summary.isNotBlank()) {
                         ttsManager?.speak(summary, "NEWS_ITEM_DONE_$newsSummaryIndex", android.speech.tts.TextToSpeech.QUEUE_ADD)
                    } else {
                         // Skip if no content extracted
                         newsSummaryIndex++
                         readNextNewsSummaryItem()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    // Skip on error
                    newsSummaryIndex++
                    readNextNewsSummaryItem()
                }
            }
        }
    }

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch = _speechPitch.asStateFlow()

    private val _isSpeechEnabled = MutableStateFlow(false)
    val isSpeechEnabled = _isSpeechEnabled.asStateFlow()

    val isRecording: StateFlow<Boolean> 
        get() = speechManager?.isRecording ?: MutableStateFlow(false).asStateFlow()
    val transcript: StateFlow<String> 
        get() = speechManager?.transcript ?: MutableStateFlow("").asStateFlow()

    init {
        // PreferenceManager.init(application) // Now initialized in MainActivity for safety
        
        // Load Tailscale settings (Must be AFTER PreferenceManager.init)
        gatewayAuthKey.value = PreferenceManager.tailscaleAuthKey
        gatewayHostname.value = PreferenceManager.tailscaleHostname
        gatewayPort.value = PreferenceManager.tailscaleGatewayPort
        gatewayToken.value = PreferenceManager.tailscaleGatewayToken
        autoConnectGateway.value = PreferenceManager.autoConnectGateway

        startTailscaleMonitoring()
        
        // Load engine from preferences
        try {
            _selectedEngine.value = AIEngine.valueOf(PreferenceManager.selectedEngine)
        } catch (e: Exception) {
            _selectedEngine.value = AIEngine.GEMINI
        }
        
        // Load speech settings
        _speechRate.value = PreferenceManager.speechRate
        _speechPitch.value = PreferenceManager.speechPitch
        ttsManager?.updateConfig(_speechRate.value, _speechPitch.value)

        // Persist Tailscale settings when they change
        viewModelScope.launch {
            gatewayAuthKey.collect { PreferenceManager.tailscaleAuthKey = it }
        }
        viewModelScope.launch {
            gatewayHostname.collect { PreferenceManager.tailscaleHostname = it }
        }
        viewModelScope.launch {
            gatewayPort.collect { PreferenceManager.tailscaleGatewayPort = it }
        }
        viewModelScope.launch {
            gatewayToken.collect { PreferenceManager.tailscaleGatewayToken = it }
        }
        viewModelScope.launch {
            autoConnectGateway.collect { PreferenceManager.autoConnectGateway = it }
        }
        
        // Context OS periodic sync loop
        viewModelScope.launch {
            while (true) {
                if (_isGatewayLinked.value) {
                    try {
                        val engine = com.quantumproperty.qcai.data.ContextEngine.getInstance(application)
                        val contextData = engine.ingest()
                        openClawService.syncContext(contextData)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                kotlinx.coroutines.delay(60_000L) // every 60s
            }
        }
        
        // Initial URL setup
        // Initial URL setup
        updateDisplayMode(AITopic.CLT_VIBE)
        refreshHotToolList()
        fetchHardwareCatalog()
        
        // Robust fetch with retry for startup
        viewModelScope.launch {
            fetchTopMenu() // First attempt
            
            // Retry check: If network was slow or not ready, try again after a few seconds
            kotlinx.coroutines.delay(2000)
            if (_topMenuItems.value.isEmpty()) {
                fetchTopMenu()
            }
            
            // Final backup retry
            kotlinx.coroutines.delay(3000)
            if (_topMenuItems.value.isEmpty()) {
                fetchTopMenu()
            }
        }
        
        
        
        
        try {
           ttsManager = TTSManager(
               context = application,
               onInitSuccess = {
                   // TTS initialized successfully
                   android.util.Log.d("TeacherViewModel", "✅ TTS ready for use")
                   // Set correct language immediately
                   val locale = if (_appLanguage.value == AppLanguage.CHINESE) java.util.Locale.CHINESE else java.util.Locale.US
                   ttsManager?.setLanguage(locale)
               },
               onInitFailure = { error ->
                   // Show user-friendly error message
                   val isChinese = _appLanguage.value == AppLanguage.CHINESE
                   val message = if (isChinese) {
                       "⚠️ 语音播报不可用\n\n" +
                       "这可能是因为：\n" +
                       "1. 模拟器通常缺少 Google TTS 服务\n" +
                       "2. TTS 语音引擎未安装\n\n" +
                       "建议：\n" +
                       "• 在真机上测试语音功能\n" +
                       "• 或从 Play 商店安装 \"Google TTS\"\n\n" +
                       "详细错误：$error"
                   } else {
                       "⚠️ Text-to-Speech Unavailable\n\n" +
                       "This may be because:\n" +
                       "1. Emulators often lack Google TTS service\n" +
                       "2. TTS engine is not installed\n\n" +
                       "Suggestions:\n" +
                       "• Test audio features on a real device\n" +
                       "• Or install \"Google Text-to-Speech\" from Play Store\n\n" +
                       "Technical error: $error"
                   }
                   _errorMessage.value = message
               }
           )
        } catch (e: Exception) { 
            e.printStackTrace()
            val isChinese = _appLanguage.value == AppLanguage.CHINESE
            _errorMessage.value = if (isChinese) {
                "无法初始化语音服务。请在真机上测试。"
            } else {
                "Failed to initialize TTS. Please test on a real device."
            }
        }

        try {
           speechManager = SpeechManager(application)
        } catch (e: Exception) { e.printStackTrace() }

        try {
            userManager = UserManager()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        viewModelScope.launch {
            userManager?.authState?.collect { profile ->
                _userProfile.value = profile
                _isLoggedIn.value = profile != null
                
                if (profile != null) {
                    _userName.value = if (profile.username.isNotBlank()) profile.username else profile.fullName
                    // Auto-refresh hot list when user is detected (auto-login or manual)
                    refreshHotToolList()
                } else {
                    val isChinese = _appLanguage.value == AppLanguage.CHINESE
                    _userName.value = if (isChinese) "访客" else "Guest"
                }
            }
        }
        
        viewModelScope.launch {
            transcript.collect { text ->
                if (text.isNotEmpty()) {
                    if (_recordingTarget.value == "OpenClawChat") {
                        _gatewayInputText.value = text
                    }
                }
            }
        }
        
        // Initialize OpenClaw Service
        openClawService.init(application)

        // OpenClaw Listener
        openClawService.addListener(object : OpenClawService.OpenClawListener {
            override fun onStateChanged(state: ConnectionState) {
                _openClawState.value = state
                if (state == ConnectionState.CONNECTED) {
                    reconnectAttempt = 0
                    _isGatewayLinked.value = true
                    _isPairingRequired.value = false
                    
                    // Fire an initial sync right upon connection
                    viewModelScope.launch {
                        try {
                            val engine = com.quantumproperty.qcai.data.ContextEngine.getInstance(application)
                            val contextData = engine.ingest()
                            openClawService.syncContext(contextData)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                } else if (state == ConnectionState.DISCONNECTED) {
                    _isGatewayLinked.value = false
                    _isPairingRequired.value = false
                    
                    if (autoConnectGateway.value) {
                        viewModelScope.launch {
                            val shift = reconnectAttempt.coerceAtMost(4)
                            val delayMs = (5000L * (1 shl shift)).coerceAtMost(60000L) // 5s, 10s, 20s, 40s, 60s
                            reconnectAttempt++
                            
                            kotlinx.coroutines.delay(delayMs)
                            if (autoConnectGateway.value && !_isGatewayLinked.value) {
                                linkGateway()
                            }
                        }
                    }
                }
            }
            override fun onMetricsUpdated(metrics: GatewayMetrics) {
                _openClawMetrics.value = metrics
            }
            override fun onPairingRequired(deviceId: String) {
                // Show Step 4 or a notice
                _isPairingRequired.value = true
                _isGatewayLinked.value = false
                _deviceId.value = deviceId
            }
            override fun onChatEvent(event: OpenClawService.ChatEvent) {
                viewModelScope.launch {
                    handleChatEvent(event)
                }
            }
            override fun onError(message: String) {
                _openClawError.value = message
                // Don't show full screen alert for minor websocket errors, just a toast or log
                android.util.Log.e("TeacherViewModel", "OpenClaw Error: $message")
            }
        })
        
        loadVibeHistory()
    }

    private fun loadVibeHistory() {
        try {
            val json = PreferenceManager.vibeHistoryJson
            val array = org.json.JSONArray(json)
            val history = mutableListOf<Recommendation>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                history.add(Recommendation(
                    name = obj.getString("name"),
                    score = obj.getInt("score"),
                    reason = obj.getString("reason"),
                    price = obj.optString("price").takeIf { it.isNotEmpty() },
                    rating = obj.optString("rating").takeIf { it.isNotEmpty() },
                    imageUrl = obj.optString("imageUrl").takeIf { it.isNotEmpty() }
                ))
            }
            _vibeHistory.value = history
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun saveVibeHistory() {
        try {
            val array = org.json.JSONArray()
            _vibeHistory.value.forEach { rec ->
                val obj = org.json.JSONObject()
                obj.put("name", rec.name)
                obj.put("score", rec.score)
                obj.put("reason", rec.reason)
                obj.put("price", rec.price)
                obj.put("rating", rec.rating)
                obj.put("imageUrl", rec.imageUrl)
                array.put(obj)
            }
            PreferenceManager.vibeHistoryJson = array.toString()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun parseRecommendations(text: String) {
        if (!text.contains("MATCH_SCORE_JSON")) return
        
        try {
            val startIdx = text.indexOf("MATCH_SCORE_JSON")
            val jsonPart = text.substring(startIdx)
            val firstBrace = jsonPart.indexOf("{")
            val lastBrace = jsonPart.lastIndexOf("}")
            if (firstBrace == -1 || lastBrace == -1) return
            
            val jsonStr = jsonPart.substring(firstBrace, lastBrace + 1)
            val json = org.json.JSONObject(jsonStr)
            
            // Handle both single object or array
            val recList = mutableListOf<Recommendation>()
            if (json.has("recommendations")) {
                val arr = json.getJSONArray("recommendations")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    recList.add(Recommendation(
                        name = obj.getString("name"), 
                        score = obj.getInt("score"), 
                        reason = obj.getString("reason"),
                        price = obj.optString("price").takeIf { it.isNotEmpty() },
                        rating = obj.optString("rating").takeIf { it.isNotEmpty() },
                        imageUrl = obj.optString("image_url").takeIf { it.isNotEmpty() } ?: obj.optString("imageUrl").takeIf { it.isNotEmpty() }
                    ))
                }
            } else if (json.has("name") && json.has("score")) {
                recList.add(Recommendation(
                    name = json.getString("name"), 
                    score = json.getInt("score"), 
                    reason = json.getString("reason"),
                    price = json.optString("price").takeIf { it.isNotEmpty() },
                    rating = json.optString("rating").takeIf { it.isNotEmpty() },
                    imageUrl = json.optString("image_url").takeIf { it.isNotEmpty() } ?: json.optString("imageUrl").takeIf { it.isNotEmpty() }
                ))
            }
            
            if (recList.isNotEmpty()) {
                _recommendations.value = recList
                
                // Update history
                val currentHistory = _vibeHistory.value.toMutableList()
                recList.forEach { rec ->
                    if (!currentHistory.any { it.name == rec.name }) {
                        currentHistory.add(0, rec)
                    }
                }
                
                val limitedHistory = if (currentHistory.size > 10) currentHistory.take(10) else currentHistory
                _vibeHistory.value = limitedHistory
                saveVibeHistory()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun clearVibeHistory() {
        _vibeHistory.value = emptyList()
        saveVibeHistory()
    }

    fun clearMessages() {
        _messages.value = emptyList()
        _recommendations.value = emptyList()
    }

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        // Immediately refresh current URL and topic if needed
        updateDisplayMode(_selectedTopic.value)
        
        // Update user status string
        if (!_isLoggedIn.value) {
            _userName.value = if (language == AppLanguage.CHINESE) "访客" else "Guest"
        } else {
             // Refresh logged in name if needed, though profile name doesn't change with language usually
        }

        // Update TTS language
        val locale = when (language) {
            AppLanguage.CHINESE -> java.util.Locale.SIMPLIFIED_CHINESE
            AppLanguage.SPANISH -> java.util.Locale("es", "ES")
            else -> java.util.Locale.US
        }
        val ttsSuccess = ttsManager?.setLanguage(locale) ?: false
        
        if (!ttsSuccess && language == AppLanguage.CHINESE) {
             // If we failed to set Chinese (likely missing data), warn the user
             //val isEnglishUI = _appLanguage.value == AppLanguage.ENGLISH // Actually we just set it to Chinese above, so this will be false
             val msg = "⚠️ 您的设备似乎没有安装中文语音包 (TTS Data)。\nSpeech functionality may not work for Chinese."
             // Don't revert UI language because user might still want to read text, but warn about Audio
             _errorMessage.value = msg
        }
        
        // Refresh News with new language
        fetchAINewsArticles()
    }

    fun cycleLanguage() {
        val next = when (_appLanguage.value) {
            AppLanguage.ENGLISH -> AppLanguage.SPANISH
            AppLanguage.SPANISH -> AppLanguage.CHINESE
            AppLanguage.CHINESE -> AppLanguage.ENGLISH
        }
        setLanguage(next)
    }

    fun setTopic(topic: AITopic) {
        // If user manually changes topic, stop any auto-sequence
        if (isSequentialReading) {
            stopSequentialListen()
        }
        _selectedTopic.value = topic
        ttsManager?.stop()
        updateDisplayMode(topic)

        // Auto Play Logic
        if (_isAutoPlayNews.value) {
            val newsTopics = listOf(AITopic.WORLD_NEWS, AITopic.FINANCE_NEWS, AITopic.AI_ANALYSIS)
            if (newsTopics.contains(topic)) {
                // Find the menu item for this topic to get title/url
                val item = _topMenuItems.value.find { it.topic == topic }
                if (item != null) {
                    playNewsSummary(listOf(item))
                }
            }
        }
    }
    
    fun setEngine(engine: AIEngine) {
        _selectedEngine.value = engine
        PreferenceManager.selectedEngine = engine.name
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        PreferenceManager.speechRate = rate
        ttsManager?.updateConfig(rate, _speechPitch.value)
    }

    fun setSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        PreferenceManager.speechPitch = pitch
        ttsManager?.updateConfig(_speechRate.value, pitch)
    }

    fun updateSpeechConfig(rate: Float, pitch: Float) {
        _speechRate.value = rate
        _speechPitch.value = pitch
        PreferenceManager.speechRate = rate
        PreferenceManager.speechPitch = pitch
        ttsManager?.updateConfig(rate, pitch)
    }

    fun toggleSpeech() {
        _isSpeechEnabled.value = !_isSpeechEnabled.value
        if (!_isSpeechEnabled.value) {
            ttsManager?.stop()
        }
    }


    fun openLogin() {
        _showLoginDialog.value = true
    }

    fun closeLogin() {
        _showLoginDialog.value = false
    }

    fun openRegister() {
        _showRegisterDialog.value = true
    }

    fun closeRegister() {
        _showRegisterDialog.value = false
    }

    fun openProfile() {
        _showProfileDialog.value = true
    }

    fun closeProfile() {
        _showProfileDialog.value = false
    }

    fun performLogin(email: String, pass: String) {
        val manager = userManager
        if (manager == null) {
            showError("Firebase not initialized")
            return
        }
        viewModelScope.launch {
             val result = manager.login(email, pass)
             if (result.isSuccess) {
                 _showLoginDialog.value = false
                 refreshHotToolList() // Refresh tools for the new user
             } else {
                 val error = result.exceptionOrNull()
                 error?.printStackTrace()
                 val msg = error?.message ?: "Login Failed"
                 
                 if (msg.contains("CONFIGURATION_NOT_FOUND")) {
                     showError("Config Error: Google Services missing. Please check your installation.")
                 } else {
                     showError(msg)
                 }
             }
        }
    }
    fun performPasswordReset(email: String) {
        val manager = userManager
        if (manager == null) {
            showError("Firebase not initialized")
            return
        }
        viewModelScope.launch {
            val result = manager.resetPassword(email)
            if (result.isSuccess) {
                showError("Reset link sent to $email")
            } else {
                showError(result.exceptionOrNull()?.message ?: "Reset Failed")
            }
        }
    }
    
    fun performRegister(email: String, pass: String, fullName: String, username: String, phone: String) {
        val manager = userManager
        if (manager == null) {
            showError("Firebase not initialized")
            return
        }
        viewModelScope.launch {
            val result = manager.register(email, pass, fullName, username, phone)
            if (result.isSuccess) {
                _showRegisterDialog.value = false
                showError("Success. Please check email to verify.")
            } else {
                showError(result.exceptionOrNull()?.message ?: "Registration Failed")
            }
        }
    }

    fun logout() {
        userManager?.logout()
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = userManager?.deleteAccount()
            if (result?.isSuccess == true) {
                // Logout/Clear UI state is handled by authState listener in UserManager
            } else {
                showError(result?.exceptionOrNull()?.message ?: "Delete account failed")
            }
        }
    }

    private val topMenuManager by lazy { com.quantumproperty.qcai.data.TopMenuManager() }

    fun fetchTopMenu() {
        viewModelScope.launch {
            try {
                 val items = topMenuManager.fetchTopMenu()
                 if (items.isNotEmpty()) {
                     _topMenuItems.value = items
                     // Refresh current topic display
                     updateDisplayMode(_selectedTopic.value)
                 }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateDisplayMode(topic: AITopic) {
        val language = _appLanguage.value
        val isChinese = language == AppLanguage.CHINESE
        
        // Match by topic directly
        val dynamicItem = _topMenuItems.value.find { it.topic == topic }

        if (dynamicItem != null && topic != AITopic.CLT_VIBE) {
            _displayMode.value = DisplayMode.WEB
            _currentWebUrl.value = if (isChinese) dynamicItem.chineseUrl else dynamicItem.englishUrl
        } else {
            // Fallback to hardcoded URLs if not found in dynamic menu
            when (topic) {
                AITopic.DIY -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://qcai-net.github.io/homediy/"
                }
                AITopic.FOOD -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://qcai-net.github.io/cfood/index_cn.html"
                }
                AITopic.AI_ANALYSIS -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = if (isChinese) "https://qcai-net.github.io/ainews/index_CN.html" else "https://qcai-net.github.io/ainews.html"
                }
                AITopic.WORLD_NEWS -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = if (isChinese) "https://quantumpropertyllc.github.io/news/topnews_cn.html" else "https://quantumpropertyllc.github.io/news/topnews.html"
                }
                AITopic.FINANCE_NEWS -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = if (isChinese) "https://quantumpropertyllc.github.io/news/money_cn.html" else "https://quantumpropertyllc.github.io/news/money.html"
                }
                AITopic.MISC -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://quantumpropertyllc.github.io/news/misc_cn.html"
                }
                AITopic.REAL_ESTATE -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://qcai-net.github.io/airealestate/"
                }
                AITopic.LIFE -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://qcai-net.github.io/homeowner/life.html"
                }
                AITopic.CLT_VIBE, AITopic.STOCK, AITopic.COLLEGE, AITopic.NONE, AITopic.BUSINESS -> {
                    _displayMode.value = DisplayMode.CHAT
                }
            }
        }
        
        // Reset AI real estate tools when changing topic
        _showAIRealEstateTools.value = false
        _showAddressInput.value = false
    }


    fun showAIRealEstateTools() {
        // Always allow showing tools regardless of current topic
        _showAIRealEstateTools.value = true
    }

    fun toggleRecording(target: String? = null) {
        ttsManager?.stop()
        if (isRecording.value) {
            stopRecordingAndSend()
        } else {
             // Check keys BEFORE starting recording
             if (!checkKeys()) return
             
             _recordingTarget.value = target

             if (target != "OpenClawChat") {
                _displayMode.value = DisplayMode.CHAT
             }
             
             // Language selection based on current app language
             val languageTag = when (_appLanguage.value) {
                 AppLanguage.CHINESE -> "zh-CN"
                 AppLanguage.SPANISH -> "es-ES"
                 else -> "en-US"
             }

             speechManager?.startRecording(languageTag) { error ->
                 handleError(error)
             }
        }
    }

    fun stopRecordingAndSend() {
        if (isRecording.value) {
            speechManager?.stopRecording()
            
            val target = _recordingTarget.value
            _recordingTarget.value = null
            
            // Send what we have if it's NOT a UI-only target
            if (target != "OpenClawChat") {
                val text = transcript.value
                if (text.isNotEmpty()) {
                    sendMessage(text)
                }
            }
        }
    }

    fun sendMessage(text: String, image: Bitmap? = null, customPrompt: String? = null, explicitTopic: AITopic? = null) {
        // No local key check needed - all queries route through backend which has server-side keys
        
        val activeTopic = explicitTopic ?: _selectedTopic.value

        if (activeTopic != AITopic.CLT_VIBE && activeTopic != AITopic.STOCK) {
             _displayMode.value = DisplayMode.CHAT
        }
        val newUserMsg = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + newUserMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                var responseText: String
                var extraData: Map<String, Any>? = null

                // Route ALL queries through backend (backend has server-side Gemini keys)
                try {
                    val chatResponse = com.quantumproperty.qcai.data.CityOSService.instance.queryChat(
                        question = text,
                        engine = _selectedEngine.value.name,
                        topic = activeTopic.id,
                        userAddress = com.quantumproperty.qcai.data.PreferenceManager.homeAddress,
                        language = if (_appLanguage.value == AppLanguage.CHINESE) "zh" else "en",
                        customPrompt = customPrompt  // Pass custom prompt if provided
                    )
                    responseText = chatResponse.answer
                    extraData = chatResponse.extraData
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback to local AIService ONLY if local keys are available
                    if (PreferenceManager.geminiKey.isNotEmpty() || PreferenceManager.openAIKey.isNotEmpty()) {
                        responseText = aiService.sendMessage(
                            text = text,
                            engine = _selectedEngine.value,
                            topic = activeTopic,
                            language = _appLanguage.value,
                            image = image
                        )
                    } else {
                        responseText = if (_appLanguage.value == AppLanguage.CHINESE) 
                            "⚠️ 服务器暂时无法响应，请稍后重试。" 
                        else 
                            "⚠️ Server is temporarily unavailable. Please try again later."
                    }
                }

                // Clean up the text for display (remove MATCH_SCORE_JSON if present)
                var displayText = responseText
                if (responseText.contains("MATCH_SCORE_JSON")) {
                    val startIdx = responseText.indexOf("MATCH_SCORE_JSON")
                    displayText = responseText.substring(0, startIdx).trim()
                }

                val aiMsg = ChatMessage(text = displayText, isUser = false, extraData = extraData)
                _messages.value = _messages.value + aiMsg
                
                if (activeTopic == AITopic.CLT_VIBE) {
                    parseRecommendations(responseText)
                }
                
                if (isSpeechEnabled.value) {
                    ttsManager?.speak(responseText)
                }
            } catch (e: Exception) {
                handleError(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private var isSequentialReading = false
    private val sequentialTopics = listOf(AITopic.WORLD_NEWS, AITopic.FINANCE_NEWS, AITopic.AI_ANALYSIS)
    private var currentSequenceIndex = 0

    fun startSequentialListen() {
        // Check if TTS is ready
        if (ttsManager?.isReady() != true) {
            val isChinese = _appLanguage.value == AppLanguage.CHINESE
            val message = if (isChinese) {
                "语音播报服务不可用\n\n请在真机上测试此功能，或确保已安装 Google TTS。"
            } else {
                "Text-to-Speech service is unavailable\n\nPlease test this feature on a real device, or ensure Google TTS is installed."
            }
            showError(message)
            return
        }

        // Give immediate feedback
        val loadingMsg = if (_appLanguage.value == AppLanguage.CHINESE) "正在获取最新新闻..." else "Checking for latest updates..."
        ttsManager?.speak(loadingMsg, null, android.speech.tts.TextToSpeech.QUEUE_FLUSH)
        
        isSequentialReading = true
        currentSequenceIndex = 0
        
        // Listen for specific "DONE_TOPIC" event only
        ttsManager?.onSpeechCompleted = { id ->
            if (isSequentialReading) {
                 // Check if the completed utterance is the one marking the end of the current topic
                 if (id == "SEQ_DONE_$currentSequenceIndex") {
                    viewModelScope.launch {
                        currentSequenceIndex++
                        if (currentSequenceIndex < sequentialTopics.size) {
                             readCurrentSequenceStep()
                        } else {
                            isSequentialReading = false
                        }
                    }
                 }
            }
        }
        
        readCurrentSequenceStep()
    }
    
    fun stopSequentialListen() {
         isSequentialReading = false
         _isNewsSummaryReading.value = false
         ttsManager?.stop()
    }
    
    private fun readCurrentSequenceStep() {
        if (currentSequenceIndex >= sequentialTopics.size) return
        
        val topic = sequentialTopics[currentSequenceIndex]
        
        // Update UI logic similar to setTopic, but don't call setTopic directly to avoid stopping TTS prematurely if we were to change logic later.
        // Actually setTopic stops TTS, which is correct because we are starting a NEW speech segment.
        _selectedTopic.value = topic
        updateDisplayMode(topic) // load the URL
        
        
        val url = _currentWebUrl.value
        if (url == null) {
            showError("No URL found for topic: $topic")
            stopSequentialListen()
            return
        }

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Add User-Agent to avoid being blocked
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                    .timeout(10000)
                    .get()
                
                
                // Smart parsing:
                // Select generic content blocks to separate them
                val elements = doc.select("h1, h2, h3, li, p")
                val textParts = if (elements.isNotEmpty()) {
                    elements.eachText().filter { it.isNotBlank() }
                } else {
                    // Fallback if no structure found
                    listOf(doc.body().text())
                }

                // Add intro text (Flush previous)
                val isChinese = _appLanguage.value == AppLanguage.CHINESE
                val intro = if (isChinese) {
                    when(topic) {
                       AITopic.WORLD_NEWS -> "现在为您播报：世界头条"
                       AITopic.FINANCE_NEWS -> "接下来是：财经头条"
                       AITopic.AI_ANALYSIS -> "最后为您播报：AI深度分析"
                       else -> ""
                    }
                } else {
                    when(topic) {
                       AITopic.WORLD_NEWS -> "Now reading: Headline News"
                       AITopic.FINANCE_NEWS -> "Next up: Finance News"
                       AITopic.AI_ANALYSIS -> "Finally: AI Deep Analysis"
                       else -> ""
                    }
                }
                
                // 1. Speak Intro (Add) - We used QUEUE_FLUSH for loading message, so here we ADD
                ttsManager?.speak(intro, null, android.speech.tts.TextToSpeech.QUEUE_ADD)
                ttsManager?.playSilence(600, android.speech.tts.TextToSpeech.QUEUE_ADD, null)
                
                // 2. Speak Items (Add)
                if (textParts.isEmpty()) {
                     // Just finish if empty
                     // Trigger a silent utterance with ID to signal completion
                     ttsManager?.playSilence(100, android.speech.tts.TextToSpeech.QUEUE_ADD, "SEQ_DONE_$currentSequenceIndex")
                } else {
                    textParts.forEachIndexed { index, part ->
                        val isLast = index == textParts.lastIndex
                        // Only the very last item gets the ID that triggers the next topic
                        val id = if (isLast) "SEQ_DONE_$currentSequenceIndex" else null
                        
                        ttsManager?.speak(part, id, android.speech.tts.TextToSpeech.QUEUE_ADD)
                        
                        if (!isLast) {
                            // Pause between news items
                            ttsManager?.playSilence(800, android.speech.tts.TextToSpeech.QUEUE_ADD, null) 
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // If error, try skip to next? 
                launch {
                    val errorMsg = "Error loading news: ${e.message}"
                    // Always show error if it's the first one, or log it
                    if (currentSequenceIndex == 0) showError(errorMsg)

                    // Short delay then next
                    kotlinx.coroutines.delay(1000)
                    currentSequenceIndex++
                    if (currentSequenceIndex < sequentialTopics.size) {
                        readCurrentSequenceStep()
                    } else {
                        isSequentialReading = false
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun helpWithDIY(image: Bitmap?) {
        if (image == null) return
        ttsManager?.stop()
        setTopic(AITopic.DIY)
        val msg = if (_appLanguage.value == AppLanguage.CHINESE) "请解析这张图片中的问题" else "Please analyze the problem in this image"
        sendMessage(msg, image)
    }

    fun analyzeRealEstate(address: String) {
        if (address.isBlank()) return
        
        _isLoading.value = true
        _displayMode.value = DisplayMode.CHAT
        _showAIRealEstateTools.value = false // dismiss tools card
        
        viewModelScope.launch {
            // Fetch official GIS data first
            val gisData = com.quantumproperty.qcai.data.PropertyDataService().fetchPropertyData(address)
            
            val prompt = if (_appLanguage.value == AppLanguage.CHINESE) 
                """
                请提供关于地址 "\(address)" 的尽可能详细、最新的信息和数据。以下是我已经为你获取的【官方政府 GIS 数据】（附在最后），请务必优先使用这些准确数据（特别是业主姓名、地块信息、学区等）来回答相关问题。 关于房主信息，如果不在GIS数据里，那么有可能的话，你可以自己做研究获得。

                    请按以下顺序整理并输出具体内容（如果 GIS 数据包含列表之外的有用信息，请继续以 12、13 等序号列出）：
                    1. 最新的房屋估值或市场价值
                    2. 房屋总面积（平方英尺/平方米）
                    3. 卧室数量和浴室数量
                    4. 建筑年份
                    5. 最近一次成交价格和成交日期
                    6. 当前的房产税率或估算税额
                    7. 所属学区及学校评分，周边环境描述，以及安全/治安指数
                    8. 房产业主的公开信息（请直接引用 GIS 数据中的 Owner）
                    9. 最近三年内该地址或周边（500米/1英里）范围内的犯罪记录和统计数据
                    10. 周边居民的人口统计信息（年龄、收入水平、教育水平）
                    11. 最近几年在这个房屋里居住过的人员名单和联系方式

                    **重要指令：**
                    - 请将 GIS 数据中的信息有机地整合到你的分析中，不要忽略它们。
                    - 凡是 GIS 数据中提供但未包含在上述11点中的信息（如 Zoning, Land Use 等），请务必作为第 12、13 点等继续列出。
                    - 直接提供数据结果，不要说空话。
                
                
                === OFFICIAL GIS DATA START ===
                $gisData
                === OFFICIAL GIS DATA END ===
                """
            else 
                """
                Please provide the most detailed and up-to-date information and data for the address "$address". I have attached the 【Official Government GIS Data】 at the end of this message. You MUST prioritize this accurate data (especially Owner Name, Parcel ID, School Zone, etc.) when answering. If the property owner is not included in the GIS data and if possible, you may do a research by yourself and get these information.

                Please provide the most detailed and up-to-date information and data for the address "\(address)". I have attached the 【Official Government GIS Data】 at the end of this message. You MUST prioritize this accurate data (especially Owner Name, Parcel ID, School Zone, etc.) when answering. If hte property owner is not included in the GIS data and if possible, you may do a researh by yourself and get these information.

                    Please organize and output the specific content in the following order (if GIS data contains useful information beyond this list, please continue listing them as 12, 13, etc.):
                    1. Latest property valuation or market value
                    2. Total living area (sq ft / sq m)
                    3. Number of bedrooms and bathrooms
                    4. Year built
                    5. Most recent sale price and date
                    6. Current property tax rate or estimated tax amount
                    7. Assigned school district and school ratings, neighborhood description, and safety/security index
                    8. Publicly available property owner information (Directly cite the 'Owner' from the GIS data)
                    9. Crime records and statistics within the last 3 years for this address or surrounding area (500m/1 mile)
                    10. Demographics of surrounding residents (age, income level, education level)
                    11. List of residents who have lived in this house in recent years

                    **IMPORTANT INSTRUCTIONS:**
                    - Integrate the GIS data organically into your analysis; do not ignore it.
                    - Any information provided in the GIS data that is not covered in the above 11 points (like Zoning, Land Use) MUST be listed as items 12, 13, etc.
                    - Directly provide data results; do not use vague conversational fillers.
                
                
                === OFFICIAL GIS DATA START ===
                $gisData
                === OFFICIAL GIS DATA END ===
                """
            
            
            _isLoading.value = false
            // Pass the detailed prompt as customPrompt, and the address as the question
            sendMessage(text = address, customPrompt = prompt)
        }
    }
    
    // Helper to send message without re-triggering checks if we are already in the flow
    private fun sendMessageInternal(text: String, isHidden: Boolean, topicOverride: AITopic? = null, realEstateAddress: String? = null) {
         _displayMode.value = DisplayMode.CHAT
        val newUserMsg = ChatMessage(text = text, isUser = true, isHidden = isHidden)
        _messages.value = _messages.value + newUserMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Use override topic if provided, otherwise use selected
                val effectiveTopic = topicOverride ?: _selectedTopic.value
                
                val response = aiService.sendMessage(
                    text = text,
                    engine = _selectedEngine.value,
                    topic = effectiveTopic,
                    language = _appLanguage.value,
                    image = null,
                    realEstateAddress = realEstateAddress
                )
                val aiMsg = ChatMessage(text = response, isUser = false)
                _messages.value = _messages.value + aiMsg

                if (effectiveTopic == AITopic.CLT_VIBE) {
                    parseRecommendations(response)
                }

                ttsManager?.speak(response)
            } catch (e: Exception) {
                handleError(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startAIChat() {
        _displayMode.value = DisplayMode.CHAT
        _showAIRealEstateTools.value = false
    }

    fun resetConversation() {
        _messages.value = emptyList()
        ttsManager?.stop()
        _displayMode.value = DisplayMode.CHAT
        
        val welcomeText = if (_appLanguage.value == AppLanguage.CHINESE) {
            "我是 Queen City AI。我主要使用 ChatGPT 和 Gemini 作为 AI 后台，结合夏洛特本地的实际环境，为大家提供面向夏洛特华人的本地信息服务。我特别被设计为一个自己动手智能助手，重点帮助华人用户获取生活服务指南、办事流程说明，以及华人饮食与餐饮信息的分享与推荐，让在夏洛特的生活变得更加方便、高效、安心。"
        } else {
            "I am Queen City AI. I primarily use ChatGPT and Gemini as the AI engine, combined with the local environment of Charlotte, to provide local information services for the Charlotte community. I am specifically designed as a DIY smart assistant, focusing on helping users get life service guides, procedure explanations, and sharing and recommending Chinese food and catering information, making life in Charlotte more convenient, efficient, and secure."
        }
        ttsManager?.speak(welcomeText)
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissAddressInput() {
        _showAddressInput.value = false
        _showAIRealEstateTools.value = false
    }

    fun dismissAPIKeySetup() {
        _showAPIKeySetup.value = false
        _apiKeySetupReason.value = null
    }

    private fun checkKeys(): Boolean {
        if (PreferenceManager.openAIKey.isEmpty() && PreferenceManager.geminiKey.isEmpty()) {
            _apiKeySetupReason.value = if (_appLanguage.value == AppLanguage.CHINESE) "请先设置 API Key" else "Please set API Key first"
            _showAPIKeySetup.value = true
            return false
        }
        return true
    }

    fun openAPIKeySetup() {
        _showAPIKeySetup.value = true
        _apiKeySetupReason.value = null
    }

    private val hotListManager by lazy { com.quantumproperty.qcai.data.HotListManager() }

    fun refreshHotToolList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = hotListManager.fetchHotList()
                _hotListItems.value = items
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Community Features Navigation
    fun showEventsView() {
        _showEventsView.value = true
    }
    
    fun closeEventsView() {
        _showEventsView.value = false
    }
    
    fun showMarketplaceView() {
        _showMarketplaceView.value = true
    }
    
    fun closeMarketplaceView() {
        _showMarketplaceView.value = false
    }
    
    fun showRentalsView() {
        _showRentalsView.value = true
    }
    
    fun closeRentalsView() {
        _showRentalsView.value = false
    }

    fun openCollegeAdmissions() {
        _showCollegeAdmissions.value = true
    }

    fun closeCollegeAdmissions() {
        _showCollegeAdmissions.value = false
    }

    fun showTheSceneView(vibe: String) {
        _showTheSceneView.value = true
        fetchScene(vibe)
    }

    fun closeTheSceneView() {
        _showTheSceneView.value = false
        _sceneData.value = null
    }

    fun fetchScene(vibe: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = com.quantumproperty.qcai.data.CityOSService.instance.fetchScene(vibe)
                _sceneData.value = data
            } catch (e: Exception) {
                showError(e.message ?: "Failed to fetch scene")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleError(error: String?) {
        val errorMsg = (error ?: "Unknown error").lowercase()
        if (errorMsg.contains("401") || 
            errorMsg.contains("invalid_api_key") || 
            errorMsg.contains("quota") || 
            (errorMsg.contains("insufficient") && !errorMsg.contains("microphone")) || // Exclude mic permission
            (errorMsg.contains("permission") && !errorMsg.contains("microphone")) ||
            errorMsg.contains("missing")
        ) {
             _apiKeySetupReason.value = if (_appLanguage.value == AppLanguage.CHINESE) 
                 "API Key 无效、缺失或已过期，请重新设置" 
             else 
                 "API Key is invalid, missing, or expired. Please reset it."
             _showAPIKeySetup.value = true
        } else {
            _errorMessage.value = error // Show original error message
        }
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    // OpenClaw Actions
    fun connectOpenClaw(host: String? = null, port: Int? = null, token: String? = null) {
        openClawService.connect(host, port, token)
    }

    fun disconnectOpenClaw() {
        openClawService.disconnect()
    }

    fun dismissOpenClawError() {
        _openClawError.value = null
    }



    private var tailscalePoller: Job? = null

    private fun startTailscaleMonitoring() {
        tailscalePoller?.cancel()
        tailscalePoller = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                checkTailscaleStatus()
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun checkTailscaleStatus() {
        try {
            if (com.quantumproperty.qcai.native.TailscaleBridge.isReady()) {
                val ip = com.quantumproperty.qcai.native.TailscaleBridge.getIPAddress()
                viewModelScope.launch(Dispatchers.Main) {
                    if (ip.startsWith("100.")) {
                        _tunnelIP.value = ip
                        _isTunnelConnected.value = true
                    } else {
                        // Only disconnect if we aren't actively trying to connect
                        if (!_isConnecting.value) {
                            _isTunnelConnected.value = false
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Internal Bridge (tsnet) - PRODUCTION INTEGRATION
    fun connectTunnel() {
        if (!com.quantumproperty.qcai.native.TailscaleBridge.isReady()) {
            _errorMessage.value = "Production Build Error: Native Tailscale library (.so) not found in jniLibs."
            return
        }

        val authKey = gatewayAuthKey.value
        val hostname = gatewayHostname.value.takeIf { it.isNotBlank() } ?: "qcai-android"
        val stateDir = java.io.File(getApplication<Application>().filesDir, "tailscale").absolutePath

        _isConnecting.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ip = com.quantumproperty.qcai.native.TailscaleBridge.start(authKey, hostname, stateDir)
                withContext(Dispatchers.Main) {
                    _isConnecting.value = false
                    if (ip.startsWith("100.")) {
                        _isTunnelConnected.value = true
                        _tunnelIP.value = ip
                    } else {
                        _errorMessage.value = "Tailscale Connection Failed: Verify Auth Key & Permissions"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isConnecting.value = false
                    _errorMessage.value = "Native Error: ${e.message}"
                }
            }
        }
    }

    fun disconnectTunnel() {
        if (com.quantumproperty.qcai.native.TailscaleBridge.isReady()) {
            com.quantumproperty.qcai.native.TailscaleBridge.stop()
        }
        _isTunnelConnected.value = false
        _tunnelIP.value = "100.x.x.x"
    }

    fun linkGateway() {
        val host = gatewayHostname.value.trim()
        val port = gatewayPort.value.toIntOrNull() ?: 443
        val token = gatewayToken.value.trim()
        
        if (host.isEmpty()) {
            _errorMessage.value = "Please enter a gateway address or paste a setup code."
            return
        }
        
        openClawService.connect(host, port, token)
    }

    fun unlinkGateway() {
        openClawService.disconnect()
        _isGatewayLinked.value = false
    }

    fun pasteSetupCode() {
        val clipboard = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val code = clip.getItemAt(0).text?.toString() ?: ""
            parseAndApplySetupCode(code)
        }
    }

    fun toggleQRScanner(show: Boolean) {
        _showQRScanner.value = show
    }

    fun toggleJoinGatewayDialog(show: Boolean) {
        _showJoinGatewayDialog.value = show
    }

    fun toggleSetupGuide(show: Boolean) {
        _showSetupGuide.value = show
    }

    fun toggleConfigPreview(show: Boolean) {
        _showConfigPreview.value = show
    }

    // --- Gateway Chat Helpers ---

    fun openGatewayChat() {
        _showGatewayChat.value = true
        if (_gatewayMessages.value.isEmpty()) {
            loadChatHistory()
        }
    }

    private fun loadChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val responseJson = openClawService.callRpc("chat.history", mapOf("sessionKey" to "agent:main:main"))
                val history = com.google.gson.Gson().fromJson(responseJson, OpenClawService.ChatHistoryResponse::class.java)
                history.messages?.let { msgs ->
                    val mapped = msgs.map { 
                        ChatMessage(
                            text = it.plainText,
                            isUser = it.role == "user"
                        )
                    }
                    withContext(Dispatchers.Main) {
                        _gatewayMessages.value = mapped
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TeacherViewModel", "Failed to load chat history: ${e.message}")
            }
        }
    }

    fun closeGatewayChat() {
        _showGatewayChat.value = false
    }

    fun sendGatewayChat(text: String, images: List<String>? = null) {
        if (text.isBlank()) return
        if (isRecording.value) {
            stopRecordingAndSend()
        }
        
        // Add User Message
        val userMsg = ChatMessage(text = text, isUser = true)
        _gatewayMessages.value = _gatewayMessages.value + userMsg
        
        // Send via Service
        openClawService.sendGatewayMessage(text, images)
    }

    /**
     * Resizes and encodes a Bitmap to Base64 JPEG for OpenClaw Gateway.
     */
    fun processImageForOpenClaw(bitmap: android.graphics.Bitmap): String? {
        return try {
            val maxDim = 1600
            var width = bitmap.width
            var height = bitmap.height
            
            if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                if (width > height) {
                    width = maxDim
                    height = (maxDim / ratio).toInt()
                } else {
                    height = maxDim
                    width = (maxDim * ratio).toInt()
                }
            }
            
            val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
            val outputStream = java.io.ByteArrayOutputStream()
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearGatewayChat() {
        _gatewayMessages.value = emptyList()
        _gatewayStreamingText.value = null
    }

    private fun handleChatEvent(event: OpenClawService.ChatEvent) {
        // Only process events for the main agent session to avoid crosstalk
        if (event.sessionKey != "agent:main:main") return
        
        // Log event for debugging
        android.util.Log.d("TeacherViewModel", "Received ChatEvent: seq=${event.seq}, state=${event.state}")

        if (event.state == "streaming") {
            val streamText = event.message?.plainText
            if (streamText != "HEARTBEAT_OK") {
                _gatewayStreamingText.value = streamText
            }
        } else if (event.state == "final") {
            val finalContent = event.message?.plainText
            if (!finalContent.isNullOrBlank() && finalContent.trim() != "HEARTBEAT_OK") {
                val aiMsg = ChatMessage(text = finalContent, isUser = false)
                _gatewayMessages.value = _gatewayMessages.value + aiMsg
            }
            _gatewayStreamingText.value = null
        } else if (event.state == "error") {
             val errorMsg = event.errorMessage ?: "Unknown Gateway Error"
             val aiMsg = ChatMessage(text = "Error: $errorMsg", isUser = false)
             _gatewayMessages.value = _gatewayMessages.value + aiMsg
             _gatewayStreamingText.value = null
        }
    }

    val openClawConfigJson = """
{
  "meta": {
    "lastTouchedVersion": "2026.3.13",
    "lastTouchedAt": "2026-03-15T02:39:40.890Z"
  },
  "models": {
    "providers": {
      "openai": {
        "baseUrl": "http://127.0.0.1:1234/v1",
        "auth": "api-key",
        "api": "openai-responses",
        "models": [
          {
            "id": "llm",
            "name": "Local Qwen 3.5",
            "contextWindow": 131072
          }
        ]
      }
    }
  },
  "agents": {
    "defaults": {
      "model": "openai/llm",
      "compaction": {
        "mode": "safeguard"
      }
    }
  },
  "commands": {
    "native": "auto",
    "nativeSkills": "auto",
    "restart": true,
    "ownerDisplay": "raw"
  },
  "channels": {
    "telegram": {
      "enabled": true,
      "dmPolicy": "pairing",
      "botToken": "1234567890:ABCDEFGHIJKLMN_OPQRSTUVWXYZ",
      "groups": {
        "*": {
          "requireMention": true
        }
      },
      "groupPolicy": "open",
      "streaming": "partial"
    }
  },
  "gateway": {
    "mode": "local",
    "bind": "loopback",
    "controlUi": {
      "allowedOrigins": [
        "*"
      ],
      "dangerouslyAllowHostHeaderOriginFallback": true
    },
    "auth": {
      "mode": "token",
      "token": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6"
    },
    "http": {
      "endpoints": {
        "chatCompletions": {
          "enabled": true
        }
      }
    },
    "trustedProxies": [
      "127.0.0.1",
      "::1",
      "100.0.0.0/8"
    ]
  },
  "plugins": {
    "entries": {
      "device-pair": {
        "enabled": true
      }
    }
  }
}
    """.trimIndent()

    fun applySetupCode(code: String) {
        val cleanCode = code.trim()
        if (cleanCode.isNotEmpty()) {
            parseAndApplySetupCode(cleanCode)
        }
    }

    private fun parseAndApplySetupCode(code: String) {
        val trimmed = code.trim()
        if (trimmed.length < 50) {
            // Already a plain token? 
            gatewayToken.value = trimmed
            return
        }

        try {
            // Replicate iOS logic: padding if needed
            val padded = when (trimmed.length % 4) {
                2 -> "$trimmed=="
                3 -> "$trimmed="
                else -> trimmed
            }
            val data = android.util.Base64.decode(padded, android.util.Base64.DEFAULT)
            val json = org.json.JSONObject(String(data))
            
            val urlString = json.optString("url")
            if (urlString.isNotEmpty()) {
                val uri = android.net.Uri.parse(urlString)
                gatewayHostname.value = uri.host ?: ""
                val port = uri.port
                if (port != -1) {
                    gatewayPort.value = port.toString()
                } else {
                    val scheme = uri.scheme?.lowercase() ?: "ws"
                    val host = uri.host ?: ""
                    gatewayPort.value = if (scheme == "wss" || scheme == "https" || host.contains(".ts.net")) "443" else "443"
                }
            }
            
            val token = json.optString("bootstrapToken", json.optString("token"))
            if (token.isNotEmpty()) {
                gatewayToken.value = token
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: just treat as token
            gatewayToken.value = trimmed
        }
    }

    fun openWebConsole() {
        val host = gatewayHostname.value.trim()
        val port = gatewayPort.value.toIntOrNull() ?: 443
        val token = gatewayToken.value.trim()
        val isSecure = port == 443
        val scheme = if (isSecure) "https" else "http"
        val portPart = if (port == 443 || port == 80) "" else ":$port"
        val url = "$scheme://$host$portPart/?token=$token"
        BrowserUtils.openURL(getApplication(), url)
    }
    
    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager?.shutdown()
    }
}
