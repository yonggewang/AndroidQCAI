package com.quantumproperty.qcai.utils

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import java.security.MessageDigest
import java.util.*

object CryptoUtils {

    private const val PREF_NAME = "openclaw_crypto_prefs"
    private const val KEY_PRIVATE = "device_private_key"

    /**
     * Loads or generates the device's persistent Ed25519 private key.
     */
    fun getOrGeneratePrivateKey(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val storedKey = prefs.getString(KEY_PRIVATE, null)
        
        if (storedKey != null) {
            return Base64.decode(storedKey, Base64.DEFAULT)
        }
        
        // Generate new Ed25519 key pair
        val keyPair = Ed25519Sign.KeyPair.newKeyPair()
        val privateKey = keyPair.privateKey
        
        prefs.edit().putString(KEY_PRIVATE, Base64.encodeToString(privateKey, Base64.DEFAULT)).apply()
        return privateKey
    }

    /**
     * Gets the HEX-encoded SHA256 hash of the Ed25519 public key.
     * Replicates iOS OpenClawService.deviceIdentity.
     */
    fun getDeviceIdentity(context: Context): String {
        val privateKey = getOrGeneratePrivateKey(context)
        val keyPair = Ed25519Sign.KeyPair.newKeyPairFromSeed(privateKey)
        val publicKey = keyPair.publicKey
        
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(publicKey)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Returns the Base64Url-encoded public key (no padding).
     * Replicates iOS OpenClawService.publicKeyBase64Url.
     */
    fun getPublicKeyBase64Url(context: Context): String {
        val privateKey = getOrGeneratePrivateKey(context)
        val keyPair = Ed25519Sign.KeyPair.newKeyPairFromSeed(privateKey)
        val publicKey = keyPair.publicKey
        
        return Base64.encodeToString(publicKey, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Signs a UTF-8 payload using the Ed25519 private key.
     * Returns Base64Url-encoded signature (no padding).
     * Replicates iOS OpenClawService.signPayload.
     */
    fun signPayload(context: Context, payload: String): String? {
        return try {
            val privateKey = getOrGeneratePrivateKey(context)
            val signer = Ed25519Sign(privateKey)
            val signature = signer.sign(payload.toByteArray(Charsets.UTF_8))
            
            Base64.encodeToString(signature, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * SHA256 helper for general use.
     */
    fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
