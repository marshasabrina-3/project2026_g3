package com.example.taskgo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskCategory
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.data.model.TaskType
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskCreateScreen(
    onTaskCreated: (String, String, TaskType, TaskCategory, String, String, Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var taskType by remember { mutableStateOf(TaskType.REQUEST) }
    var category by remember { mutableStateOf(TaskCategory.GENERAL) }
    var location by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var noDeadline by remember { mutableStateOf(false) }
    var hour by remember { mutableStateOf("") }
    var minute by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = taskType == TaskType.REQUEST, onClick = { taskType = TaskType.REQUEST }, label = { Text("Request") })
                FilterChip(selected = taskType == TaskType.SERVICE, onClick = { taskType = TaskType.SERVICE }, label = { Text("Service") })
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text("Category", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (taskType == TaskType.SERVICE) {
                if (noDeadline) {
                    Text("No deadline set")
                } else {
                    Button(onClick = { showDatePicker = true }) {
                        Text(selectedDate?.toString() ?: "Select Date")
                    }
                    if (selectedDate != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showTimePicker = true }) {
                            Text(selectedTime?.toString() ?: "Select Time")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { noDeadline = true; selectedDate = null; selectedTime = null }) {
                    Text("Don't display deadline")
                }
            } else {
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedDate?.toString() ?: "Select Deadline Date")
                }
                if (selectedDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedTime?.toString() ?: "Select Deadline Time")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = paymentAmount,
                onValueChange = { paymentAmount = it },
                label = { Text("Payment Amount (RM)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val amount = paymentAmount.toDoubleOrNull() ?: 0.0
                    val finalDeadline = if (taskType == TaskType.SERVICE) {
                        if (noDeadline) "No deadline" else "${selectedDate} ${selectedTime}"
                    } else {
                        if (selectedDate != null && selectedTime != null) {
                            "${selectedDate} ${selectedTime}"
                        } else {
                            ""
                        }
                    }
                    onTaskCreated(title, description, taskType, category, location, finalDeadline, amount)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && paymentAmount.isNotBlank()
            ) {
                Text("Post Task")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis?.let {
                        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
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

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Hour", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(24) { h ->
                            FilterChip(
                                selected = hour.toIntOrNull() == h,
                                onClick = { hour = h.toString() },
                                label = { Text(h.toString().padStart(2, '0'), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            if ((h + 1) % 6 == 0 && h < 23) {
                                Spacer(modifier = Modifier.width(0.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Minute", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55).forEach { m ->
                            FilterChip(
                                selected = minute.toIntOrNull() == m,
                                onClick = { minute = m.toString() },
                                label = { Text(m.toString().padStart(2, '0'), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val h = hour.ifBlank { "00" }
                    val m = minute.ifBlank { "00" }
                    selectedTime = LocalTime.of(h.toIntOrNull() ?: 0, m.toIntOrNull() ?: 0)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(
    task: Task, // Assume Task is a data class with all relevant fields
    onTaskUpdated: (Task) -> Unit,
    onNavigateBack: () -> Unit
) {
    // State for each field, initialized with the task's current values
    val parts = task.deadline.split(" ")
    val initialSelectedDate = if (parts.size >= 2 && task.deadline != "No deadline") {
        try { LocalDate.parse(parts[0]) } catch (e: Exception) { null }
    } else null
    val initialSelectedTime = if (parts.size >= 2 && task.deadline != "No deadline") {
        try { LocalTime.parse(parts[1]) } catch (e: Exception) { null }
    } else null
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var taskType by remember { mutableStateOf(task.type) }
    var category by remember { mutableStateOf(task.category) }
    var location by remember { mutableStateOf(task.location) }
    var deadline by remember { mutableStateOf(task.deadline) }
    var paymentAmount by remember { mutableStateOf(task.paymentAmount.toString()) }
    var selectedDate by remember { mutableStateOf(initialSelectedDate) }
    var selectedTime by remember { mutableStateOf(initialSelectedTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var noDeadline by remember { mutableStateOf(task.deadline == "No deadline") }
    var hour by remember { mutableStateOf(selectedTime?.hour?.toString() ?: "") }
    var minute by remember { mutableStateOf(selectedTime?.minute?.toString() ?: "") }
    val datePickerState = rememberDatePickerState()

    // Only allow editing if the task is open (placeholder logic)
    val isEditable = task.status == TaskStatus.OPEN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditable
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = isEditable
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = taskType == TaskType.REQUEST, onClick = { if (isEditable) taskType = TaskType.REQUEST }, label = { Text("Request") })
                FilterChip(selected = taskType == TaskType.SERVICE, onClick = { if (isEditable) taskType = TaskType.SERVICE }, label = { Text("Service") })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Category", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { if (isEditable) category = cat },
                        label = { Text(cat.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditable
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (taskType == TaskType.SERVICE) {
                if (noDeadline) {
                    Text("No deadline set")
                } else {
                    Button(onClick = { if (isEditable) showDatePicker = true }, enabled = isEditable) {
                        Text(selectedDate?.toString() ?: "Select Date")
                    }
                    if (selectedDate != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { if (isEditable) showTimePicker = true }, enabled = isEditable) {
                            Text(selectedTime?.toString() ?: "Select Time")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { if (isEditable) { noDeadline = true; selectedDate = null; selectedTime = null } }, enabled = isEditable) {
                    Text("Don't display deadline")
                }
            } else {
                Button(
                    onClick = { if (isEditable) showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditable
                ) {
                    Text(selectedDate?.toString() ?: "Select Deadline Date")
                }
                if (selectedDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { if (isEditable) showTimePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditable
                    ) {
                        Text(selectedTime?.toString() ?: "Select Deadline Time")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = paymentAmount,
                onValueChange = { paymentAmount = it },
                label = { Text("Payment Amount (RM)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditable
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val amount = paymentAmount.toDoubleOrNull() ?: 0.0
                    val finalDeadline = if (taskType == TaskType.SERVICE) {
                        if (noDeadline) "No deadline" else "${selectedDate} ${selectedTime}"
                    } else {
                        if (selectedDate != null && selectedTime != null) {
                            "${selectedDate} ${selectedTime}"
                        } else {
                            ""
                        }
                    }
                    val updatedTask = task.copy(
                        title = title,
                        description = description,
                        type = taskType,
                        category = category,
                        location = location,
                        deadline = finalDeadline,
                        paymentAmount = amount
                    )
                    onTaskUpdated(updatedTask)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditable && title.isNotBlank() && paymentAmount.isNotBlank()
            ) {
                Text("Save Changes")
            }
            if (!isEditable) {
                Text("Only open tasks can be edited.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis?.let {
                        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
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
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Hour", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(24) { h ->
                            FilterChip(
                                selected = hour.toIntOrNull() == h,
                                onClick = { hour = h.toString() },
                                label = { Text(h.toString().padStart(2, '0'), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Minute", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55).forEach { m ->
                            FilterChip(
                                selected = minute.toIntOrNull() == m,
                                onClick = { minute = m.toString() },
                                label = { Text(m.toString().padStart(2, '0'), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val h = hour.ifBlank { "00" }
                    val m = minute.ifBlank { "00" }
                    selectedTime = LocalTime.of(h.toIntOrNull() ?: 0, m.toIntOrNull() ?: 0)
                    showTimePicker = false
                }, enabled = isEditable) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
