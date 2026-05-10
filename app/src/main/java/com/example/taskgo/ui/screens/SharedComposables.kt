package com.example.taskgo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import coil.compose.AsyncImage
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.data.model.User
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import com.example.taskgo.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NameWithRating(name: String, rating: Double, modifier: Modifier = Modifier, fontSize: TextUnit = 14.sp, color: Color = Color.Black, fontWeight: FontWeight = FontWeight.Bold) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = name,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Surface(
            color = Color.LightGray.copy(alpha = 0.2f),
            shape = CircleShape
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                Text(
                    text = " %.1f".format(Locale.getDefault(), rating),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun EditTaskDialog(task: Task, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description) }
    var amount by remember { mutableStateOf(task.paymentAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Edit Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(task.copy(title = title, description = desc, paymentAmount = amount.toDoubleOrNull() ?: task.paymentAmount)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
    onChat: (String, String) -> Unit,
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
                if (!isRequester && task.status == TaskStatus.OPEN) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 16.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp).padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedIconButton(
                                onClick = onReport,
                                modifier = Modifier.size(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFE57373))
                            ) {
                                Icon(Icons.Default.Report, contentDescription = "Report", tint = Color(0xFFD32F2F))
                            }

                            Button(
                                onClick = {
                                    if (task.requesterId.isNotEmpty()) onChat(task.requesterId, task.title)
                                },
                                modifier = Modifier.height(54.dp).weight(0.4f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = utmMaroon)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chat", fontWeight = FontWeight.Bold)
                            }

                            val hasApplied = task.interestedRunnerIds.contains(currentUser?.id)
                            Button(
                                onClick = onAccept,
                                enabled = !hasApplied,
                                modifier = Modifier.height(54.dp).weight(0.6f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (hasApplied) Color.Gray else Color(0xFF43A047))
                            ) {
                                Text(if (hasApplied) "Applied" else "Accept", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (!isRequester && task.runnerId == currentUser?.id) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 16.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                             Button(
                                onClick = { 
                                    if (task.requesterId.isNotEmpty()) onChat(task.requesterId, task.title)
                                },
                                modifier = Modifier.height(54.dp).weight(0.4f),
                                colors = ButtonDefaults.buttonColors(containerColor = utmMaroon),
                                shape = RoundedCornerShape(16.dp)
                             ) {
                                 Icon(Icons.AutoMirrored.Filled.Chat, null)
                                 Text(" Chat", fontWeight = FontWeight.Bold)
                             }
                             
                             if (task.status == TaskStatus.ASSIGNED) {
                                 Button(
                                    onClick = { taskViewModel.updateTask(task.copy(status = TaskStatus.WAITING_VERIFICATION)) },
                                    modifier = Modifier.height(54.dp).weight(0.6f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                    shape = RoundedCornerShape(16.dp)
                                 ) {
                                     Text("Mark Finished", fontWeight = FontWeight.Bold)
                                 }
                             } else if (task.status == TaskStatus.WAITING_VERIFICATION) {
                                 Button(onClick = {}, enabled = false, modifier = Modifier.height(54.dp).weight(0.6f), shape = RoundedCornerShape(16.dp)) {
                                     Text("Pending Verification", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                 }
                             }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    if (task.images.isNotEmpty()) {
                        val pagerState = rememberPagerState(pageCount = { task.images.size })
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                            val imageBytes = remember(task.images[index]) { ImageUtils.decodeBase64ToByteArray(task.images[index]) }
                            AsyncImage(model = imageBytes, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        if (task.images.size > 1) {
                            Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(task.images.size) { i ->
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (pagerState.currentPage == i) Color.White else Color.White.copy(0.5f)))
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(utmMaroon.copy(0.1f), utmMaroon.copy(0.3f)))), contentAlignment = Alignment.Center) {
                             Icon(Icons.Default.Image, null, modifier = Modifier.size(60.dp), tint = utmMaroon.copy(0.5f))
                        }
                    }
                    IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).background(Color.Black.copy(0.3f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        Surface(color = if (task.status == TaskStatus.OPEN) Color(0xFFE8F5E9) else Color(0xFFF5F5F5), shape = CircleShape) {
                            Text(text = task.status.name, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (task.status == TaskStatus.OPEN) Color(0xFF2E7D32) else Color.Gray)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        DetailBadge(text = task.category.name, color = utmMaroon)
                        DetailBadge(text = task.type.name, color = if (task.type == com.example.taskgo.data.model.TaskType.REQUEST) Color(0xFF2196F3) else Color(0xFFFF9800))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailInfoCard(label = "Payment", value = "RM %.2f".format(Locale.getDefault(), task.paymentAmount), icon = Icons.Default.Payments, color = utmMaroon, modifier = Modifier.weight(1f))
                        DetailInfoCard(label = "Deadline", value = task.deadline.ifBlank { "None" }, icon = Icons.Default.Timer, color = Color(0xFFFF9800), modifier = Modifier.weight(1f))
                    }

                    val postDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(task.timestamp))
                    Text(text = "Posted on: $postDate", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    
                    task.completionTimestamp?.let {
                        val compDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                        Text(text = "Completed on: $compDate", fontSize = 12.sp, color = Color(0xFF43A047), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }

                    Text("Description", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 32.dp), color = utmMaroon)
                    Text(text = task.description, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyLarge, color = Color.DarkGray)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp), color = Color.LightGray.copy(0.3f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = utmMaroon, modifier = Modifier.size(24.dp))
                        Text(" Location", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = task.campus, fontWeight = FontWeight.Bold, color = utmMaroon)
                            Text(text = task.address, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Requester Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color.White) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = utmMaroon) }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isRequester) "Posted by You" else "Posted by", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                NameWithRating(
                                    name = if (isRequester) currentUser?.name ?: "You" else task.requesterName.ifBlank { "User" },
                                    rating = taskViewModel.getUserRating(task.requesterId),
                                    fontSize = 15.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    if (isRequester && task.status == TaskStatus.OPEN) {
                        Spacer(modifier = Modifier.height(40.dp))
                        Text("Interested Runners (${interestedRunners.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (interestedRunners.isEmpty()) {
                            Text("Waiting for applications...", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            interestedRunners.forEach { runner ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = Color(0xFFF5F5F5)) {
                                                Box(contentAlignment = Alignment.Center) { Text(runner.name.take(1), fontWeight = FontWeight.Bold) }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            NameWithRating(
                                                name = runner.name,
                                                rating = taskViewModel.getUserRating(runner.id),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { onChat(runner.id, "Chat with ${runner.name}") }) {
                                                Icon(Icons.AutoMirrored.Filled.Chat, null, tint = utmMaroon)
                                            }
                                            Button(
                                                onClick = { taskViewModel.assignRunner(task.id, runner.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Assign", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isRequester && task.status == TaskStatus.WAITING_VERIFICATION) {
                        Card(modifier = Modifier.padding(top = 40.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFFFD54F))) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, null, tint = Color(0xFFF57F17))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verification Required", fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                                }
                                Text("The runner has finished. Please verify payment/service received.", modifier = Modifier.padding(top = 8.dp), fontSize = 13.sp)
                                Button(onClick = { showConfirmCompleteDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)), shape = RoundedCornerShape(12.dp)) {
                                    Text("Confirm Completion")
                                }
                            }
                        }
                    } else if (task.runnerId != null) {
                         Spacer(modifier = Modifier.height(40.dp))
                         Text("Assigned Runner", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                         Card(
                             modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                             shape = RoundedCornerShape(16.dp),
                             colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                         ) {
                             Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                 Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color.White) {
                                     Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountCircle, null, tint = Color(0xFF1976D2)) }
                                 }
                                 Spacer(modifier = Modifier.width(16.dp))
                                 Column {
                                     Text("Completed by", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                     NameWithRating(
                                         name = task.runnerName ?: "Runner",
                                         rating = taskViewModel.getUserRating(task.runnerId),
                                         fontWeight = FontWeight.Bold
                                     )
                                     Text("Task is ${task.status.name.lowercase()}", fontSize = 11.sp, color = Color(0xFF1976D2))
                                 }
                             }
                         }
                    }
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }

    if (showConfirmCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCompleteDialog = false },
            title = { Text("Confirm Completion", fontWeight = FontWeight.Bold) },
            text = { Text("Marking this task as complete is permanent.") },
            confirmButton = { Button(onClick = { taskViewModel.completeTask(task.id); showConfirmCompleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { showConfirmCompleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun DetailBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
        Text(text = text.replace("_", " "), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun DetailInfoCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color(0xFFF9F9F9), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}
