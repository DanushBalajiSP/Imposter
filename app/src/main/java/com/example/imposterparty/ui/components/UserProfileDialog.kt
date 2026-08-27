package com.example.imposterparty.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.imposterparty.data.model.UserProfile
import com.example.imposterparty.theme.*
import com.example.imposterparty.viewmodel.GameViewModel

private enum class AuthMode {
    CREATE,
    LOGIN,
}

@Composable
fun UserProfileDialog(
    gameViewModel: GameViewModel,
    currentUser: UserProfile?,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf(AuthMode.CREATE) }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = StitchSurfaceContainer,
            border = BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // If user is already logged in
                if (currentUser != null) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NeonCyan.copy(alpha = 0.3f), Color.Transparent)))
                            .border(BorderStroke(2.dp, NeonCyan), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = currentUser.username.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                            ),
                            color = NeonCyanSoft,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = currentUser.username,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        color = StitchSurfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.3f)),
                    ) {
                        Text(
                            text = "ID: ${currentUser.userId}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            ),
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Log out / Switch Account button
                    OutlinedButton(
                        onClick = {
                            gameViewModel.logoutProfile()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DangerRed,
                        ),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Log Out / Switch Account", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                } else {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (mode == AuthMode.CREATE) "Create Profile" else "Account Login",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                            ),
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = OnSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Mode Toggle (Create vs Login)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StitchSurfaceContainerHigh)
                            .padding(3.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (mode == AuthMode.CREATE) NeonPurple else Color.Transparent)
                                .clickable {
                                    mode = AuthMode.CREATE
                                    errorMessage = null
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "New Account",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (mode == AuthMode.CREATE) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = if (mode == AuthMode.CREATE) Color.White else OnSurfaceVariant,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (mode == AuthMode.LOGIN) NeonPurple else Color.Transparent)
                                .clickable {
                                    mode = AuthMode.LOGIN
                                    errorMessage = null
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Log In",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (mode == AuthMode.LOGIN) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = if (mode == AuthMode.LOGIN) Color.White else OnSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Username Input
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text("Username") },
                        placeholder = { Text("e.g. ShadowMaster, Danush") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = OutlineSubtle,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = OnSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )

                    Spacer(Modifier.height(12.dp))

                    // 4-Digit Security PIN Input
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                pin = it
                                errorMessage = null
                            }
                        },
                        label = { Text("4-Digit Security PIN") },
                        placeholder = { Text("••••") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = NeonGold)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGold,
                            unfocusedBorderColor = OutlineSubtle,
                            focusedLabelColor = NeonGold,
                            unfocusedLabelColor = OnSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )

                    // Confirm PIN if in CREATE mode
                    if (mode == AuthMode.CREATE) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                    confirmPin = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Confirm 4-Digit PIN") },
                            placeholder = { Text("••••") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = NeonGold)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGold,
                                unfocusedBorderColor = OutlineSubtle,
                                focusedLabelColor = NeonGold,
                                unfocusedLabelColor = OnSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                        )
                    }

                    // Helper note
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (mode == AuthMode.CREATE)
                            "Your 4-digit PIN secures your account so you can log in on other devices without passwords or OAuth."
                        else
                            "Enter your existing username and the 4-digit PIN you set up during registration.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = OnSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Error Message
                    if (errorMessage != null) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = DangerRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = DangerRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (username.isBlank()) {
                                errorMessage = "Please enter a username."
                                return@Button
                            }
                            if (pin.length != 4) {
                                errorMessage = "PIN must be exactly 4 digits."
                                return@Button
                            }
                            if (mode == AuthMode.CREATE && pin != confirmPin) {
                                errorMessage = "PINs do not match."
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null

                            if (mode == AuthMode.CREATE) {
                                gameViewModel.createProfile(username, pin) { result ->
                                    isLoading = false
                                    result.onSuccess {
                                        onDismiss()
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Failed to create account."
                                    }
                                }
                            } else {
                                gameViewModel.loginProfile(username, pin) { result ->
                                    isLoading = false
                                    result.onSuccess {
                                        onDismiss()
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Failed to log in."
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && username.isNotBlank() && pin.length == 4 && (mode == AuthMode.LOGIN || confirmPin.length == 4),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = if (mode == AuthMode.CREATE) "Create Profile & Save" else "Log In to Account",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}
