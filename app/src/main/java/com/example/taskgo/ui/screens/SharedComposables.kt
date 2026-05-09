package com.example.taskgo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.data.model.User
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import java.util.Locale
import coil.compose.AsyncImage

@Composable
fun EditTaskDialog(task: Task, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description) }
    var amount by remember { mutableStateOf(task.paymentAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Edit Task", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: task.paymentAmount
                onSave(task.copy(title = title, description = desc, paymentAmount = amt))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: Task,
    userViewModel: UserViewModel,
    taskViewModel: TaskViewModel,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val isRequester = currentUser?.id == task.requesterId
    val utmMaroon = Color(0xFF800000)

    var interestedRunners by remember { mutableStateOf<List<User>>(emptyList()) }
    var showConfirmCompleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(task.interestedRunnerIds) {
        if (isRequester) {
            interestedRunners = taskViewModel.getInterestedRunners(task.interestedRunnerIds)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        Scaffold(
            bottomBar = {
                if (!isRequester) {
                    Surface(
                        tonalElevation = 12.dp,
                        shadowElevation = 12.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = onReport,
                                modifier = Modifier.weight(0.3f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.Red),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(20.dp))
                            }

                            Button(
                                onClick = { /* Navigate to Chat */ },
                                modifier = Modifier.weight(0.3f).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = utmMaroon),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = Color.White)
                            }
                            
                            val hasApplied = task.interestedRunnerIds.contains(currentUser?.id)
                            val isAssignedRunner = currentUser?.id == task.runnerId

                            if (task.status == TaskStatus.ASSIGNED && isAssignedRunner) {
                                Button(
                                    onClick = { taskViewModel.updateTask(task.copy(status = TaskStatus.WAITING_VERIFICATION)) },
                                    modifier = Modifier.weight(0.5f).height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Mark as Complete", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            } else if (task.status == TaskStatus.WAITING_VERIFICATION && isAssignedRunner) {
                                Button(
                                    onClick = { },
                                    enabled = false,
                                    modifier = Modifier.weight(0.5f).height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Pending Verification", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = onAccept,
                                    enabled = task.status == TaskStatus.OPEN && !hasApplied,
                                    modifier = Modifier.weight(0.5f).height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasApplied) Color.Gray else Color(0xFF4CAF50),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = if (hasApplied) "Applied" else if (task.type == com.example.taskgo.data.model.TaskType.SERVICE) "Request Service" else "Accept Request",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Image Section
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    if (task.images.isNotEmpty()) {
                         AsyncImage(
                            model = task.images.first(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(utmMaroon.copy(alpha = 0.1f), utmMaroon.copy(alpha = 0.3f)))),
                            contentAlignment = Alignment.Center
                        ) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = utmMaroon.copy(alpha = 0.5f)
                                )
                                Text("No Images Provided", color = utmMaroon.copy(alpha = 0.5f))
                             }
                        }
                    }

                    // Floating Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    // Title and Category
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = utmMaroon.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = task.category.name.replace("_", " "),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = utmMaroon,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = if (task.type == com.example.taskgo.data.model.TaskType.REQUEST) Color(0xFF2196F3).copy(alpha = 0.1f) else Color(0xFFFF9800).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = task.type.name,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (task.type == com.example.taskgo.data.model.TaskType.REQUEST) Color(0xFF2196F3) else Color(0xFFFF9800),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        // Status Badge
                        Surface(
                            color = when(task.status) {
                                TaskStatus.OPEN -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                else -> Color.Gray.copy(alpha = 0.1f)
                            },
                            shape = CircleShape
                        ) {
                            Text(
                                text = task.status.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when(task.status) {
                                    TaskStatus.OPEN -> Color(0xFF4CAF50)
                                    else -> Color.Gray
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Key Info Row (Payment & Deadline)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoCard(
                            label = "Payment",
                            value = if (task.paymentAmount > 0) "RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)}" else "Flexible",
                            icon = Icons.Default.Payments,
                            color = utmMaroon,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        InfoCard(
                            label = "Deadline",
                            value = task.deadline.ifBlank { "None" },
                            icon = Icons.Default.Timer,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Description", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))

                    // Location Section
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = utmMaroon)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Location", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = task.campus, fontWeight = FontWeight.Bold, color = utmMaroon)
                            Text(text = task.address, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Requester Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color.LightGray) {
                             Box(contentAlignment = Alignment.Center) {
                                 Icon(Icons.Default.Person, contentDescription = null)
                             }
                         }
                         Spacer(modifier = Modifier.width(12.dp))
                         Column {
                             Text(if (isRequester) "Posted by You" else "Requested by", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                             Text(if (isRequester) currentUser?.name ?: "You" else "User ID: ${task.requesterId}", fontWeight = FontWeight.Bold)
                         }
                    }

                    if (isRequester && task.status == TaskStatus.OPEN) {
                        // ... existing interested runners logic ...
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Interested Runners (${interestedRunners.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (interestedRunners.isEmpty()) {
                            Text("No applicants yet. Share your task to get help!", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            interestedRunners.forEach { runner ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = utmMaroon.copy(alpha = 0.1f)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(runner.name.take(1), color = utmMaroon, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(runner.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFCC00), modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("${runner.rating}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                }
                                            }
                                        }
                                        Button(
                                            onClick = { taskViewModel.assignRunner(task.id, runner.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = utmMaroon)
                                        ) {
                                            Text("Assign", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isRequester && task.status == TaskStatus.WAITING_VERIFICATION) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Verification Required", fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                                Text("The runner has marked this task as complete. Please verify that you have received the service/payment before confirming.", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showConfirmCompleteDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Text("Confirm Completion")
                                }
                            }
                        }
                    } else if (task.status == TaskStatus.ASSIGNED || task.status == TaskStatus.COMPLETED || task.status == TaskStatus.WAITING_VERIFICATION) {
                         Spacer(modifier = Modifier.height(32.dp))
                         Text("Assigned Runner", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                         Spacer(modifier = Modifier.height(8.dp))
                         Text("User ID: ${task.runnerId}", color = Color.DarkGray)
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (showConfirmCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCompleteDialog = false },
            title = { Text("Confirm Completion") },
            text = { Text("Are you sure you want to mark this task as complete? Please ensure you have received the payment or service first.") },
            confirmButton = {
                Button(onClick = { 
                    taskViewModel.updateTask(task.copy(status = TaskStatus.COMPLETED))
                    showConfirmCompleteDialog = false
                }) {
                    Text("Yes, Complete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCompleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InfoCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
