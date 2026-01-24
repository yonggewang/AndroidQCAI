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
        updateDisplayMode(AITopic.WORLD_NEWS)
        fetchHotList()
        fetchTopMenu()
        
        
        
        
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

    private fun fetchTopMenu() {
        viewModelScope.launch(Dispatchers.IO) {
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
        if (_selectedTopic.value == AITopic.REAL_ESTATE) {
            _showAIRealEstateTools.value = true
        }
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
        _displayMode.value = DisplayMode.CHAT
        _showAIRealEstateTools.value = false // dismiss tools card
        
        val prompt = if (_appLanguage.value == AppLanguage.CHINESE) 
            "请分析房产地址 \"$address\" 的价值。请包括以下信息：总面积、卧室和浴室数量、目前的房产税率。请同时提供房产业主、学校信息以及你能找到的其他相关细节。" 
        else 
            "Please analyze the property value of \"$address\". In your analysis, please include the following information: the total square footage, the number of bedrooms and bathrooms, and the current property tax rate. Please also include the owner of the property, the school information, and any other relevant details you can find."
        
        sendMessage(prompt, null)
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
