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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskgo.R

import com.example.taskgo.data.model.User
import com.example.taskgo.ui.viewmodel.UserViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegisterScreen(
    userViewModel: UserViewModel = viewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var studentId by remember { mutableStateOf("") }
    var nric by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var emailPrefix by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val currentUser by userViewModel.currentUser.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val error by userViewModel.error.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onRegisterSuccess()
        }
    }

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo Section
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                color = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.utmlogo),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Join TaskGO",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = Color.White
            )
            Text(
                text = "Create your student account",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        label = { Text("Matric No") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = nric,
                        onValueChange = { nric = it },
                        label = { Text("NRIC Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        placeholder = { Text("e.g. 000101-01-0001") },
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = emailPrefix,
                        onValueChange = { emailPrefix = it },
                        label = { Text("UTM Email Prefix") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        suffix = { Text("@graduate.utm.my", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
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
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        colors = textFieldColors,
                        singleLine = true
                    )

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val email = if (emailPrefix.contains("@")) emailPrefix else "$emailPrefix@graduate.utm.my"
                            val user = User(
                                id = "", // Will be set by ViewModel
                                name = name,
                                matric = studentId,
                                nric = nric,
                                email = email,
                                phoneNumber = phone
                            )
                            userViewModel.register(user, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("REGISTER", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? ", color = Color.White.copy(alpha = 0.8f))
                Text("Login", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
