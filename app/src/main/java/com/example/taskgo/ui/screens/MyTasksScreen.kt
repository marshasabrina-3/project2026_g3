package com.example.taskgo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.data.model.TaskType
import com.example.taskgo.ui.viewmodel.TaskViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen(
    taskViewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val allTasks by taskViewModel.allTasks.collectAsState()
    val myTasks = allTasks.filter { it.requesterId == "user_123" } // Mocked current user ID
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Requests", "Services")

    val requestTasks = myTasks.filter { it.type == TaskType.REQUEST }
    val serviceTasks = myTasks.filter { it.type == TaskType.SERVICE }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("My Posted Tasks", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                    )
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Black,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, color = if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Gray) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            val currentTasks = if (selectedTab == 0) requestTasks else serviceTasks
            
            if (currentTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No ${tabs[selectedTab].lowercase()} posted yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentTasks) { task ->
                        MyTaskItem(
                            task = task,
                            onEdit = { taskToEdit = task },
                            onCancel = { taskViewModel.cancelTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (taskToEdit != null) {
        EditTaskDialog(
            task = taskToEdit!!,
            onDismiss = { taskToEdit = null },
            onSave = { updatedTask ->
                taskViewModel.updateTask(updatedTask)
                taskToEdit = null
            }
        )
    }
}

@Composable
fun MyTaskItem(task: Task, onEdit: () -> Unit, onCancel: () -> Unit) {
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = task.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(text = "Status: ${task.status}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = task.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, color = Color.White.copy(alpha = 0.8f))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (task.status == TaskStatus.OPEN) {
                    TextButton(onClick = onEdit) {
                        Text("Edit", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showCancelConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679), contentColor = Color.White)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Confirm Cancellation?") },
            text = { Text("Are you sure you want to cancel this task? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    onCancel()
                    showCancelConfirmDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679))) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showCancelConfirmDialog = false }) { Text("Cancel") } }
        )
    }
}
