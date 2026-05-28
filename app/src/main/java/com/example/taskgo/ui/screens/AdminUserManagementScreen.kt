package com.example.taskgo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskgo.data.model.User
import com.example.taskgo.data.model.UserStatus
import com.example.taskgo.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(userViewModel: UserViewModel) {
    val searchQuery by userViewModel.searchQuery.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()

    // Tracks which user is clicked to show the action popup
    var selectedUserForAction by remember { mutableStateOf<User?>(null) }

    // Task #163: Automatically refresh user records registry upon loading screen
    LaunchedEffect(Unit) {
        userViewModel.fetchAllUserRecords()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Manage Users",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF800000),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Task #165: Search Input Box component
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { userViewModel.setSearchQuery(it) },
            label = { Text("Search by name, email, or matric ID") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF800000),
                focusedLabelColor = Color(0xFF800000)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF800000))
            }
        } else {
            val filteredUsers = userViewModel.getFilteredUsers()
            if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No user accounts found.", color = Color.Gray)
                }
            } else {
                // Task #164 & #166: User Registry List View
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredUsers) { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUserForAction = student }, // Handles row selection clicks
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = student.email, color = Color.Gray, fontSize = 14.sp)
                                    Text(text = "Matric: ${student.matric}", color = Color.Gray, fontSize = 12.sp)
                                }

                                // Account Status Badge styling
                                val badgeColor = when(student.status) {
                                    UserStatus.ACTIVE -> Color(0xFF4CAF50)
                                    UserStatus.SUSPENDED -> Color(0xFFFF9800)
                                    UserStatus.BANNED -> Color(0xFFF44336)
                                }

                                Surface(
                                    color = badgeColor.copy(alpha = 0.15f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = student.status.name,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Task #196 & #197: Admin action alert modal to Ban/Suspend selected profiles
    selectedUserForAction?.let { user ->
        // State tracker for switching inside the dialog to the duration select list
        var showSuspensionOptions by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                selectedUserForAction = null
                showSuspensionOptions = false
            },
            title = { Text(text = "Manage: ${user.name}") },
            text = {
                Column {
                    Text("Choose an administration security action to perform on this account.")
                    if (showSuspensionOptions) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Suspension Period:", fontWeight = FontWeight.Bold, color = Color(0xFF800000))
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!showSuspensionOptions) {
                        // Main Action Options View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Button Variant A: Lift restrictions or set Active (Unban)
                            if (user.status != UserStatus.ACTIVE) {
                                Button(
                                    onClick = {
                                        userViewModel.modifyUserAccountStatus(user.id, UserStatus.ACTIVE)
                                        selectedUserForAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Activate", color = Color.White) }
                            }

                            // Button Variant B: Open Suspension Duration Panel
                            if (user.status == UserStatus.ACTIVE) {
                                Button(
                                    onClick = { showSuspensionOptions = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Suspend...", color = Color.White) }
                            }

                            // Button Variant C: Permanent Ban
                            if (user.status != UserStatus.BANNED) {
                                Button(
                                    onClick = {
                                        userViewModel.modifyUserAccountStatus(user.id, UserStatus.BANNED)
                                        selectedUserForAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Ban", color = Color.White) }
                            }
                        }
                    } else {
                        // Custom Suspension Durations Panel View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    userViewModel.modifyUserAccountStatus(user.id, UserStatus.SUSPENDED, suspensionDays = 3)
                                    selectedUserForAction = null
                                    showSuspensionOptions = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) { Text("3 Days", fontSize = 12.sp) }

                            Button(
                                onClick = {
                                    userViewModel.modifyUserAccountStatus(user.id, UserStatus.SUSPENDED, suspensionDays = 7)
                                    selectedUserForAction = null
                                    showSuspensionOptions = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) { Text("7 Days", fontSize = 12.sp) }

                            Button(
                                onClick = {
                                    userViewModel.modifyUserAccountStatus(user.id, UserStatus.SUSPENDED, suspensionDays = 30)
                                    selectedUserForAction = null
                                    showSuspensionOptions = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) { Text("30 Days", fontSize = 12.sp) }
                        }

                        TextButton(
                            onClick = { showSuspensionOptions = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Actions", color = Color.Gray)
                        }
                    }
                }
            }
        )
    }
}