package com.quantumproperty.qcai.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.quantumproperty.qcai.utils.CryptoUtils
import okhttp3.*
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    HANDSHAKING,
    CONNECTED
}

data class GatewayMetrics(
    val cpuUsage: Double = 0.0,
    val ramUsage: Double = 0.0,
    val diskUsage: Double = 0.0,
    val uptime: String = "N/A"
)

class OpenClawService private constructor() {
    private var client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // For WebSockets
        .build()

    private var webSocket: WebSocket? = null
    private val gson = com.google.gson.GsonBuilder()
        .disableHtmlEscaping()
        .create()
    private val handler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var connectionState = ConnectionState.DISCONNECTED
    var isConnected = false
    var isConnecting = false
    var metrics = GatewayMetrics()
    var gatewayHost: String = "127.0.0.1"
    var gatewayPort: Int = 443
    var gatewayToken: String = ""
    var isUsingRemote: Boolean = false
    var isPairingRequired: Boolean = false
    var lastDeviceIdentity: String = ""

    private val TAG = "OpenClawService"
    private val pendingRequests = ConcurrentHashMap<String, (String?, String?) -> Unit>()

    // Gateway Chat Data Models (Matching iOS v5.1)
    data class ChatEvent(
        val runId: String,
        val sessionKey: String,
        val seq: Int,
        val state: String,
        val message: ChatMessagePayload? = null,
        val errorMessage: String? = null
    )

    data class ChatMessagePayload(
        val role: String? = null,
        @com.google.gson.annotations.SerializedName("content") val content: List<ContentBlock>? = null
    ) {
        data class ContentBlock(
            val type: String? = null,
            val text: String? = null
        )

        val plainText: String
            get() = content?.mapNotNull { it.text }?.joinToString("\n") ?: ""
    }

    data class ChatHistoryResponse(
        val sessionKey: String,
        val messages: List<HistoryMessage>? = null
    )

    data class HistoryMessage(
        val role: String,
        val content: List<ChatMessagePayload.ContentBlock>? = null,
        val timestamp: Long? = null
    ) {
        val plainText: String
            get() = content?.mapNotNull { it.text }?.joinToString("\n") ?: ""
    }

    interface OpenClawListener {
        fun onStateChanged(state: ConnectionState)
        fun onMetricsUpdated(metrics: GatewayMetrics)
        fun onPairingRequired(deviceId: String)
        fun onChatEvent(event: ChatEvent)
        fun onError(message: String)
    }

    private val _latestInsight = MutableStateFlow<String?>(null)
    val latestInsight = _latestInsight.asStateFlow()

    private val listeners = mutableListOf<OpenClawListener>()

    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    fun addListener(listener: OpenClawListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OpenClawListener) {
        listeners.remove(listener)
    }

