package com.quantumproperty.qcai.utils

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object BrowserUtils {
    /**
     * Opens a URL in a Chrome Custom Tab for a standard in-app browsing experience.
     * Falls back to a standard browser intent if Custom Tabs are not supported.
     */
    fun openURL(context: Context, url: String) {
        if (url.isBlank()) {
            android.util.Log.e("BrowserUtils", "Cannot open blank URL")
            return
        }
        
        // Ensure URL has a scheme
        var finalUrl = url.trim()
        if (!finalUrl.startsWith("http://", ignoreCase = true) && 
            !finalUrl.startsWith("https://", ignoreCase = true)) {
            finalUrl = "https://$finalUrl"
        }

        android.util.Log.i("BrowserUtils", "Attempting to open URL (Android): $finalUrl")
        
        try {
            val intentBuilder = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .setInstantAppsEnabled(true)
            
            val customTabsIntent = intentBuilder.build()
            
            // Adding NEW_TASK flag only if we are NOT at an Activity context
            if (context !is android.app.Activity) {
                customTabsIntent.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            android.util.Log.d("BrowserUtils", "Launching Chrome Custom Tab...")
            customTabsIntent.launchUrl(context, Uri.parse(finalUrl))
            android.util.Log.i("BrowserUtils", "Chrome Custom Tab launch command sent.")
        } catch (e: Exception) {
            android.util.Log.w("BrowserUtils", "Chrome Custom Tabs failed, trying standard browser. Error: ${e.message}", e)
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(finalUrl))
                if (context !is android.app.Activity) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                android.util.Log.i("BrowserUtils", "Standard browser intent launched successfully.")
            } catch (fallbackEx: Exception) {
                android.util.Log.e("BrowserUtils", "CRITICAL: Final fallback failure for URL: $finalUrl. Error: ${fallbackEx.message}", fallbackEx)
            }
        }
    }
}
