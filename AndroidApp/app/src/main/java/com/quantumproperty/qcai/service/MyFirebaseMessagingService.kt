package com.quantumproperty.qcai.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.quantumproperty.qcai.data.CityOSService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Register token with backend
        CoroutineScope(Dispatchers.IO).launch {
            CityOSService.instance.registerDeviceToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Handle incoming message if needed (Android handles tray notifications automatically if 'notification' payload exists)
    }
}