    fun connect(host: String? = null, port: Int? = null, token: String? = null) {
        if (isConnecting || isConnected) return

        val targetHost = host ?: gatewayHost
        val targetPort = port ?: gatewayPort
        val targetToken = (token ?: gatewayToken).trim()

        // Persist for handshake
        this.gatewayHost = targetHost
        this.gatewayPort = targetPort
        this.gatewayToken = targetToken

        isConnecting = true
        connectionState = ConnectionState.CONNECTING
        isPairingRequired = false
        notifyListenersState()

        // Protocol selection: Use wss (Secure) for port 443 or any Tailscale hostname (.ts.net)
        val isSecure = targetPort == 443 || targetHost.contains(".ts.net")
        val protocol = if (isSecure) "wss" else "ws"
        val url = "$protocol://$targetHost:$targetPort/ws?token=$targetToken"
        Log.d(TAG, "Connecting to Gateway: $url")
        Log.d(TAG, "Origin: http://$targetHost:$targetPort")
        
        // --- TAILSCALE SOCKS5 PROXY CONFIG ---
        val isTailscale = targetHost.contains(".ts.net") || targetHost.startsWith("100.")
        Log.d(TAG, "isSecure=$isSecure, isTailscale=$isTailscale, targetPort=$targetPort")
        
        val builder = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        if (isTailscale) {
            Log.d(TAG, "🔒 Tailscale detected. Routing through SOCKS5 Proxy 127.0.0.1:18791")
            builder.proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 18791)))
        }
        client = builder.build()

        val request = Request.Builder()
            .url(url)
            .header("Origin", "http://$targetHost:$targetPort")
            .header("User-Agent", "OpenClaw-Mobile-AI")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                handler.post {
                    isConnected = false // Not fully connected until handshake
                    isConnecting = false
                    connectionState = ConnectionState.HANDSHAKING
                    notifyListenersState()
                    sendHandshake()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handler.post {
                    handleIncomingMessage(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handler.post {
                    handleDisconnection()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handler.post {
                    Log.e(TAG, "WebSocket Failure: ${t.message}")
                    handleDisconnection(t.message ?: "Unknown error")
                }
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        handleDisconnection()
    }

    private fun handleDisconnection(error: String? = null) {
        isConnected = false
        isConnecting = false
        connectionState = ConnectionState.DISCONNECTED
        webSocket = null
        notifyListenersState()
        error?.let { msg ->
            listeners.forEach { it.onError(msg) }
        }
    }

    private fun sendHandshake() {
        // In v3, the server sends a challenge automatically on socket open.
        // We MUST wait for that challenge event before sending our first 'connect' request.
        // If we send an unsigned connect request now, the server will reject it and CLOSE the socket.
        Log.d(TAG, "Waiting for Handshake Challenge (v3)...")
        connectionState = ConnectionState.HANDSHAKING
        notifyListenersState()
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            val type = json.get("type")?.asString
            val id = json.get("id")?.asString
            val event = json.get("event")?.asString

            Log.d(TAG, "[Rx]: $text")

            // 1. Handle Response (res)
            if (type == "res" && id != null) {
                val callback = pendingRequests.remove(id)
                if (json.get("ok")?.asBoolean == true) {
                    callback?.invoke(gson.toJson(json.get("payload")), null)
                } else {
                    val errorObj = json.getAsJsonObject("error")
                    val errorCode = errorObj?.get("code")?.asString ?: ""
                    val errorMessage = errorObj?.get("message")?.asString ?: "Unknown server error"
                    
                    Log.e(TAG, "Gateway Error: [$errorCode] $errorMessage")
                    
                    if (errorCode == "NOT_PAIRED" || errorMessage.contains("NOT_PAIRED") || errorMessage.contains("DEVICE_IDENTITY_REQUIRED") || errorMessage.contains("PAIRING_REQUIRED")) {
                        isPairingRequired = true
                        appContext?.let { lastDeviceIdentity = CryptoUtils.getDeviceIdentity(it) }
                        listeners.forEach { it.onPairingRequired(lastDeviceIdentity) }
                    }
                    callback?.invoke(null, errorMessage)
                }
            }

            // 2. Handle Authorized Success
            if (event == "connect.authorized" || (type == "res" && json.get("ok")?.asBoolean == true && connectionState == ConnectionState.HANDSHAKING)) {
                Log.d(TAG, "✅ Authorized!")
                isConnected = true
                connectionState = ConnectionState.CONNECTED
                notifyListenersState()
                sendCommand("health")
            }
            
            // 3. Handle Challenge (v3 Protocol)
            if (event == "connect.challenge") {
                val payload = json.getAsJsonObject("payload")
                val nonce = payload.get("nonce")?.asString
                if (nonce != null) {
                    handleChallenge(nonce)
                }
            }

            // 5. Handle Chat Events (v5.1)
            if (event == "chat") {
                val payload = json.getAsJsonObject("payload")
                if (payload != null) {
                    val chatEvent = gson.fromJson(payload, ChatEvent::class.java)
                    listeners.forEach { it.onChatEvent(chatEvent) }
                    
                    // Expose latest feedback for AI Insight Card
                    val content = chatEvent.message?.plainText
                    if (!content.isNullOrBlank()) {
                        val trimmed = content.trim()
                        
                        // If it's a delta and it matches the HEARTBEAT_OK prefix, don't show it yet
                        if (chatEvent.state == "streaming" && "HEARTBEAT_OK".startsWith(trimmed)) {
                            // Suppress heartbeat prefixes from flashing (HE, HEAR...)
                        } else {
                            if (chatEvent.state == "final" || chatEvent.state == "streaming") {
                                _latestInsight.value = content
                            }
                        }
                    }
                }
            }

            // 6. Handle metrics update
            if (event == "metrics") {
                val payload = json.getAsJsonObject("payload")
                metrics = GatewayMetrics(
                    cpuUsage = payload.get("cpuUsage")?.asDouble ?: 0.0,
                    ramUsage = payload.get("ramUsage")?.asDouble ?: 0.0,
                    diskUsage = payload.get("diskUsage")?.asDouble ?: 0.0,
                    uptime = payload.get("uptime")?.asString ?: "N/A"
                )
                listeners.forEach { it.onMetricsUpdated(metrics) }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleChallenge(nonce: String) {
        val context = appContext ?: return
        val deviceId = CryptoUtils.getDeviceIdentity(context)
        val signedAtMs = System.currentTimeMillis()
        
        // SIGNATURE PAYLOAD V3: v3|deviceId|clientId|clientMode|role|scopes|signedAtMs|token|nonce|platform|deviceFamily
        val payload = listOf(
            "v3",
            deviceId,
            "openclaw-android",
            "cli",
            "operator",
            "operator.admin",
            signedAtMs.toString(),
            gatewayToken.trim(),
            nonce,
            "android",
            "phone"
        ).joinToString("|")

        Log.d(TAG, "Signing Challenge Payload: $payload")
        val signature = CryptoUtils.signPayload(context, payload) ?: run {
            Log.e(TAG, "Failed to sign payload!")
            return
        }
        Log.d(TAG, "Generated Signature: $signature")
        
        // Use explicit JsonObject to ensure perfect serialization
        val clientObj = JsonObject().apply {
            addProperty("id", "openclaw-android")
            addProperty("displayName", "Android (${deviceId.take(6)})")
            addProperty("version", "2026.3.13")
            addProperty("platform", "android")
            addProperty("deviceFamily", "phone")
            addProperty("mode", "cli")
        }

        val deviceObj = JsonObject().apply {
            addProperty("id", deviceId)
            addProperty("publicKey", CryptoUtils.getPublicKeyBase64Url(context))
            addProperty("signature", signature)
            addProperty("signedAt", signedAtMs)
            addProperty("nonce", nonce)
        }

        val authObj = JsonObject().apply {
            addProperty("token", gatewayToken.trim())
        }

        val paramsObj = JsonObject().apply {
            addProperty("minProtocol", 3)
            addProperty("maxProtocol", 3)
            add("client", clientObj)
            addProperty("role", "operator")
            val scopesArr = com.google.gson.JsonArray()
            scopesArr.add("operator.admin")
            add("scopes", scopesArr)
            add("device", deviceObj)
            add("auth", authObj)
        }

        val response = JsonObject().apply {
            addProperty("type", "req")
            addProperty("id", UUID.randomUUID().toString())
            addProperty("method", "connect")
            add("params", paramsObj)
        }

        val jsonResponse = gson.toJson(response)
        Log.d(TAG, "[Tx]: $jsonResponse")
        webSocket?.send(jsonResponse)
    }

    private fun sendCommand(method: String, params: Map<String, Any>? = null) {
        val request = mapOf(
            "type" to "req",
            "id" to UUID.randomUUID().toString(),
            "method" to method,
            "params" to params
        )
        webSocket?.send(gson.toJson(request))
    }

    suspend fun callRpc(method: String, params: Any? = null): String = suspendCoroutine { continuation ->
        val id = UUID.randomUUID().toString()
        val request = mapOf(
            "type" to "req",
            "id" to id,
            "method" to method,
            "params" to params
        )

        pendingRequests[id] = { result, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error))
            } else {
                continuation.resume(result ?: "")
            }
        }

        if (webSocket?.send(gson.toJson(request)) != true) {
            pendingRequests.remove(id)
            continuation.resumeWithException(Exception("WebSocket not connected"))
        }
    }

    /**
     * Sends a chat message to the gateway using the chat.send RPC.
     */
    suspend fun sendChatMessage(sessionKey: String, message: String, images: List<String>? = null) {
        val params = mutableMapOf<String, Any>(
            "sessionKey" to sessionKey,
            "idempotencyKey" to UUID.randomUUID().toString(),
            "message" to message
        )
        if (images != null) {
            params["images"] = images
        }
        callRpc("chat.send", params)
    }

    /**
     * High-level helper to send a message to the default chat session.
     */
    fun sendGatewayMessage(text: String, images: List<String>? = null) {
        if (!isConnected) return
        
        // Use our serviceScope for the RPC call
        handler.post {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    sendChatMessage("agent:main:main", text, images)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send gateway message: ${e.message}")
                }
            }
        }
    }

    suspend fun autoInstallQCAI(agentId: String = "main", soul: String, heartbeat: String, tools: String) {
        // Master System Runtime Specification (v5.2) - Unified Workspace
        // Gateway whitelist: SOUL.md, HEARTBEAT.md, TOOLS.md, IDENTITY.md, USER.md, etc.
        // STYLE and SKILLS content is embedded inside SOUL.md.
        // QCAI_Proactive skill content goes into TOOLS.md (whitelisted).
        callRpc("agents.files.set", mapOf(
            "agentId" to agentId,
            "name" to "SOUL.md",
            "content" to soul
        ))
        callRpc("agents.files.set", mapOf(
            "agentId" to agentId,
            "name" to "HEARTBEAT.md",
            "content" to heartbeat
        ))
        callRpc("agents.files.set", mapOf(
            "agentId" to agentId,
            "name" to "TOOLS.md",
            "content" to tools
        ))
    }

    fun syncContext(context: ContextObject) {
        if (!isConnected) return
        
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())
        
        val eventCount = context.temporalIntelligence.upcomingEvents.size
        val stress = context.userState.stressScore
        val activity = context.userState.activity.value.name
        
        // Detailed platform telemetry for parity with iOS
        val contextEngine = ContextEngine.getInstance(appContext ?: return)
        val locStatus = if (contextEngine.locationEnabled) (if (context.userState.location != null) "YES" else "DENIED") else "OFF"
        val healthStatus = if (contextEngine.healthEnabled) "YES" else "OFF"
        val motionStatus = if (contextEngine.motionEnabled) "YES" else "OFF"
        
        Log.d(TAG, "📤 [OpenClawService] Syncing Context v5.5: $eventCount events | Activity: $activity | Stress: ${String.format("%.2f", stress)} | Loc: $locStatus | Health: $healthStatus | Motion: $motionStatus")

        val json = gson.toJson(context)
        val message = """
            [CONTEXT_SYNC] $timestamp
            Follow your SOUL.md identity and HEARTBEAT.md execution rules.
            v5.5 Deep Context Planning: Synthesize my vitals (hrv=${context.userState.activity.vitals?.hrv ?: 0}), location, and 7-day outlook into Proactive Value.
            - If P < 0.2: Reply HEARTBEAT_OK
            - If P >= 0.2: Provide a Strategic Affirmation or a 7-Day Planning Insight (Visual Template SOUL.md §4).
            Max 280 characters.
            
            $json
        """.trimIndent()
        
        handler.post {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    sendChatMessage("agent:main:main", message)
                    Log.d(TAG, "✅ Sent Context Sync (v5.5 via Chat)")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed Context Sync: ${e.message}")
                }
            }
        }
    }

    private fun notifyListenersState() {
        listeners.forEach { it.onStateChanged(connectionState) }
    }

    companion object {
        val instance = OpenClawService()
    }
}
