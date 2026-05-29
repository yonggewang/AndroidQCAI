package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputArea(
    onSend: (String) -> Unit, 
    onCameraClick: () -> Unit, 
    onMicClick: () -> Unit,
    isRecording: Boolean,
    placeholder: String
) {
    var text by remember { mutableStateOf("") }
    
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)

                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCameraClick,
                colors = IconButtonDefaults.iconButtonColors(contentColor = PrimaryPurple)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
            }
            Spacer(Modifier.width(4.dp))

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            println("SharedInput: Keyboard Send Action clicked. Calling onSend callback with '$text'")
                            onSend(text)
                            text = ""
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    println("SharedInput: IconButton clicked. text.isNotBlank=${text.isNotBlank()}, text='$text'")
                    if (text.isNotBlank()) {
                        println("SharedInput: Calling onSend callback with '$text'")
                        onSend(text)
                        text = ""
                    } else {
                        onMicClick()
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isRecording) Color.Red else Color.Transparent,
                    contentColor = if (isRecording) Color.White else PrimaryPurple
                )
            ) {
                Icon(
                    imageVector = when {
                        isRecording -> Icons.Default.Stop
                        text.isNotBlank() -> Icons.Default.Send
                        else -> Icons.Default.Mic
                    }, 
                    contentDescription = if (isRecording) "Stop" else "Send/Mic"
                )
            }
        }
    }
}
