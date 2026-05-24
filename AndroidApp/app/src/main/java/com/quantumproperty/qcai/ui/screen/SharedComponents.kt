package com.quantumproperty.qcai.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quantumproperty.qcai.ui.viewmodel.TeacherViewModel

import com.quantumproperty.qcai.data.AppLanguage
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

// Extracted from TeacherScreen.kt to be shared
@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPassword: (String) -> Unit,
    appLanguage: AppLanguage
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                when {
                    isSpanish -> "Iniciar Sesión"
                    isEnglish -> "Login"
                    else -> "登录"
                }
            ) 
        },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Correo electrónico"
                                isEnglish -> "Email"
                                else -> "邮箱"
                            }
                        ) 
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Contraseña"
                                isEnglish -> "Password"
                                else -> "密码"
                            }
                        ) 
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                TextButton(onClick = { onForgotPassword(email) }) {
                     Text(
                        when {
                            isSpanish -> "¿Olvidaste tu contraseña?"
                            isEnglish -> "Forgot Password?"
                            else -> "忘记密码？"
                        },
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onLogin(email, password) }) {
                Text(
                    when {
                        isSpanish -> "Acceso"
                        isEnglish -> "Login"
                        else -> "登录"
                    }
                )
            }
        },
        dismissButton = {
            Row {
                 TextButton(onClick = onRegisterClick) {
                    Text(
                        when {
                            isSpanish -> "Registro"
                            isEnglish -> "Register"
                            else -> "注册"
                        }
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        when {
                            isSpanish -> "Cancelar"
                            isEnglish -> "Cancel"
                            else -> "取消"
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun RegisterDialog(
    onDismiss: () -> Unit,
    onRegister: (String, String, String, String, String) -> Unit,
    onLoginClick: () -> Unit,
    appLanguage: AppLanguage
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") } // User Handle
    var phone by remember { mutableStateOf("") }

    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                when {
                    isSpanish -> "Registro"
                    isEnglish -> "Register"
                    else -> "注册"
                }
            ) 
        },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                OutlinedTextField(
                    value = email, 
                    onValueChange = { email = it }, 
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Correo electrónico"
                                isEnglish -> "Email"
                                else -> "电子邮箱"
                            }
                        ) 
                    }, 
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password, 
                    onValueChange = { password = it }, 
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Contraseña"
                                isEnglish -> "Password"
                                else -> "密码"
                            }
                        ) 
                    }, 
                    visualTransformation = PasswordVisualTransformation(), 
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Nombre"
                                isEnglish -> "Name"
                                else -> "姓名"
                            }
                        ) 
                    }, 
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username, 
                    onValueChange = { username = it }, 
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Nombre de usuario"
                                isEnglish -> "Username"
                                else -> "用户名"
                            }
                        ) 
                    }, 
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone, 
                    onValueChange = { phone = it }, 
                    label = { 
                        Text(
                            when {
                                isSpanish -> "Teléfono (Opcional)"
                                isEnglish -> "Phone (Optional)"
                                else -> "电话（可选）"
                            }
                        ) 
                    }, 
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onRegister(email, password, name, username, phone) }) {
                Text(
                    when {
                        isSpanish -> "Inscribirse"
                        isEnglish -> "Sign Up"
                        else -> "注册"
                    }
                )
            }
        },
        dismissButton = {
             Row {
                TextButton(onClick = onLoginClick) {
                    Text(
                        when {
                            isSpanish -> "Acceso"
                            isEnglish -> "Login"
                            else -> "登录"
                        }
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        when {
                            isSpanish -> "Cancelar"
                            isEnglish -> "Cancel"
                            else -> "取消"
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun APIKeySetupDialog(onDismiss: () -> Unit) {
    // Deprecated or moved logic
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Key") },
        text = { Text("API Keys are now managed by the backend.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
fun RealEstateInputArea(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onChatWithAI: () -> Unit,
    modifier: Modifier = Modifier,
    appLanguage: AppLanguage
) {
    var address by remember { mutableStateOf("") }
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val isSpanish = appLanguage == AppLanguage.SPANISH

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isResolvingId by remember { mutableStateOf(false) }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = when {
                isSpanish -> "Analizar Propiedad"
                isEnglish -> "Analyze Property"
                else -> "房产分析"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { 
                Text(
                    when {
                        isSpanish -> "Ingresar dirección"
                        isEnglish -> "Enter Address"
                        else -> "输入地址"
                    }
                ) 
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onChatWithAI) {
                Text(
                    when {
                        isSpanish -> "Preguntar IA"
                        isEnglish -> "Ask AI"
                        else -> "咨询AI"
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onConfirm(address); onDismiss() },
                enabled = address.isNotBlank()
            ) {
                Text(
                    when {
                        isSpanish -> "Analizar"
                        isEnglish -> "Analyze"
                        else -> "开始分析"
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isResolvingId = true
                    scope.launch {
                        val res = com.quantumproperty.qcai.data.PropertyDataService().resolveSpatialestId(address)
                        isResolvingId = false
                        if (res != null) {
                            com.quantumproperty.qcai.utils.BrowserUtils.openURL(context, "https://polaris3g.mecklenburgcountync.gov/address/$res")
                        } else {
                            android.widget.Toast.makeText(context, if (isEnglish) "Could not resolve property ID" else "无法解析房产 ID", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                enabled = !isResolvingId && address.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (isResolvingId) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Polaris", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    isResolvingId = true
                    scope.launch {
                        val res = com.quantumproperty.qcai.data.PropertyDataService().resolveSpatialestId(address)
                        isResolvingId = false
                        if (res != null) {
                            com.quantumproperty.qcai.utils.BrowserUtils.openURL(context, "https://property.spatialest.com/nc/mecklenburg/#/property/$res")
                        } else {
                            android.widget.Toast.makeText(context, if (isEnglish) "Could not resolve property ID" else "无法解析房产 ID", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !isResolvingId && address.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (isResolvingId) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Spatialest", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isEnglish) "Owner Search" else "业主搜索",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        var ownerFirstName by remember { mutableStateOf("") }
        var ownerLastName by remember { mutableStateOf("") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = ownerFirstName,
                onValueChange = { ownerFirstName = it },
                label = { Text(if (isEnglish) "First Name" else "名字") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ownerLastName,
                onValueChange = { ownerLastName = it },
                label = { Text(if (isEnglish) "Last Name" else "姓氏") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val cleanFirst = android.net.Uri.encode(ownerFirstName.trim().replace(" ", "+"))
                    val cleanLast = android.net.Uri.encode(ownerLastName.trim().replace(" ", "+"))
                    val url = "https://polaris3g.mecklenburgcountync.gov/ownerfull/$cleanFirst+$cleanLast"
                    com.quantumproperty.qcai.utils.BrowserUtils.openURL(context, url)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                enabled = ownerFirstName.isNotBlank() || ownerLastName.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Polaris", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val cleanFirst = android.net.Uri.encode(ownerFirstName.trim())
                    val cleanLast = android.net.Uri.encode(ownerLastName.trim())
                    val url = "https://property.spatialest.com/nc/mecklenburg/#/search/?term=$cleanLast%2C%20$cleanFirst&page=1"
                    com.quantumproperty.qcai.utils.BrowserUtils.openURL(context, url)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = ownerFirstName.isNotBlank() || ownerLastName.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Spatialest", fontWeight = FontWeight.Bold)
            }
        }
    }
}
