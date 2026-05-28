package com.example.taskgo.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskgo.data.model.Report
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskCategory
import com.example.taskgo.data.model.TaskType
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.data.model.PaymentStatus
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import java.util.Locale


fun decodeBase64ToImageBitmap(base64Str: String?): ImageBitmap? {
    if (base64Str.isNullOrBlank()) return null
    return try {
        val cleanString = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
        val decodedBytes = Base64.decode(cleanString, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    taskViewModel: TaskViewModel,
    userViewModel: UserViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showActivityReport by remember { mutableStateOf(false) } // TG-US28 state
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskToViewReports by remember { mutableStateOf<Task?>(null) }
    var taskToViewProofs by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    val allTasksByModel by taskViewModel.allTasks.collectAsState()
    val allReportsByModel by taskViewModel.allReports.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                TopAppBar(
                    title = { Text("TaskGO Admin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    actions = {
                        // TG-US28: Generate Task Activity Report Action Button
                        IconButton(onClick = { showActivityReport = true }) {
                            Icon(Icons.Default.Analytics, contentDescription = "System Analytics Summary", tint = Color(0xFF800000))
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.Gray)
                        }
                        if (selectedTab == 0) {
                            IconButton(onClick = { showCreateDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFFF5F5F5),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Tasks") },
                        label = { Text("Tasks") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Report, contentDescription = "Reports") },
                        label = { Text("Reports") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Users") },
                        label = { Text("Users") }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> {
                        AdminTasksList(
                            taskViewModel = taskViewModel,
                            onEditClick = { taskToEdit = it },
                            onDeleteClick = { taskToDelete = it },
                            onReportsClick = { taskToViewReports = it },
                            onProofsClick = { taskToViewProofs = it }
                        )
                    }
                    1 -> {
                        AdminReportsList(taskViewModel = taskViewModel)
                    }
                    2 -> {
                        AdminUserManagementScreen(userViewModel = userViewModel)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AdminCreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, amount ->
                taskViewModel.addTask(
                    title = title,
                    description = desc,
                    category = TaskCategory.GENERAL,
                    type = TaskType.REQUEST,
                    campus = "UTMKL",
                    address = "Admin HQ",
                    deadline = "None",
                    paymentAmount = amount.toDoubleOrNull() ?: 0.0,
                    requesterId = "Admin",
                    requesterName = "TaskGO Admin"
                )
                showCreateDialog = false
            }
        )
    }

    if (taskToEdit != null) {
        AdminEditTaskDialog(
            task = taskToEdit!!,
            onDismiss = { taskToEdit = null },
            onSave = { updatedTask ->
                taskViewModel.updateTask(updatedTask)
                taskToEdit = null
            }
        )
    }

    // TG-US24: View detailed dispute issue reports matched with specific multi-proof evidence
    if (taskToViewReports != null) {
        val filteredReports = allReportsByModel.filter { it.taskId == taskToViewReports?.id }

        AdminTaskReportsDialog(
            task = taskToViewReports!!,
            reports = filteredReports,
            onDismiss = { taskToViewReports = null }
        )
    }

    // Ticket #169: Proof verification overlay dialog matching workflow state parameters
    if (taskToViewProofs != null) {
        AdminTaskProofsDialog(
            task = taskToViewProofs!!,
            onDismiss = { taskToViewProofs = null }
        )
    }

    // TG-US28: Show Analytical System Reports Engine View Overlay
    if (showActivityReport) {
        AdminActivityReportDialog(
            tasks = allTasksByModel,
            reports = allReportsByModel,
            onDismiss = { showActivityReport = false }
        )
    }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            containerColor = Color.White,
            title = { Text(text = "Confirm Task Removal", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${taskToDelete!!.title}\"? This will flag its status as removed and hide it across all student streams.",
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val softDeletedTask = taskToDelete!!.copy(status = TaskStatus.REMOVED)
                        taskViewModel.updateTask(softDeletedTask)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFBA1A1A),
                        contentColor = Color.White
                    )
                ) {
                    Text("Remove Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}


@Composable
fun AdminTasksList(
    taskViewModel: TaskViewModel,
    onEditClick: (Task) -> Unit,
    onDeleteClick: (Task) -> Unit,
    onReportsClick: (Task) -> Unit,
    onProofsClick: (Task) -> Unit
) {
    val tasks by taskViewModel.allTasks.collectAsState()

    var currentStatusFilter by remember { mutableStateOf("ALL") }
    val filterOptions = listOf("ALL", "OPEN", "PENDING_APPROVAL", "ASSIGNED", "WAITING_VERIFICATION", "COMPLETED", "CANCELLED", "REMOVED")

    val filteredTasks = remember(tasks, currentStatusFilter) {
        if (currentStatusFilter == "ALL") {
            tasks
        } else {
            tasks.filter { task ->
                val statusStr = task.status.toString().uppercase(Locale.ROOT)
                statusStr == currentStatusFilter
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = filterOptions.indexOf(currentStatusFilter).coerceAtLeast(0),
            containerColor = Color.White,
            edgePadding = 16.dp,
            divider = {}
        ) {
            filterOptions.forEach { statusOption ->
                Tab(
                    selected = currentStatusFilter == statusOption,
                    onClick = { currentStatusFilter = statusOption },
                    text = {
                        Text(
                            text = statusOption.replace("_", " "),
                            fontWeight = if (currentStatusFilter == statusOption) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    selectedContentColor = Color(0xFF800000),
                    unselectedContentColor = Color.Gray
                )
            }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

        if (filteredTasks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No tasks match this status filter.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks) { task ->
                    AdminTaskItem(
                        task = task,
                        onEdit = { onEditClick(task) },
                        onDelete = { onDeleteClick(task) },
                        onViewReports = { onReportsClick(task) },
                        onViewProofs = { onProofsClick(task) }
                    )
                }
            }
        }
    }
}


@Composable
fun AdminTaskItem(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewReports: () -> Unit,
    onViewProofs: () -> Unit
) {
    val isAdminTask = task.requesterId == "Admin"
    val utmMaroon = Color(0xFF800000)

    val normalizedStatus = task.status.toString().uppercase(Locale.ROOT)

    val (statusBg, statusText) = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        TaskStatus.CANCELLED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        TaskStatus.REMOVED -> Color(0xFFEEEEEE) to Color(0xFF616161)
        TaskStatus.ASSIGNED -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        TaskStatus.WAITING_VERIFICATION -> Color(0xFFEDE7F6) to Color(0xFF4527A0)
        else -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isAdminTask) BorderStroke(1.5.dp, utmMaroon) else BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column {
            if (isAdminTask) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(utmMaroon).padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("OFFICIAL ADMIN POST", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = task.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF212121), modifier = Modifier.weight(1f))
                    Text(text = "RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)}", color = utmMaroon, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = statusBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(end = 8.dp)) {
                        Text(text = normalizedStatus.replace("_", " "), color = statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    val safeId = task.id
                    val displayId = if (safeId.length >= 8) safeId.take(8).uppercase(Locale.ROOT) else safeId.uppercase(Locale.ROOT)
                    Text(text = "Ref: $displayId...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF616161),
                    maxLines = 3,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F0F0), shape = RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Requested By: ${task.requesterName.ifBlank { task.requesterId }}", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                    Text(text = "Assigned Runner: ${task.runnerName ?: task.runnerId ?: "None assigned yet"}", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF5F5F5))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onViewReports, contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Reports", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = onViewProofs, contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Proofs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onEdit,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text("Edit", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = onDelete,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                        ) {
                            Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AdminTaskProofsDialog(task: Task, onDismiss: () -> Unit) {
    val completionBitmap = remember(task.completionProof) { decodeBase64ToImageBitmap(task.completionProof) }
    val paymentBitmap = remember(task.paymentProof) { decodeBase64ToImageBitmap(task.paymentProof) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Task Workflow Receipts", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Completion Proof Image:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)

                if (completionBitmap != null) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Image(
                            bitmap = completionBitmap,
                            contentDescription = "Runner Completion Proof",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PendingActions, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No completion proof uploaded yet", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                Text("Payment Status & Verification:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)

                val (badgeBg, badgeText, statusLabel) = when (task.paymentStatus) {
                    PaymentStatus.PAID -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "PAID")
                    PaymentStatus.DISPUTED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "DISPUTED")
                    PaymentStatus.PENDING -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "PENDING")
                }

                Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = "$statusLabel (RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)})",
                        color = badgeText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (paymentBitmap != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Payment Receipt Attachment:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    ) {
                        Image(
                            bitmap = paymentBitmap,
                            contentDescription = "User Payment Receipt",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", fontWeight = FontWeight.Bold, color = Color(0xFF800000))
            }
        }
    )
}


@Composable
fun AdminReportsList(taskViewModel: TaskViewModel) {
    val reports by taskViewModel.allReports.collectAsState()

    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Report, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No reports filed yet.", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reports) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Task ID: ${report.taskId}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Reporter: ${report.reporterId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(report.description, color = Color.Black)
                        Text("Status: ${report.status?.toString()?.uppercase(Locale.ROOT) ?: "PENDING"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}


// TG-US24: Full implementation for viewing issue reports alongside contextual task data and multi-proof evidence
@Composable
fun AdminTaskReportsDialog(
    task: Task,
    reports: List<Report>,
    onDismiss: () -> Unit
) {
    val runnerCompletionBitmap = remember(task.completionProof) { decodeBase64ToImageBitmap(task.completionProof) }
    val requesterPaymentBitmap = remember(task.paymentProof) { decodeBase64ToImageBitmap(task.paymentProof) }
    val sortedReports = remember(reports) { reports.sortedByDescending { it.timestamp } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        title = {
            Column {
                Text("Dispute & Issue Investigation", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Text("Task Ref ID: ${task.id.take(8).uppercase(Locale.ROOT)}", fontSize = 12.sp, color = Color.Gray)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section A: Task Details Context Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Task: ${task.title}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                        Text("Description: ${task.description}", fontSize = 12.sp, color = Color.DarkGray)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE0E0E0))
                        Text("Requester: ${task.requesterName.ifBlank { task.requesterId }}", fontSize = 12.sp, color = Color.Black)
                        Text("Runner: ${task.runnerName ?: task.runnerId ?: "Unassigned"}", fontSize = 12.sp, color = Color.Black)
                    }
                }

                // Section B: List of Filed Complaints
                Text("Filed Grievances / Complaints:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                if (sortedReports.isEmpty()) {
                    Text("No formal logs attached to this item.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    sortedReports.forEach { report ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFFBF0), shape = RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, Color(0xFFFFE0B2)), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("By User: ${report.reporterId}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                Text(text = report.status?.toString() ?: "PENDING", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = report.description, fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Section C: Runner Delivery Verification Evidence
                Text("Evidence 1: Runner Completion Proof", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                if (runnerCompletionBitmap != null) {
                    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        Image(bitmap = runnerCompletionBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                } else {
                    Text("⚠️ No completion image attached by runner.", fontSize = 12.sp, color = Color(0xFFBA1A1A), fontWeight = FontWeight.Medium)
                }

                // Section D: Requester Financial Verification Evidence
                Text("Evidence 2: Requester Payment Status", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current Payment Status:", fontSize = 12.sp, color = Color.Gray)
                    Surface(
                        color = if (task.paymentStatus == PaymentStatus.PAID) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = task.paymentStatus.toString(),
                            color = if (task.paymentStatus == PaymentStatus.PAID) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (requesterPaymentBitmap != null) {
                    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        Image(bitmap = requesterPaymentBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                } else {
                    Text("⚠️ No transaction slip uploaded by requester.", fontSize = 12.sp, color = Color(0xFFBA1A1A), fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))) {
                Text("Close File", color = Color.White)
            }
        }
    )
}


// TG-US28: Functional Activity Report Summary Dialog computing total data fields in structural metrics card grid
@Composable
fun AdminActivityReportDialog(
    tasks: List<Task>,
    reports: List<Report>,
    onDismiss: () -> Unit
) {
    val totalPostedTasks = tasks.size
    val completedTasksCount = tasks.count { it.status == TaskStatus.COMPLETED }
    val cancelledTasksCount = tasks.count { it.status == TaskStatus.CANCELLED }
    val totalReportedIssues = reports.size
    val totalPayoutProcessed = tasks.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.paymentAmount }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF800000))
                Text("System Task Activity Report", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Generated Summary Summary:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricRow(label = "Total Posted Tasks", value = totalPostedTasks.toString(), color = Color.Black)
                    MetricRow(label = "Completed Workflows", value = completedTasksCount.toString(), color = Color(0xFF2E7D32))
                    MetricRow(label = "Cancelled Assignments", value = cancelledTasksCount.toString(), color = Color(0xFFBA1A1A))
                    MetricRow(label = "System Disputes/Reports Filed", value = totalReportedIssues.toString(), color = Color(0xFFF57F17))
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Paid Volume:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.DarkGray)
                        Text(
                            text = "RM ${String.format(Locale.getDefault(), "%.2f", totalPayoutProcessed)}",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF800000)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss Report", fontWeight = FontWeight.Bold, color = Color(0xFF800000))
            }
        }
    )
}

@Composable
fun MetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.DarkGray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreateTaskDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("New Admin Task", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (RM)") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, desc, amount) }) { Text("Post as Admin") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditTaskDialog(task: Task, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description) }
    var amount by remember { mutableStateOf(task.paymentAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Edit Task Details", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (RM)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(task.copy(title = title, description = desc, paymentAmount = amount.toDoubleOrNull() ?: task.paymentAmount))
            }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}