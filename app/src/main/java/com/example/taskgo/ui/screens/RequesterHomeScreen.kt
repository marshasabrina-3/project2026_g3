package com.example.taskgo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.ui.viewmodel.TaskViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequesterHomeScreen(
    taskViewModel: TaskViewModel,
    onNavigateToCreateTask: () -> Unit
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    // Mock user ID for now
    val currentUserId = "user_id_123"
    val myTasks = tasks.filter { it.requesterId == currentUserId }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TaskGO - My Requests") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateTask) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        if (myTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tasks posted yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(myTasks) { task ->
                    TaskItem(task = task, taskViewModel = taskViewModel)
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, taskViewModel: TaskViewModel) {
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(text = task.status.name, fontSize = MaterialTheme.typography.labelSmall.fontSize, color = if (task.status == TaskStatus.OPEN) Color(0xFF43A047) else Color.Gray, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = task.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(modifier = Modifier.height(12.dp))

            if (task.status == TaskStatus.OPEN) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { showCancelConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679), contentColor = Color.White),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Cancel Task", fontSize = MaterialTheme.typography.labelMedium.fontSize)
                    }
                }
            }
        }
    }

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Confirm deletion?") },
            text = { Text("Are you sure you want to cancel this task? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    taskViewModel.deleteTask(task.id)
                    showCancelConfirmDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679))) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showCancelConfirmDialog = false }) { Text("Cancel") } }
        )
    }
}
