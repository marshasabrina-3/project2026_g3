package com.example.taskgo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                        onEdit = { taskToEdit = it },
                        onViewReports = { taskToViewReports = it }
                    )
                } else {
                    AdminReportsList(taskViewModel)
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateAdminTaskDialog(
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
        EditTaskDialog(
            task = taskToEdit!!,
            onDismiss = { taskToEdit = null },
            onSave = { updatedTask: Task ->
                taskViewModel.updateTask(updatedTask)
                taskToEdit = null
            }
        )
    }

    if (taskToViewReports != null) {
        val reports by taskViewModel.allReports.collectAsState()
        val filteredReports = reports.filter { it.taskId == taskToViewReports?.id }
        
        TaskReportsDialog(
            taskTitle = taskToViewReports?.title ?: "",
            reports = filteredReports,
            onDismiss = { taskToViewReports = null }
        )
    }
}

@Composable
fun AdminTasksList(
    taskViewModel: TaskViewModel,
    onEdit: (Task) -> Unit,
    onViewReports: (Task) -> Unit
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
                    onEdit = { onEdit(task) },
                    onDelete = { taskViewModel.deleteTask(task.id) },
                    onViewReports = { onViewReports(task) }
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
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        border = if (isAdminTask) BorderStroke(1.dp, Color(0xFF4CAF50)) else null
    ) {
        Column {
            if (isAdminTask) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF4CAF50))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Posted by Admin",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${task.id} | Status: ${task.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onViewReports) {
                        Text("View Reports", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = onEdit) {
                        Text("Edit", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679), contentColor = Color.White)
                    ) {
                        Text("Remove")
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
                        Text("Status: ${report.status}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskReportsDialog(taskTitle: String, reports: List<Report>, onDismiss: () -> Unit) {
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
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdminTaskDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("New Admin Task", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") }
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (RM)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, desc, amount) }) {
                Text("Post as Admin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

