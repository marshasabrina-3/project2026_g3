package com.example.taskgo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskgo.data.model.Report
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskCategory
import com.example.taskgo.data.model.TaskType
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.ui.viewmodel.TaskViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    taskViewModel: TaskViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskToViewReports by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

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
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (selectedTab == 0) {
                    AdminTasksList(
                        taskViewModel = taskViewModel,
                        onEditClick = { taskToEdit = it },
                        onDeleteClick = { taskToDelete = it },
                        onReportsClick = { taskToViewReports = it }
                    )
                } else {
                    AdminReportsList(taskViewModel = taskViewModel)
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

    if (taskToViewReports != null) {
        val reports by taskViewModel.allReports.collectAsState()
        val filteredReports = reports.filter { it.taskId == taskToViewReports?.id }

        AdminTaskReportsDialog(
            taskTitle = taskToViewReports?.title ?: "",
            reports = filteredReports,
            onDismiss = { taskToViewReports = null }
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
    onReportsClick: (Task) -> Unit
) {
    val tasks by taskViewModel.allTasks.collectAsState()

    if (tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tasks available.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks) { task ->
                AdminTaskItem(
                    task = task,
                    onEdit = { onEditClick(task) },
                    onDelete = { onDeleteClick(task) },
                    onViewReports = { onReportsClick(task) }
                )
            }
        }
    }
}

@Composable
fun AdminTaskItem(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewReports: () -> Unit
) {
    val isAdminTask = task.requesterId == "Admin"
    val utmMaroon = Color(0xFF800000)

    val rawStatusStr = task.status?.toString() ?: "PENDING"
    val normalizedStatus = if (rawStatusStr.isBlank()) {
        "PENDING"
    } else {
        rawStatusStr.uppercase(Locale.ROOT)
    }

    val (statusBg, statusText) = when (normalizedStatus) {
        "COMPLETED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "CANCELLED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        "REMOVED" -> Color(0xFFEEEEEE) to Color(0xFF616161)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(utmMaroon)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "OFFICIAL ADMIN POST",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF212121),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)}",
                        color = utmMaroon,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = normalizedStatus,
                            color = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    val safeId = task.id ?: ""
                    val displayId = if (safeId.length >= 8) safeId.take(8).uppercase(Locale.ROOT) else safeId.uppercase(Locale.ROOT)
                    Text(
                        text = "Ref: $displayId...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF616161),
                    maxLines = 3,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF5F5F5))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onViewReports,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Reports", fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onEdit,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, Color.LightGray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onDelete,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFBA1A1A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
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

                        val rawReportStatusStr = report.status?.toString() ?: "PENDING"
                        val reportStatus = if (rawReportStatusStr.isBlank()) "PENDING" else rawReportStatusStr.uppercase(Locale.ROOT)
                        Text("Status: $reportStatus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTaskReportsDialog(taskTitle: String, reports: List<Report>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Reports for: $taskTitle", color = Color.Black, fontSize = 18.sp) },
        text = {
            if (reports.isEmpty()) {
                Text("No reports for this task.", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(reports) { report ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Reporter: ${report.reporterId}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(report.description, color = Color.Black)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Gray.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
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