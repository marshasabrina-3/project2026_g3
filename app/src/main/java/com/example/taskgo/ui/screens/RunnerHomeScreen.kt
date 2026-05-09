package com.example.taskgo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnerHomeScreen(taskViewModel: TaskViewModel) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val availableTasks = tasks.filter { it.status == TaskStatus.OPEN }
    
    // Mock runner ID
    val currentRunnerId = "runner_id_456"

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TaskGO - Marketplace") })
        }
    ) { padding ->
        if (availableTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No tasks available in the marketplace.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableTasks) { task ->
                    AvailableTaskItem(task, onAccept = {
                        taskViewModel.applyForTask(task.id, currentRunnerId)
                    })
                }
            }
        }
    }
}

@Composable
fun AvailableTaskItem(task: Task, onAccept: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                Text(text = "RM ${String.format("%.2f", task.paymentAmount)}", color = MaterialTheme.colorScheme.primary)
            }
            Text(text = "Location: ${task.location}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = task.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAccept, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) {
                Text("Accept Task")
            }
        }
    }
}
