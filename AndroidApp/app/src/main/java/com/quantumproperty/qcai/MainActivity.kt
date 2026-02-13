package com.quantumproperty.qcai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.quantumproperty.qcai.ui.screen.TeacherScreen
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme

import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Notification Permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                println("Notification Permission Granted: $isGranted")
            }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                Surface {
                    TeacherScreen()
                }
            }
        }
    }
}
