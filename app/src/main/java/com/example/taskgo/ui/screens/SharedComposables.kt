package com.example.taskgo.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import coil.compose.AsyncImage
import com.example.taskgo.data.model.*
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import com.example.taskgo.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NameWithRating(name: String, rating: Double, modifier: Modifier = Modifier, fontSize: TextUnit = 14.sp, color: Color = MaterialTheme.colorScheme.onSurface, fontWeight: FontWeight = FontWeight.Bold) {
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            shape = CircleShape
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(10.dp), tint = Color(0xFFFFB300))
                Text(
                    text = " %.1f".format(Locale.getDefault(), rating),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(task: Task, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description) }
    var address by remember { mutableStateOf(task.address) }
    var deadline by remember { mutableStateOf(task.deadline) }
    var amount by remember { mutableStateOf(task.paymentAmount.toString()) }
    var selectedCategory by remember { mutableStateOf(task.category) }
    var campus by remember { mutableStateOf(task.campus) }

    // Date/Time picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Update Task", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                
                Column {
                    Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TaskCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { campus = "UTMKL" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (campus == "UTMKL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (campus == "UTMKL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("UTMKL", fontSize = 10.sp) }
                    Button(onClick = { campus = "UTMJB" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (campus == "UTMJB") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (campus == "UTMJB") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("UTMJB", fontSize = 10.sp) }
                }

                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Price (RM)") }, modifier = Modifier.fillMaxWidth())

                // Deadline field with calendar picker
                Text("Deadline", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Deadline Date: ${deadline.substringBefore(" ").ifBlank { "Not Set" }}")
                }

                if (deadline.isNotBlank() && !deadline.contains("No deadline")) {
                    Button(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set Time: ${deadline.substringAfter(" ").ifBlank { "Not Set" }}")
                    }
                }

                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        },
        confirmButton = { 
            Button(onClick = { 
                onSave(task.copy(
                    title = title, 
                    description = desc, 
                    address = address,
                    deadline = deadline,
                    category = selectedCategory,
                    campus = campus,
                    paymentAmount = amount.toDoubleOrNull() ?: task.paymentAmount
                )) 
            }) { Text("Update") } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = java.time.LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                        val existingTime = deadline.substringAfter(" ").ifBlank { "00:00" }
                        deadline = "$date $existingTime"
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val h = timePickerState.hour.toString().padStart(2, '0')
                    val m = timePickerState.minute.toString().padStart(2, '0')
                    val date = deadline.substringBefore(" ").ifBlank { java.time.LocalDate.now().toString() }
                    deadline = "$date $h:$m"
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
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
    modifier: Modifier = Modifier,
    onEdit: (Task) -> Unit = {},
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUser by userViewModel.currentUser.collectAsState()
    val isRequester = currentUser?.id == task.requesterId
    val utmMaroon = MaterialTheme.colorScheme.primary

    var interestedRunners by remember { mutableStateOf<List<User>>(emptyList()) }
    var showConfirmCompleteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }

    val proofPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            taskViewModel.markTaskAsFinished(task.id, uri)
        }
    }

    val paymentProofPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            taskViewModel.updatePaymentStatus(task.id, PaymentStatus.PAID, uri)
        }
    }

    LaunchedEffect(task.interestedRunnerIds) {
        if (isRequester) {
            interestedRunners = taskViewModel.getInterestedRunners(task.interestedRunnerIds)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!isRequester && task.status == TaskStatus.OPEN) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 16.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp).padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedIconButton(
                                onClick = { showReportDialog = true },
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
                                colors = ButtonDefaults.buttonColors(containerColor = if (hasApplied) MaterialTheme.colorScheme.outlineVariant else Color(0xFF43A047))
                            ) {
                                Text(if (hasApplied) "Applied" else "Accept", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (!isRequester && task.runnerId == currentUser?.id) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 16.dp,
                        color = MaterialTheme.colorScheme.surface,
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
                                    onClick = { proofPickerLauncher.launch("image/*") },
                                    modifier = Modifier.height(54.dp).weight(0.6f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                    shape = RoundedCornerShape(16.dp)
                                 ) {
                                     Icon(Icons.Default.FileUpload, null)
                                     Text(" Mark Finished", fontWeight = FontWeight.Bold)
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

                    // Requester Menu (Three Dots)
                    if (isRequester && task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED) {
                        var showMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)
                            ) {
                                Icon(Icons.Default.MoreVert, null, tint = Color.White)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                if (task.status == TaskStatus.OPEN) {
                                    DropdownMenuItem(
                                        text = { Text("Update Task") },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                                        onClick = {
                                            showMenu = false
                                            onEdit(task)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Cancel Task", color = Color.Red) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Surface(color = if (task.status == TaskStatus.OPEN) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                            Text(text = task.status.name, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (task.status == TaskStatus.OPEN) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        DetailBadge(text = task.category.name, color = utmMaroon)
                        DetailBadge(text = task.type.name, color = if (task.type == TaskType.REQUEST) Color(0xFF2196F3) else Color(0xFFFF9800))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailInfoCard(label = "Payment", value = "RM %.2f".format(Locale.getDefault(), task.paymentAmount), icon = Icons.Default.Payments, color = utmMaroon, modifier = Modifier.weight(1f))
                        DetailInfoCard(label = "Deadline", value = task.deadline.ifBlank { "None" }, icon = Icons.Default.Timer, color = Color(0xFFFF9800), modifier = Modifier.weight(1f))
                    }

                    val postDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(task.timestamp))
                    Text(text = "Posted on: $postDate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    
                    task.completionTimestamp?.let {
                        val compDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                        Text(text = "Completed on: $compDate", fontSize = 12.sp, color = Color(0xFF43A047), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }

                    Text("Description", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 32.dp), color = utmMaroon)
                    Text(text = task.description, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = utmMaroon, modifier = Modifier.size(24.dp))
                        Text(" Location", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        if (task.latitude != null && task.longitude != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { showMapDialog = true }) {
                                Icon(Icons.Default.Map, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Map", fontSize = 12.sp)
                            }
                        }
                    }
                    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth().clickable {
                        if (task.latitude != null && task.longitude != null) {
                            showMapDialog = true
                        }
                    }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = task.campus, fontWeight = FontWeight.Bold, color = utmMaroon)
                            Text(text = task.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (task.latitude != null && task.longitude != null) {
                                Text("📍 GPS coordinates attached", fontSize = 10.sp, color = Color(0xFF43A047), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Completion Proof Section
                    if (task.completionProof != null || (isRequester && task.status == TaskStatus.WAITING_VERIFICATION)) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TaskAlt, null, tint = Color(0xFF43A047), modifier = Modifier.size(24.dp))
                            Text(" Completion Proof", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) Color(0xFF1B2E1B) else Color(0xFFF1F8E9)), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (task.completionProof != null) {
                                    val proofBytes = remember(task.completionProof) { ImageUtils.decodeBase64ToByteArray(task.completionProof) }
                                    AsyncImage(model = proofBytes, contentDescription = "Proof", modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                } else {
                                    Text("Runner has not uploaded proof yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Payment Status Section
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, null, tint = utmMaroon, modifier = Modifier.size(24.dp))
                        Text(" Payment Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = when(task.paymentStatus) {
                                    PaymentStatus.PAID -> Color(0xFFE8F5E9)
                                    PaymentStatus.DISPUTED -> Color(0xFFFFEBEE)
                                    else -> Color(0xFFFFF3E0)
                                }, shape = CircleShape) {
                                    Text(text = task.paymentStatus.name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = when(task.paymentStatus) {
                                        PaymentStatus.PAID -> Color(0xFF2E7D32)
                                        PaymentStatus.DISPUTED -> Color(0xFFD32F2F)
                                        else -> Color(0xFFE65100)
                                    })
                                }
                                
                                if (isRequester && task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(onClick = { taskViewModel.updatePaymentStatus(task.id, PaymentStatus.PAID) }) {
                                            Icon(Icons.Default.CheckCircle, "Paid", tint = Color(0xFF43A047))
                                        }
                                        IconButton(onClick = { taskViewModel.updatePaymentStatus(task.id, PaymentStatus.DISPUTED) }) {
                                            Icon(Icons.Default.Gavel, "Dispute", tint = Color(0xFFD32F2F))
                                        }
                                        IconButton(onClick = { paymentProofPickerLauncher.launch("image/*") }) {
                                            Icon(Icons.Default.FileUpload, "Upload Proof", tint = utmMaroon)
                                        }
                                    }
                                }
                            }
                            
                            if (task.paymentProof != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Payment Proof:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val payProofBytes = remember(task.paymentProof) { ImageUtils.decodeBase64ToByteArray(task.paymentProof) }
                                AsyncImage(model = payProofBytes, contentDescription = "Payment Proof", modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 4.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            }
                            
                            if (!isRequester && task.runnerId == currentUser?.id) {
                                Button(
                                    onClick = { onChat(task.requesterId, task.title) },
                                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = utmMaroon),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(16.dp))
                                    Text(" Chat about Payment", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

// Requester Info Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // ⚡ Determine whose profile to target based on Task Type
                                val targetUserId = if (task.type == TaskType.REQUEST) {
                                    task.requesterId
                                } else {
                                    task.runnerId ?: ""
                                }

                                // ⚡ Fire navigation lambda callback if the ID exists
                                if (targetUserId.isNotEmpty()) {
                                    onUserClick(targetUserId)
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = utmMaroon) }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val labelText = if (isRequester) {
                                    if (task.type == TaskType.REQUEST) "Requested by You" else "Service Provided by You"
                                } else {
                                    if (task.type == TaskType.REQUEST) "Posted by" else "Provider"
                                }
                                Text(labelText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                NameWithRating(
                                    name = if (isRequester) currentUser?.name ?: "You" else {
                                        if (task.type == TaskType.REQUEST) task.requesterName.ifBlank { "User" }
                                        else (task.runnerName ?: "User")
                                    },
                                    rating = taskViewModel.getUserRating(if (task.type == TaskType.REQUEST) task.requesterId else (task.runnerId ?: "")),
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    if (isRequester && task.status == TaskStatus.OPEN) {
                        Spacer(modifier = Modifier.height(40.dp))
                        val interestedLabel = if (task.type == TaskType.REQUEST) "Interested Runners" else "Interested Requesters"
                        Text("$interestedLabel (${interestedRunners.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        if (interestedRunners.isEmpty()) {
                            Text("Waiting for applications...", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            interestedRunners.forEach { runner ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                                Box(contentAlignment = Alignment.Center) { Text(runner.name.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                                                Text(if (task.type == TaskType.REQUEST) "Assign" else "Accept", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
else if (isRequester && task.status == TaskStatus.WAITING_VERIFICATION) {
                        Card(modifier = Modifier.padding(top = 40.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) Color(0xFF332F1D) else Color(0xFFFFF9C4)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFFFD54F))) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, null, tint = Color(0xFFF57F17))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verification Required", fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                                }
                                Text("The runner has finished. Please verify payment/service received.", modifier = Modifier.padding(top = 8.dp), fontSize = 13.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                Button(onClick = { showConfirmCompleteDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)), shape = RoundedCornerShape(12.dp)) {
                                    Text("Confirm Completion")
                                }
                            }
                        }
                    } else if (task.runnerId != null) {
                         Spacer(modifier = Modifier.height(40.dp))
                         Text("Assigned Runner", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                         Card(
                             modifier = Modifier
                                 .padding(top = 12.dp)
                                 .fillMaxWidth()
                                 .clickable { onUserClick(task.runnerId) },
                             shape = RoundedCornerShape(16.dp),
                             colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) Color(0xFF1A2A3A) else Color(0xFFE3F2FD))
                         ) {
                             Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                 Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                                     Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountCircle, null, tint = Color(0xFF1976D2)) }
                                 }
                                 Spacer(modifier = Modifier.width(16.dp))
                                 Column {
                                     Text("Completed by", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Cancellation?") },
            text = { Text("Are you sure you want to cancel this task? This action cannot be undone.") },
            confirmButton = { 
                Button(onClick = { 
                    taskViewModel.cancelTask(task.id)
                    showDeleteConfirmDialog = false
                    onBack()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    if (showConfirmCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCompleteDialog = false },
            title = { Text("Confirm Completion", fontWeight = FontWeight.Bold) },
            text = { Text("Marking this task as complete is permanent.") },
            confirmButton = { 
                Button(onClick = { 
                    taskViewModel.completeTask(task.id)
                    showConfirmCompleteDialog = false
                    showReviewDialog = true
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) { Text("Confirm") } 
            },
            dismissButton = { TextButton(onClick = { showConfirmCompleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showReviewDialog) {
        var rating by remember { mutableIntStateOf(5) }
        var comment by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Rate your Runner", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(5) { i ->
                            IconButton(onClick = { rating = i + 1 }) {
                                Icon(
                                    imageVector = if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (i < rating) Color(0xFFFFB300) else Color.Gray
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Write a review (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    taskViewModel.addReview(Review(
                        taskId = task.id,
                        reviewerId = currentUser?.id ?: "",
                        revieweeId = task.runnerId ?: "",
                        rating = rating,
                        comment = comment
                    ))
                    showReviewDialog = false
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showReviewDialog = false }) { Text("Skip") } }
        )
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("Fraud") }
        var description by remember { mutableStateOf("") }
        val reasons = listOf("Fraud", "Incomplete Work", "Unprofessional Behavior", "Payment Issue", "Other")
        
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Issue", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select a reason:", fontSize = 12.sp, color = Color.Gray)
                    Column {
                        reasons.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { reason = r }) {
                                RadioButton(selected = reason == r, onClick = { reason = r })
                                Text(r, fontSize = 14.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe the issue") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    taskViewModel.addReport(Report(
                        taskId = task.id,
                        reporterId = currentUser?.id ?: "",
                        reportedUserId = if (isRequester) task.runnerId ?: "" else task.requesterId,
                        reason = reason,
                        description = description
                    ))
                    showReportDialog = false
                    Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) { Text("Submit Report") }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }

    if (showMapDialog && task.latitude != null && task.longitude != null) {
        AlertDialog(
            onDismissRequest = { showMapDialog = false },
            title = { Text("Task Location", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp))) {
                        com.google.maps.android.compose.GoogleMap(
                            cameraPositionState = com.google.maps.android.compose.rememberCameraPositionState {
                                position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                                    com.google.android.gms.maps.model.LatLng(task.latitude!!, task.longitude!!), 15f
                                )
                            }
                        ) {
                            com.google.maps.android.compose.Marker(
                                state = com.google.maps.android.compose.MarkerState(
                                    position = com.google.android.gms.maps.model.LatLng(task.latitude!!, task.longitude!!)
                                ),
                                title = task.title
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val uri = android.net.Uri.parse("geo:${task.latitude},${task.longitude}?q=${task.latitude},${task.longitude}(${task.title})")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Navigation, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in External Maps")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMapDialog = false }) { Text("Close") } }
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
