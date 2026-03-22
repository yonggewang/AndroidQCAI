package com.quantumproperty.qcai.native

import android.util.Log
import android.system.Os

/**
 * JNI Interface for the internal Tailscale bridge (tsnet).
 * This class interacts with the native library compiled from bridge.go.
 */
object TailscaleBridge {
    private const val TAG = "TailscaleBridge"

    init {
        try {
            // CRITICAL: Set environment variables before any Go code initializes
            Os.setenv("TS_DEBUG_NO_NETLINK", "1", true)
            Os.setenv("TS_NO_EXTERNAL_CONFIG", "1", true)
            Os.setenv("TS_PORTABLE_NETSTACK", "1", true)
            Os.setenv("TS_NO_SYSNETWORK_CONNS", "1", true)
            Os.setenv("TS_DEBUG_NO_ROUTING", "1", true)
            Os.setenv("TS_DEBUG_IGNORE_NETLINK_ERRORS", "1", true)
            Log.d(TAG, "Native environment variables set (netlink bypass active)")
            
            System.loadLibrary("tailscalebridge")
            Log.d(TAG, "Native Tailscale library loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize native bridge: ${e.message}")
        }
    }

    @JvmStatic
    private external fun connect(authKey: String, hostname: String, stateDir: String): String?
    
    @JvmStatic
    private external fun disconnect()
    
    @JvmStatic
    private external fun getIP(): String?
    
    @JvmStatic
    private external fun getState(): String?

    /**
     * Start the internal Tailscale node.
     */
    fun start(authKey: String, hostname: String?, stateDir: String): String {
        try {
            val error = connect(authKey, hostname ?: "", stateDir)
            if (error != null) {
                throw Exception(error)
            }
            val ip = getIP() ?: ""
            if (ip == "") {
                throw Exception("Connection Timeout: No IP assigned")
            }
            Log.d(TAG, "Native Tailscale connected. IP: $ip")
            return ip
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start native tailscale: ${e.message}")
            throw e
        }
    }

    /**
     * Stop the internal Tailscale node.
     */
    fun stop() {
        try {
            disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop native tailscale: ${e.message}")
        }
    }

    /**
     * Get the current Tailscale IP address.
     */
    fun getIPAddress(): String {
        return try {
            getIP() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get the current backend state.
     */
    fun getStatus(): String {
        return try {
            getState() ?: ""
        } catch (e: Exception) {
            "Error"
        }
    }

    fun isReady(): Boolean = true
}
