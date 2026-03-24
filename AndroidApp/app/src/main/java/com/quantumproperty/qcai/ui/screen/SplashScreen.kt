package com.quantumproperty.qcai.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var textDelayed by remember { mutableStateOf(false) }
    
    // Core Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerScale"
    )
    
    val outerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerAlpha"
    )
    
    val innerScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "innerScale"
    )

    LaunchedEffect(Unit) {
        delay(800)
        textDelayed = true
        delay(1400) // Total 2200ms
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF19194D), Color.Black),
                    center = androidx.compose.ui.geometry.Offset.Unspecified,
                    radius = 1000f,
                    tileMode = TileMode.Clamp
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // 2. Pulsing AI Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(outerScale)
                        .alpha(outerAlpha)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF).copy(alpha = 0.15f))
                )
                
                // Middle glow
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(innerScale)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF).copy(alpha = 0.3f))
                )
                
                // Main Symbol
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(80.dp)
            ) {
                Text(
                    text = "QUEEN CITY AI",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                
                AnimatedVisibility(
                    visible = textDelayed,
                    enter = fadeIn() + expandVertically()
                ) {
                    Text(
                        text = "Loading City Intelligence...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF007AFF).copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "C O N T E X T   O S",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}
