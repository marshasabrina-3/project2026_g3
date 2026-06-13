package com.example.taskgo.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.taskgo.util.AiAgentManager
import kotlinx.coroutines.launch
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
    onLogout: () -> Unit,
    isEmbedded: Boolean = false,
    onBack: () -> Unit = {},
    onViewUserProfile: (String) -> Unit = {},
    initialTab: Int = 0,
    onTabChange: (Int) -> Unit = {}
) {
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showActivityReport by remember { mutableStateOf(false) } // TG-US28 state
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskToViewReports by remember { mutableStateOf<Task?>(null) }
    var taskToViewProofs by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    val allTasksByModel by taskViewModel.allTasks.collectAsState()
    val allReportsByModel by taskViewModel.allReports.collectAsState()

    LaunchedEffect(Unit) {
        userViewModel.fetchAllUserRecords()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("TaskGO Admin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        if (isEmbedded) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    actions = {
                        // TG-US28: Generate Task Activity Report Action Button
                        IconButton(onClick = { showActivityReport = true }) {
                            Icon(Icons.Default.Analytics, contentDescription = "System Analytics Summary", tint = MaterialTheme.colorScheme.primary)
                        }
                        if (!isEmbedded) {
                            IconButton(onClick = onLogout) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { 
                            selectedTab = 0
                            onTabChange(0) 
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Tasks") },
                        label = { Text("Tasks") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { 
                            selectedTab = 1
                            onTabChange(1) 
                        },
                        icon = { Icon(Icons.Default.Report, contentDescription = "Reports") },
                        label = { Text("Reports") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { 
                            selectedTab = 3
                            onTabChange(3) 
                        },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Reviews") },
                        label = { Text("Reviews") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { 
                            selectedTab = 2
                            onTabChange(2) 
                        },
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
                            onViewProofs = { taskToViewProofs = it }
                        )
                    }
                    1 -> {
                        AdminReportsList(taskViewModel = taskViewModel, userViewModel = userViewModel)
                    }
                    2 -> {
                        AdminUserManagementScreen(
                            userViewModel = userViewModel,
                            onViewProfile = onViewUserProfile
                        )
                    }
                    3 -> {
                        AdminBadReviewsList(
                            taskViewModel = taskViewModel,
                            userViewModel = userViewModel,
                            onViewProfile = { userId -> onViewUserProfile(userId) }
                        )
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = "Confirm Task Removal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${taskToDelete!!.title}\"? This will flag its status as removed and hide it across all student streams.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Remove Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onViewProofs: (Task) -> Unit
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var currentStatusFilter by remember { mutableStateOf("ALL") }
    val filterOptions = listOf("ALL", "OPEN", "PENDING_APPROVAL", "ASSIGNED", "WAITING_VERIFICATION", "COMPLETED", "CANCELLED", "REMOVED")

    val filteredTasks = remember(tasks, currentStatusFilter, searchQuery) {
        tasks.filter { task ->
            val matchesStatus = if (currentStatusFilter == "ALL") true
                               else task.status.toString().uppercase(Locale.ROOT) == currentStatusFilter
            val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) ||
                               task.id.contains(searchQuery, ignoreCase = true) ||
                               task.requesterName.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tasks...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )

                var showFilterMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        filterOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.replace("_", " ")) },
                                onClick = {
                                    currentStatusFilter = status
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = filterOptions.indexOf(currentStatusFilter).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.surface,
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
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

        if (filteredTasks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No tasks found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        onViewProofs = { onViewProofs(task) }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isAdminTask) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column {
            if (isAdminTask) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("OFFICIAL ADMIN POST", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = task.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(text = "RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = statusBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(end = 8.dp)) {
                        Text(text = normalizedStatus.replace("_", " "), color = statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    val safeId = task.id
                    val displayId = if (safeId.length >= 8) safeId.take(8).uppercase(Locale.ROOT) else safeId.uppercase(Locale.ROOT)
                    Text(text = "Ref: $displayId...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Requested By: ${task.requesterName.ifBlank { task.requesterId }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    Text(text = "Assigned Runner: ${task.runnerName ?: task.runnerId ?: "None assigned yet"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
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
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Task Workflow Receipts", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Completion Proof Image:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

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
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PendingActions, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No completion proof uploaded yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text("Payment Status & Verification:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

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
                    Text("Payment Receipt Attachment:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
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
                Text("Dismiss", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}


@Composable
fun AdminReportsList(taskViewModel: TaskViewModel, userViewModel: UserViewModel) {
    val reports by taskViewModel.allReports.collectAsState()
    val allUsers by userViewModel.allUsers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var currentStatusFilter by remember { mutableStateOf("ALL") }
    val filterOptions = listOf("ALL", "PENDING", "RESOLVED", "ACTION_TAKEN")

    val filteredReports = remember(reports, currentStatusFilter, searchQuery, allUsers) {
        reports.filter { report ->
            val matchesStatus = if (currentStatusFilter == "ALL") true
                               else report.status?.toString()?.uppercase(Locale.ROOT) == currentStatusFilter

            val reporterName = allUsers.find { it.id == report.reporterId }?.name ?: ""
            val matchesSearch = report.description.contains(searchQuery, ignoreCase = true) ||
                               (report.taskId?.contains(searchQuery, ignoreCase = true) ?: false) ||
                               report.reporterId.contains(searchQuery, ignoreCase = true) ||
                               reporterName.contains(searchQuery, ignoreCase = true) ||
                               report.reason.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesSearch
        }.sortedByDescending { it.timestamp }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search reports...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )

                var showFilterMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        filterOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.replace("_", " ")) },
                                onClick = {
                                    currentStatusFilter = status
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (filteredReports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Report, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No reports found.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredReports) { report ->
                    val reporterName = allUsers.find { it.id == report.reporterId }?.name ?: "Unknown User"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (report.taskId != null) "Task ID: ${report.taskId}" else "System/App Issue",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                                val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(report.timestamp))
                                Text(date, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text("Reporter: $reporterName", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Text("ID: ${report.reporterId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = report.reason.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(report.description, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                var showStatusMenu by remember { mutableStateOf(false) }
                                Box {
                                    Text(
                                        text = "Status: ${report.status.toString().replace("_", " ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier
                                            .clickable { showStatusMenu = true }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                                        com.example.taskgo.data.model.ReportStatus.entries.forEach { status ->
                                            DropdownMenuItem(
                                                text = { Text(status.name.replace("_", " ")) },
                                                onClick = {
                                                    taskViewModel.updateReportStatus(report.id, status)
                                                    showStatusMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBadReviewsList(
    taskViewModel: TaskViewModel,
    userViewModel: UserViewModel,
    onViewProfile: (String) -> Unit
) {
    val reviews by taskViewModel.allReviews.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val allUsers by userViewModel.allUsers.collectAsState()
    
    // Filtering for low ratings (Task #191)
    val badReviews = remember(reviews) { reviews.filter { it.rating <= 2 }.sortedByDescending { it.timestamp } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bad Review Monitoring", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        if (badReviews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No low-rated reviews found.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(badReviews) { review ->
                    val task = allTasks.find { it.id == review.taskId }
                    val runner = allUsers.find { it.id == review.revieweeId } ?: com.example.taskgo.data.model.User(name = "Unknown Runner")
                    val requester = allUsers.find { it.id == review.reviewerId } ?: com.example.taskgo.data.model.User(name = "Unknown Requester")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rating: ${review.rating}/5", fontWeight = FontWeight.ExtraBold, color = Color.Red)
                                val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(review.timestamp))
                                Text(date, fontSize = 11.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Task: ${task?.title ?: "Deleted Task"}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Comment: \"${review.comment}\"", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Runner: ${runner.name}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("Requester: ${requester.name}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                
                                // TG-US26 sub-task: Allow admin to review related user profile
                                Button(
                                    onClick = { onViewProfile(review.revieweeId) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("View Profile", fontSize = 10.sp)
                                }
                            }
                        }
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

    val scope = rememberCoroutineScope()
    var arbitrationSummary by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    LaunchedEffect(task.id) {
        if (task.paymentStatus == PaymentStatus.DISPUTED) {
            isAnalyzing = true
            val transcript = reports.joinToString("\n") { "${it.reporterId}: ${it.description}" }
            arbitrationSummary = AiAgentManager.generateDisputeArbitration(
                task.title,
                task.description,
                transcript
            )
            isAnalyzing = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        title = {
            Column {
                Text("Dispute & Issue Investigation", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Task Ref ID: ${task.id.take(8).uppercase(Locale.ROOT)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Task: ${task.title}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Text("Description: ${task.description}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Requester: ${task.requesterName.ifBlank { task.requesterId }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Runner: ${task.runnerName ?: task.runnerId ?: "Unassigned"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // AI Arbitration Section
                if (task.paymentStatus == PaymentStatus.DISPUTED) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Magic Arbitration Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isAnalyzing) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            } else {
                                Text(
                                    text = arbitrationSummary ?: "No analysis available.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Section B: List of Filed Complaints
                Text("Filed Grievances / Complaints:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                if (sortedReports.isEmpty()) {
                    Text("No formal logs attached to this item.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                } else {
                    sortedReports.forEach { report ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("By User: ${report.reporterId}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(text = report.status?.toString() ?: "PENDING", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = report.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section C: Runner Delivery Verification Evidence
                Text("Evidence 1: Runner Completion Proof", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                if (runnerCompletionBitmap != null) {
                    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        Image(bitmap = runnerCompletionBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                } else {
                    Text("⚠️ No completion image attached by runner.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }

                // Section D: Requester Financial Verification Evidence
                Text("Evidence 2: Requester Payment Status", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current Payment Status:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("⚠️ No transaction slip uploaded by requester.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("Close File", color = MaterialTheme.colorScheme.onPrimary)
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
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("System Task Activity Report", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Generated Summary Summary:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricRow(label = "Total Posted Tasks", value = totalPostedTasks.toString(), color = MaterialTheme.colorScheme.onSurface)
                    MetricRow(label = "Completed Workflows", value = completedTasksCount.toString(), color = Color(0xFF2E7D32))
                    MetricRow(label = "Cancelled Assignments", value = cancelledTasksCount.toString(), color = MaterialTheme.colorScheme.error)
                    MetricRow(label = "System Disputes/Reports Filed", value = totalReportedIssues.toString(), color = Color(0xFFF57F17))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Paid Volume:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "RM ${String.format(Locale.getDefault(), "%.2f", totalPayoutProcessed)}",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss Report", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun MetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New Admin Task", color = MaterialTheme.colorScheme.onSurface) },
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
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Edit Task Details", color = MaterialTheme.colorScheme.onSurface) },
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
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}
