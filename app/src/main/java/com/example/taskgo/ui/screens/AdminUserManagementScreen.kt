package com.example.taskgo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    val allUsers by userViewModel.allUsers.collectAsState()

    var currentStatusFilter by remember { mutableStateOf("ALL") }
    val filterOptions = listOf("ALL", "ACTIVE", "SUSPENDED", "BANNED")

    // Tracks which user is clicked to show the action popup
    var selectedUserForAction by remember { mutableStateOf<User?>(null) }

    // Task #163: Automatically refresh user records registry upon loading screen
    LaunchedEffect(Unit) {
        userViewModel.fetchAllUserRecords()
    }

    val sortedAndFilteredUsers = remember(allUsers, searchQuery, currentStatusFilter) {
        allUsers.filter { user ->
            val matchesSearch = user.name.contains(searchQuery, ignoreCase = true) ||
                               user.email.contains(searchQuery, ignoreCase = true) ||
                               user.matric.contains(searchQuery, ignoreCase = true)
            val matchesFilter = if (currentStatusFilter == "ALL") true
                               else user.status.name == currentStatusFilter
            matchesSearch && matchesFilter
        }.sortedWith(compareBy({
            // Priority: Active (0) > Suspended (1) > Banned (2)
            when(it.status) {
                UserStatus.ACTIVE -> 0
                UserStatus.SUSPENDED -> 1
                UserStatus.BANNED -> 2
            }
        }, { it.name }))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Manage Users",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Task #165: Search Input Box component with Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { userViewModel.setSearchQuery(it) },
                label = { Text("Search users...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            var showFilterMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showFilterMenu = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.FilterAlt, contentDescription = "Filter")
                }
                DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                    filterOptions.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                currentStatusFilter = status
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            if (sortedAndFilteredUsers.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No user accounts found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Task #164 & #166: User Registry List View
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sortedAndFilteredUsers) { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUserForAction = student }, // Handles row selection clicks
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = student.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    Text(text = "Matric: ${student.matric}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = "Manage: ${user.name}", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Choose an administration security action to perform on this account.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (showSuspensionOptions) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Set Suspension Duration:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        var days by remember { mutableStateOf("") }
                        var hours by remember { mutableStateOf("") }
                        var minutes by remember { mutableStateOf("1") }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = days,
                                onValueChange = { if (it.all { c -> c.isDigit() }) days = it },
                                label = { Text("Days", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = hours,
                                onValueChange = { if (it.all { c -> c.isDigit() }) hours = it },
                                label = { Text("Hours", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = minutes,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) {
                                        minutes = it
                                    }
                                },
                                label = { Text("Mins", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        val totalMinutes = remember(days, hours, minutes) {
                            val d = days.toLongOrNull() ?: 0L
                            val h = hours.toLongOrNull() ?: 0L
                            val m = minutes.toLongOrNull() ?: 0L

                            var result = (d * 24 * 60) + (h * 60) + m
                            if (result < 1 && d == 0L && h == 0L) result = 1
                            result
                        }

                        Text(
                            text = "Suspended for: ${totalMinutes / 1440}d ${ (totalMinutes % 1440) / 60}h ${totalMinutes % 60}m",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                userViewModel.modifyUserAccountStatus(user.id, UserStatus.SUSPENDED, totalMinutes)
                                selectedUserForAction = null
                                showSuspensionOptions = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("Confirm Suspension", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                if (!showSuspensionOptions) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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

                            if (user.status == UserStatus.ACTIVE) {
                                Button(
                                    onClick = { showSuspensionOptions = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Suspend", color = Color.White) }
                            }

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
                    }
                }
            },
            dismissButton = {
                if (showSuspensionOptions) {
                    TextButton(onClick = { showSuspensionOptions = false }) {
                        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}
