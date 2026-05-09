package com.example.taskgo.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskgo.data.model.*
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import java.util.*

@Composable
fun SearchPostScreen(
    taskViewModel: TaskViewModel,
    userViewModel: UserViewModel,
    modifier: Modifier = Modifier
) {
    var screenState by remember { mutableStateOf("MAIN") } // MAIN, CREATE_REQUEST, CREATE_SERVICE
    val user by userViewModel.currentUser.collectAsState()
    var selectedTaskForDetail by remember { mutableStateOf<Task?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Handle selected image URI
    }
    
    if (selectedTaskForDetail != null) {
        val allTasks by taskViewModel.allTasks.collectAsState()
        // Ensure we always have the latest version of the task
        val currentTask = allTasks.find { it.id == selectedTaskForDetail?.id } ?: selectedTaskForDetail!!
        
        BackHandler { selectedTaskForDetail = null }
        TaskDetailScreen(
            task = currentTask,
            userViewModel = userViewModel,
            taskViewModel = taskViewModel,
            onBack = { selectedTaskForDetail = null },
            onAccept = {
                user?.id?.let { runnerId ->
                    taskViewModel.applyForTask(currentTask.id, runnerId)
                    taskViewModel.updateTask(currentTask.copy(status = TaskStatus.PENDING_APPROVAL))
                }
            },
            onReport = { /* Handle report */ },
            modifier = modifier
        )
    } else if (screenState == "MAIN") {
        PostMainScreen(
            taskViewModel = taskViewModel,
            user = user,
            onCreateRequest = { screenState = "CREATE_REQUEST" },
            onCreateService = { screenState = "CREATE_SERVICE" },
            onTaskClick = { selectedTaskForDetail = it },
            modifier = modifier
        )
    } else {
        BackHandler { screenState = "MAIN" }
        CreateTaskScreen(
            type = if (screenState == "CREATE_REQUEST") TaskType.REQUEST else TaskType.SERVICE,
            onBack = { screenState = "MAIN" },
            onImagePick = { photoPickerLauncher.launch("image/*") },
            onConfirm = { title, desc, cat, campus, addr, dead, amt ->
                taskViewModel.addTask(
                    title = title,
                    description = desc,
                    category = cat,
                    type = if (screenState == "CREATE_REQUEST") TaskType.REQUEST else TaskType.SERVICE,
                    campus = campus,
                    address = addr,
                    deadline = dead,
                    paymentAmount = amt ?: 0.0,
                    requesterId = user?.id ?: "unknown"
                )
                screenState = "MAIN"
            },
            modifier = modifier
        )
    }
}

