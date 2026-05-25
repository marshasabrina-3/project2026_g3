package com.example.taskgo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskgo.R
import com.example.taskgo.data.model.UserRole
import com.example.taskgo.ui.viewmodel.UserViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    userViewModel: UserViewModel = viewModel(),
    onLoginSuccess: (UserRole) -> Unit, // Updated from String to your team's UserRole Enum class type
    onNavigateToRegister: () -> Unit
) {
    var emailPrefix by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Local state to capture and present immediate validation issues (Task #157)
    var localValidationError by remember { mutableStateOf<String?>(null) }

    // UTM Maroon and a darker variant for gradient
    val utmMaroon = Color(0xFF800000)
    val utmDarkMaroon = Color(0xFF4D0000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(utmMaroon, utmDarkMaroon)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo Section
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(12.dp, CircleShape),
                shape = CircleShape,
                color = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.utmlogo),
                    contentDescription = "UTM Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TaskGO",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 42.sp
                ),
                color = Color.White
            )
            Text(
                text = "UTM Student Service Marketplace",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Login Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Welcome Back",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = utmMaroon
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = emailPrefix,
                        onValueChange = {
                            emailPrefix = it
                            localValidationError = null // Clear message when typing
                        },
                        label = { Text("UTM Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = utmMaroon) },
                        suffix = { Text("@graduate.utm.my", color = Color.Gray, fontSize = 12.sp) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            localValidationError = null // Clear message when typing
                        },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = utmMaroon) },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                            }
                        },
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = utmMaroon)
                            )
                            Text("Remember Me", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { if (emailPrefix.isNotBlank()) showResetDialog = true }) {
                            Text("Forgot Password?", color = utmMaroon, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val emailTrimmed = emailPrefix.trim()
                            val passwordTrimmed = password.trim()

                            // 1. Client-Side Login Validation Enforcement Flow (Task #157)
                            if (emailTrimmed.isEmpty()) {
                                localValidationError = "UTM Email identifier cannot be left blank!"
                            } else if (passwordTrimmed.isEmpty()) {
                                localValidationError = "Account access password cannot be left blank!"
                            } else {
                                localValidationError = null
                                // 2. Trigger async authentication call checking rules from Firestore (Task #158)
                                userViewModel.login(emailTrimmed, passwordTrimmed, rememberMe) { success, analyzedRole ->
                                    if (success && analyzedRole != null) {
                                        // 3. Forward the role enum to control system layout routing rules (Task #159)
                                        onLoginSuccess(analyzedRole)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = utmMaroon)
                    ) {
                        if (userViewModel.isLoading.collectAsState().value) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text("LOGIN", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp)
                        }
                    }

                    // Read explicit errors received directly from Firebase execution failures
                    val error by userViewModel.error.collectAsState()

                    // Renders local input issues immediately
                    if (localValidationError != null) {
                        Text(
                            text = localValidationError!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Renders structural system or connection faults
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text(
                    "Don't have an account? ",
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    "Register Now",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Password") },
            text = { Text("A password reset link has been sent to ${emailPrefix}@graduate.utm.my") },
            confirmButton = {
                Button(onClick = { showResetDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = utmMaroon)) {
                    Text("OK")
                }
            }
        )
    }
}