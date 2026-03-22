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

    interface OpenClawListener {
        fun onStateChanged(state: ConnectionState)
        fun onMetricsUpdated(metrics: GatewayMetrics)
        fun onPairingRequired(deviceId: String)
        fun onError(message: String)
    }

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

            // 4. Handle metrics update
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
            "openclaw-ios",
            "cli",
            "operator",
            "operator.admin",
            signedAtMs.toString(),
            gatewayToken.trim(),
            nonce,
            "ios",
            "iphone"
        ).joinToString("|")

        Log.d(TAG, "Signing Challenge Payload: $payload")
        val signature = CryptoUtils.signPayload(context, payload) ?: run {
            Log.e(TAG, "Failed to sign payload!")
            return
        }
        Log.d(TAG, "Generated Signature: $signature")
        
        // Use explicit JsonObject to ensure perfect serialization
        val clientObj = JsonObject().apply {
            addProperty("id", "openclaw-ios")
            addProperty("displayName", "iPhone (${deviceId.take(6)})")
            addProperty("version", "2026.3.13")
            addProperty("platform", "ios")
            addProperty("deviceFamily", "iphone")
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

    suspend fun callRpc(method: String, params: Map<String, Any>? = null): String = suspendCoroutine { continuation ->
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

    private fun notifyListenersState() {
        listeners.forEach { it.onStateChanged(connectionState) }
    }

    companion object {
        val instance = OpenClawService()
    }
}