@Composable
fun PostMainScreen(
    taskViewModel: TaskViewModel,
    user: User?,
    onCreateRequest: () -> Unit,
    onCreateService: () -> Unit,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier
) {
    val allTasks by taskViewModel.allTasks.collectAsState()
    var acceptedExpanded by remember { mutableStateOf(false) }
    var requestedExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
    ) {
        item {
            Text(
                "Post Something New",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PostTypeButton(
                    title = "Post Request",
                    onClick = onCreateRequest,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
                PostTypeButton(
                    title = "Offer Service",
                    onClick = onCreateService,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.Black,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Accepted Task Dropdown (Runner View) - Consistently displayed now
        item {
            val myAcceptedTasks = allTasks.filter { it.interestedRunnerIds.contains(user?.id) || it.runnerId == user?.id }
            ExpandableSection(
                title = "Accepted Task",
                isExpanded = acceptedExpanded,
                onToggle = { acceptedExpanded = !acceptedExpanded }
            ) {
                if (myAcceptedTasks.isEmpty()) {
                    Text("No accepted tasks.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    Column {
                        myAcceptedTasks.forEach { task ->
                            AcceptedTaskListItem(task, user?.id ?: "", onClick = { onTaskClick(task) })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Requested Task Dropdown (Requester View)
        item {
            val myRequestedTasks = allTasks.filter { it.requesterId == user?.id && it.status != TaskStatus.COMPLETED }
            ExpandableSection(
                title = "Requested Task",
                isExpanded = requestedExpanded,
                onToggle = { requestedExpanded = !requestedExpanded }
            ) {
                if (myRequestedTasks.isEmpty()) {
                    Text("No pending requests.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    Column {
                        myRequestedTasks.forEach { task ->
                            RequestedTaskListItem(task, onClick = { onTaskClick(task) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostTypeButton(
    title: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(title, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                content()
            }
        }
    }
}

@Composable
fun AcceptedTaskListItem(task: Task, userId: String, onClick: () -> Unit) {
    val statusText = if (task.runnerId == userId) "Accepted by requester" else "Pending for requester's approval"
    val statusColor = if (task.runnerId == userId) Color(0xFF4CAF50) else Color(0xFFFF9800)

    ListItem(
        headlineContent = { Text(task.title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(statusText, color = statusColor, fontWeight = FontWeight.Bold) },
        trailingContent = { 
            if (task.paymentAmount > 0) {
                Text("RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)}")
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun RequestedTaskListItem(task: Task, onClick: () -> Unit) {
    val statusText = if (task.interestedRunnerIds.isEmpty()) "Waiting for runner to accept" else "Pending Approval"
    val statusColor = if (task.interestedRunnerIds.isEmpty()) Color.Gray else Color(0xFFFF9800)

    Column(modifier = Modifier.clickable { onClick() }.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(task.title, fontWeight = FontWeight.Bold)
            Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        
        if (task.interestedRunnerIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Interested Runners:", style = MaterialTheme.typography.labelSmall)
            task.interestedRunnerIds.forEach { runnerId ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Runner $runnerId", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("4.5", fontSize = 10.sp, color = Color.Gray)
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFCC00))
                        }
                        Text("012-3456789", fontSize = 10.sp, color = Color.Gray)
                    }
                    Row {
                        TextButton(onClick = { /* Accept */ }) { Text("Accept", color = Color(0xFF4CAF50)) }
                        TextButton(onClick = { /* Reject */ }) { Text("Reject", color = Color.Red) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    type: TaskType,
    onBack: () -> Unit,
    onImagePick: () -> Unit,
    onConfirm: (String, String, TaskCategory, String, String, String, Double?) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var campus by remember { mutableStateOf("UTMKL") }
    var address by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dontDisplayPayment by remember { mutableStateOf(false) }
    var dontDisplayDeadline by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(TaskCategory.GENERAL) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun showDateTimePicker() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        deadline = String.format(Locale.getDefault(), "%02d/%02d/%d %02d:%02d", day, month + 1, year, hour, minute)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (type == TaskType.REQUEST) "Post Request" else "Offer Service") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Images Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFFF0F0F0), MaterialTheme.shapes.medium)
                    .clickable { onImagePick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Text("Add Images", color = Color.Gray)
                }
            }

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            // Category
            Text("Category:", fontWeight = FontWeight.Bold)
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Location
            Text("Location:", fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { campus = "UTMKL" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (campus == "UTMKL") MaterialTheme.colorScheme.primary else Color.LightGray)) { Text("UTMKL") }
                Button(onClick = { campus = "UTMJB" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (campus == "UTMJB") MaterialTheme.colorScheme.primary else Color.LightGray)) { Text("UTMJB") }
            }
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Specific Address") }, modifier = Modifier.fillMaxWidth())

            // Payment
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Payment (Min RM 0.20)") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("RM ") },
                    enabled = !dontDisplayPayment
                )
                if (type == TaskType.SERVICE) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { dontDisplayPayment = !dontDisplayPayment }) {
                        Checkbox(checked = dontDisplayPayment, onCheckedChange = { dontDisplayPayment = it })
                        Text("Don't display payment (If have multiple prices)", fontSize = 12.sp)
                    }
                }
            }

            // Deadline
            Column {
                OutlinedTextField(
                    value = if (dontDisplayDeadline) "No Deadline" else deadline,
                    onValueChange = { },
                    label = { Text("Deadline (Date & Time)") },
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !dontDisplayDeadline) { showDateTimePicker() },
                    trailingIcon = { 
                        IconButton(onClick = { if (!dontDisplayDeadline) showDateTimePicker() }, enabled = !dontDisplayDeadline) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        }
                    },
                    readOnly = true,
                    enabled = !dontDisplayDeadline
                )
                if (type == TaskType.SERVICE) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { dontDisplayDeadline = !dontDisplayDeadline }) {
                        Checkbox(checked = dontDisplayDeadline, onCheckedChange = { dontDisplayDeadline = it })
                        Text("Don't display deadline", fontSize = 12.sp)
                    }
                }
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val amt = if (dontDisplayPayment) 0.0 else amount.toDoubleOrNull()
                    val finalDeadline = if (dontDisplayDeadline) "None" else deadline
                    
                    if (title.isBlank() || desc.isBlank() || address.isBlank() || (finalDeadline.isBlank() && !dontDisplayDeadline) || (amt == null && !dontDisplayPayment)) {
                        errorMessage = "All fields are required"
                    } else if (!dontDisplayPayment && amt!! < 0.20) {
                        errorMessage = "Minimum payment is RM 0.20"
                    } else {
                        onConfirm(title, desc, selectedCategory, campus, address, finalDeadline, amt)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Post Now", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            
            // Extra spacing to ensure button is not blocked by bottom nav
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
