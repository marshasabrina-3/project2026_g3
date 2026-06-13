package com.example.taskgo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
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
    var showSupportDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // Local state to capture and present immediate validation issues (Task #157)
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val isLoading by userViewModel.isLoading.collectAsState()
    val error by userViewModel.error.collectAsState()

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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo Section
            Surface(
                modifier = Modifier
                    .size(200.dp)
                    .shadow(12.dp, CircleShape),
                shape = CircleShape,
                color = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.taskgologo),
                    contentDescription = "UTM Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "A Campus Task & Runner App For UTM",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Login Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        color = MaterialTheme.colorScheme.primary
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
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        suffix = { Text("@graduate.utm.my", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
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
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
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
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text("Remember Me", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        TextButton(onClick = { if (emailPrefix.isNotBlank()) showResetDialog = true }) {
                            Text("Forgot Password?", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val emailTrimmed = emailPrefix.trim()
                            val passwordTrimmed = password.trim()

                            if (emailTrimmed.isEmpty()) {
                                localValidationError = "UTM Email identifier cannot be left blank!"
                            } else if (passwordTrimmed.isEmpty()) {
                                localValidationError = "Account access password cannot be left blank!"
                            } else {
                                localValidationError = null
                                userViewModel.login(emailTrimmed, passwordTrimmed, rememberMe) { success, role ->
                                    if (success && role != null) {
                                        onLoginSuccess(role)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Text("LOGIN", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp)
                        }
                    }

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

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { showSupportDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Support", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (showSupportDialog) {
        val clipboardManager = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("TaskGO Support", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("If you encounter any issues, please contact our administrator:", style = MaterialTheme.typography.bodyMedium)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Name:", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("Wan Muhamad Danish Bin Wan Muhamad Nubli", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Email:", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("wanmuhamaddanish@graduate.utm.my")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString("wanmuhamaddanish@graduate.utm.my"))
                    android.widget.Toast.makeText(context, "Email copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copy Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Close")
                }
            }
        )
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