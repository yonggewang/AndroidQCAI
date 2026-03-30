package com.quantumproperty.qcai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.quantumproperty.qcai.ui.screen.MainScreen
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*

import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Preference Manager BEFORE scheduling background sync or loading UI
        com.quantumproperty.qcai.data.PreferenceManager.init(this)
        
        // Schedule Context OS Background Sync
        com.quantumproperty.qcai.service.BackgroundSyncWorker.schedule(this)
        
        // Request Notification Permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                println("Notification Permission Granted: $isGranted")
            }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Create Notification Channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Default Channel"
            val descriptionText = "General Notifications"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("default", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: android.app.NotificationManager =
                getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Force Register FCM Token on Launch (Fix for Server Wipe)
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    println("Fetching FCM registration token failed: ${task.exception}")
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                println("Force registering FCM token (Android): $token")
                
                CoroutineScope(Dispatchers.IO).launch {
                     com.quantumproperty.qcai.data.CityOSService.instance.registerDeviceToken(token, "android")
                }
            }
        } catch (e: Exception) {
            println("Firebase Messaging not available: ${e.message}")
        }

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPreferences = context.getSharedPreferences("qcai_prefs", android.content.Context.MODE_PRIVATE)
            var hasCompletedOnboarding by remember {
                mutableStateOf(sharedPreferences.getBoolean("hasCompletedOnboarding", false))
            }
            var showSplash by remember { mutableStateOf(true) }
            
            MaterialTheme {
                Surface {
                    if (showSplash) {
                        com.quantumproperty.qcai.ui.screen.SplashScreen(onTimeout = { 
                            showSplash = false 
                        })
                    } else {
                        if (hasCompletedOnboarding) {
                            MainScreen()
                        } else {
                            com.quantumproperty.qcai.ui.screen.WelcomeScreen {
                                sharedPreferences.edit().putBoolean("hasCompletedOnboarding", true).apply()
                                hasCompletedOnboarding = true
                            }
                        }
                    }
                }
            }
        }
    }
}
