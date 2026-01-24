package com.example.cltdiy.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cltdiy.data.AIEngine
import com.example.cltdiy.data.AIService
import com.example.cltdiy.data.AITopic
import com.example.cltdiy.data.AppLanguage
import com.example.cltdiy.data.ChatMessage
import com.example.cltdiy.data.HotToolItem
import com.example.cltdiy.data.TopMenuItem
import com.example.cltdiy.data.PreferenceManager
import kotlinx.coroutines.Dispatchers
import com.example.cltdiy.utils.SpeechManager
import com.example.cltdiy.utils.TTSManager
import com.example.cltdiy.data.UserManager
import com.example.cltdiy.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class DisplayMode {
    WEB, CHAT, SETTINGS
}

class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    private val aiService by lazy { AIService() }
    private var ttsManager: TTSManager? = null
    private var speechManager: SpeechManager? = null
    private var userManager: UserManager? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.CHINESE)
    val appLanguage = _appLanguage.asStateFlow()

    private val _selectedEngine = MutableStateFlow(AIEngine.GEMINI)
    val selectedEngine = _selectedEngine.asStateFlow()

    private val _topMenuItems = MutableStateFlow<List<TopMenuItem>>(emptyList())
    val topMenuItems = _topMenuItems.asStateFlow()

    private val _selectedTopic = MutableStateFlow(AITopic.WORLD_NEWS)
    val selectedTopic = _selectedTopic.asStateFlow()

    private val _displayMode = MutableStateFlow(DisplayMode.WEB)
    val displayMode = _displayMode.asStateFlow()

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

    private val _showRegisterDialog = MutableStateFlow(false)
    val showRegisterDialog = _showRegisterDialog.asStateFlow()

    private val _apiKeySetupReason = MutableStateFlow<String?>(null)
    val apiKeySetupReason = _apiKeySetupReason.asStateFlow()

    private val _hotListItems = MutableStateFlow<List<HotToolItem>>(emptyList())
    // Allow observing raw items if needed, but UI should likely use userSpecificTools
    val hotListItems = _hotListItems.asStateFlow()

    // Filtered list based on VIP level
    val userSpecificTools = combine(_hotListItems, _userProfile) { items, profile ->
        if (profile != null && profile.vipLevel >= 1) {
            items
        } else {
            emptyList()
        }
    }

    private val _showHotToolWebView = MutableStateFlow(false)
    val showHotToolWebView = _showHotToolWebView.asStateFlow()

    private val _hotToolWebUrl = MutableStateFlow<String?>(null)
    val hotToolWebUrl = _hotToolWebUrl.asStateFlow()

    private val _showAIRealEstateTools = MutableStateFlow(false)
    val showAIRealEstateTools = _showAIRealEstateTools.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch = _speechPitch.asStateFlow()

    val isRecording: StateFlow<Boolean> 
        get() = speechManager?.isRecording ?: MutableStateFlow(false).asStateFlow()
    val transcript: StateFlow<String> 
        get() = speechManager?.transcript ?: MutableStateFlow("").asStateFlow()

    init {
        PreferenceManager.init(application)
        
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
        
        // Initial URL setup
        // Initial URL setup
        updateDisplayMode(AITopic.WORLD_NEWS)
        fetchHotList()
        
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
           ttsManager = TTSManager(application)
        } catch (e: Exception) { e.printStackTrace() }

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
                } else {
                    val isChinese = _appLanguage.value == AppLanguage.CHINESE
                    _userName.value = if (isChinese) "访客" else "Guest"
                }
            }
        }
        
        viewModelScope.launch {
            transcript.collect { text ->
                if (text.isNotEmpty() && isRecording.value == false) {
                     // Check if we just finished recording? 
                }
            }
        }
        

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
    }

    fun setTopic(topic: AITopic) {
        _selectedTopic.value = topic
        ttsManager?.stop()
        updateDisplayMode(topic)
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

    fun openSettings() {
        _displayMode.value = DisplayMode.SETTINGS
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
             } else {
                 showError(result.exceptionOrNull()?.message ?: "Login Failed")
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

    private val topMenuManager by lazy { com.example.cltdiy.data.TopMenuManager() }

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
        val isEnglish = _appLanguage.value == AppLanguage.ENGLISH
        
        // Match by topic directly
        val dynamicItem = _topMenuItems.value.find { it.topic == topic }

        if (dynamicItem != null) {
            _displayMode.value = DisplayMode.WEB
            _currentWebUrl.value = if (isEnglish) dynamicItem.englishUrl else dynamicItem.chineseUrl
        } else {
            // Fallback to hardcoded URLs if not found in dynamic menu
            when (topic) {
                AITopic.DIY -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://quantumpropertyllc.github.io/homeowner/"
                }
                AITopic.FOOD -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://quantumpropertyllc.github.io/homeowner/chineserestaurant.html"
                }
                AITopic.AI_ANALYSIS -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = if (isEnglish) "https://quantumpropertyllc.github.io/news/ainews.html" else "https://quantumpropertyllc.github.io/news/ainews_cn.html"
                }
                AITopic.WORLD_NEWS -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = if (isEnglish) "https://quantumpropertyllc.github.io/news/topnews.html" else "https://quantumpropertyllc.github.io/news/topnews_cn.html"
                }
                AITopic.FINANCE_NEWS -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = if (isEnglish) "https://quantumpropertyllc.github.io/news/money.html" else "https://quantumpropertyllc.github.io/news/money_cn.html"
                }
                AITopic.MISC -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://quantumpropertyllc.github.io/news/misc_cn.html"
                }
                AITopic.REAL_ESTATE -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://quantumpropertyllc.github.io/homeowner/realEstate.html"
                }
                AITopic.LIFE -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://quantumpropertyllc.github.io/homeowner/life.html"
                }

                else -> {
                    _displayMode.value = DisplayMode.WEB
                    _currentWebUrl.value = "https://quantumpropertyllc.github.io/news/topnews.html"
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

    fun toggleRecording() {
        ttsManager?.stop()
        if (isRecording.value) {
            stopRecordingAndSend()
        } else {
             // Check keys BEFORE starting recording
             if (!checkKeys()) return
 
             _displayMode.value = DisplayMode.CHAT
             speechManager?.startRecording { error ->
                 handleError(error)
             }
        }
    }

    fun stopRecordingAndSend() {
        if (isRecording.value) {
            speechManager?.stopRecording()
            // Send what we have
            val text = transcript.value
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }
    }

    fun sendMessage(text: String, image: Bitmap? = null) {
        if (!checkKeys()) return

        _displayMode.value = DisplayMode.CHAT
        val newUserMsg = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + newUserMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = aiService.sendMessage(
                    text = text,
                    engine = _selectedEngine.value,
                    topic = _selectedTopic.value,
                    language = _appLanguage.value,
                    image = image
                )
                val aiMsg = ChatMessage(text = response, isUser = false)
                _messages.value = _messages.value + aiMsg
                ttsManager?.speak(response)
            } catch (e: Exception) {
                handleError(e.message)
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
            val gisData = com.example.cltdiy.data.PropertyDataService().fetchPropertyData(address)
            
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
            // Send message but hide it from user view
            sendMessageInternal(prompt, true, AITopic.REAL_ESTATE, address)
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
            "我是 CyberPanda。我主要使用 ChatGPT 和 Gemini 作为 AI 后台，结合夏洛特本地的实际环境，为大家提供面向夏洛特华人的本地信息服务。我特别被设计为一个自己动手智能助手，重点帮助华人用户获取生活服务指南、办事流程说明，以及华人饮食与餐饮信息的分享与推荐，让在夏洛特的生活变得更加方便、高效、安心。"
        } else {
            "I am CyberPanda. I primarily use ChatGPT and Gemini as the AI engine, combined with the local environment of Charlotte, to provide local information services for the Charlotte community. I am specifically designed as a DIY smart assistant, focusing on helping users get life service guides, procedure explanations, and sharing and recommending Chinese food and catering information, making life in Charlotte more convenient, efficient, and secure."
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

    private val hotListManager by lazy { com.example.cltdiy.data.HotListManager() }

    private fun fetchHotList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = hotListManager.fetchHotList()
                _hotListItems.value = items
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun openHotTool(url: String) {
        _hotToolWebUrl.value = url
        _showHotToolWebView.value = true
    }

    fun closeHotTool() {
        _showHotToolWebView.value = false
        _hotToolWebUrl.value = null
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

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager?.shutdown()
    }
}
