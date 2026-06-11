package com.example.taskgo.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.taskgo.data.model.*
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import com.example.taskgo.util.AiAgentManager
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Composable
fun SearchPostScreen(
    taskViewModel: TaskViewModel,
    userViewModel: UserViewModel,
    onChat: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var screenState by remember { mutableStateOf("MAIN") }
    var previousScreenState by remember { mutableStateOf("MAIN") }
    val user by userViewModel.currentUser.collectAsState()
    var selectedTaskForDetail by remember { mutableStateOf<Task?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var selectedLocation by remember { mutableStateOf<com.google.android.gms.maps.model.LatLng?>(null) }
    var selectedAddressStr by remember { mutableStateOf<String?>(null) }
    var mapPickerTarget by remember { mutableStateOf<String?>(null) } // "START" or "DEST"
    val isPosting by taskViewModel.isPosting.collectAsState()

    val utmMaroon = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (selectedTaskForDetail != null) {
            val allTasks by taskViewModel.allTasks.collectAsState()
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
                    }
                },
                onReport = { /* Handle report */ },
                onChat = { otherId, title ->
                    onChat(currentTask.id, otherId, title)
                },
                onEdit = { task ->
                    taskToEdit = task
                    selectedTaskForDetail = null
                    screenState = "EDIT"
                }
            )
        } else {
            // Main content logic to preserve state during map picking
            if (screenState == "MAIN") {
                PostMainScreen(
                    taskViewModel = taskViewModel,
                    user = user,
                    onCreateRequest = {
                        selectedLocation = null
                        selectedAddressStr = null
                        screenState = "CREATE_REQUEST"
                    },
                    onCreateService = {
                        selectedLocation = null
                        selectedAddressStr = null
                        screenState = "CREATE_SERVICE"
                    },
                    onTaskClick = { selectedTaskForDetail = it }
                )
            }

            // CreateTaskScreen remains in composition during MAP state if we are picking a location
            val isCreationMode = screenState.startsWith("CREATE") || screenState == "EDIT"
            val wasCreationMode = previousScreenState.startsWith("CREATE") || previousScreenState == "EDIT"
            
            if (isCreationMode || (screenState == "MAP" && wasCreationMode)) {
                BackHandler {
                    screenState = "MAIN"
                    taskToEdit = null
                }
                CreateTaskScreen(
                    type = if (screenState == "CREATE_REQUEST") TaskType.REQUEST 
                           else if (screenState == "CREATE_SERVICE") TaskType.SERVICE 
                           else if (wasCreationMode && previousScreenState == "CREATE_REQUEST") TaskType.REQUEST
                           else if (wasCreationMode && previousScreenState == "CREATE_SERVICE") TaskType.SERVICE
                           else taskToEdit?.type ?: TaskType.REQUEST,
                    taskToEdit = taskToEdit,
                    initialLatLng = selectedLocation,
                    initialAddress = selectedAddressStr,
                    initialPickerTarget = mapPickerTarget,
                    onOpenMap = { target, currentLoc ->
                        mapPickerTarget = target
                        selectedLocation = currentLoc
                        selectedAddressStr = null
                        previousScreenState = screenState
                        screenState = "MAP"
                    },
                    onLocationConsumed = {
                        selectedLocation = null
                        selectedAddressStr = null
                        mapPickerTarget = null
                    },
                    onBack = {
                        screenState = "MAIN"
                        taskToEdit = null
                        selectedLocation = null
                        selectedAddressStr = null
                    },
                    onConfirm = { title, desc, cat, campus, addr, dead, amt, images, lat, lng, destAddr, destLat, destLng ->
                        if (screenState == "EDIT" && taskToEdit != null) {
                            taskViewModel.updateTask(taskToEdit!!.copy(
                                title = title,
                                description = desc,
                                category = cat,
                                campus = campus,
                                address = addr,
                                destinationAddress = destAddr,
                                deadline = dead,
                                paymentAmount = amt ?: 0.0,
                                latitude = lat,
                                longitude = lng,
                                destinationLatitude = destLat,
                                destinationLongitude = destLng
                            ))
                        } else {
                            taskViewModel.addTask(
                                title = title,
                                description = desc,
                                category = cat,
                                type = if (screenState == "CREATE_REQUEST") TaskType.REQUEST else TaskType.SERVICE,
                                campus = campus,
                                address = addr,
                                destinationAddress = destAddr,
                                deadline = dead,
                                paymentAmount = amt ?: 0.0,
                                requesterId = user?.id ?: "unknown",
                                requesterName = user?.name ?: "Unknown",
                                imageUris = images,
                                latitude = lat,
                                longitude = lng,
                                destinationLatitude = destLat,
                                destinationLongitude = destLng
                            )
                        }
                        screenState = "MAIN"
                        taskToEdit = null
                        selectedLocation = null
                        selectedAddressStr = null
                    }
                )
            }

            if (screenState == "MAP") {
                MapSelectorScreen(
                    initialLocation = if (selectedLocation != null) selectedLocation!!
                                     else if (taskToEdit?.latitude != null) com.google.android.gms.maps.model.LatLng(taskToEdit!!.latitude!!, taskToEdit!!.longitude!!)
                                     else com.google.android.gms.maps.model.LatLng(3.1718, 101.7145),
                    onLocationSelected = { latLng, address ->
                        selectedLocation = latLng
                        selectedAddressStr = address
                        screenState = previousScreenState
                    },
                    onBack = { screenState = previousScreenState }
                )
            }
        }

        if (isPosting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = utmMaroon, strokeWidth = 4.dp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Creating Task...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Uploading images to cloud", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun PostMainScreen(
    taskViewModel: TaskViewModel,
    user: User?,
    onCreateRequest: () -> Unit,
    onCreateService: () -> Unit,
    onTaskClick: (Task) -> Unit
) {
    val allTasks by taskViewModel.allTasks.collectAsState()

    // Controlled expansion states
    var requestsExpanded by remember { mutableStateOf(true) }
    var servicesExpanded by remember { mutableStateOf(true) }
    var applicationsRequestExpanded by remember { mutableStateOf(true) }
    var applicationsServiceExpanded by remember { mutableStateOf(true) }

    // Auto-expand/collapse based on data load (only once or when tasks change from empty to non-empty)
    var hasInitializedExpansion by remember { mutableStateOf(false) }
    LaunchedEffect(allTasks, user) {
        if (allTasks.isNotEmpty() && user != null && !hasInitializedExpansion) {
            val myRequests = allTasks.filter { it.requesterId == user.id && it.type == TaskType.REQUEST && it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
            val myServices = allTasks.filter { it.requesterId == user.id && it.type == TaskType.SERVICE && it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
            val appliedRequests = allTasks.filter { (it.interestedRunnerIds.contains(user.id) || it.runnerId == user.id) && it.type == TaskType.REQUEST && it.status != TaskStatus.CANCELLED }
            val appliedServices = allTasks.filter { (it.interestedRunnerIds.contains(user.id) || it.runnerId == user.id) && it.type == TaskType.SERVICE && it.status != TaskStatus.CANCELLED }

            requestsExpanded = myRequests.isNotEmpty()
            servicesExpanded = myServices.isNotEmpty()
            applicationsRequestExpanded = appliedRequests.isNotEmpty()
            applicationsServiceExpanded = appliedServices.isNotEmpty()
            hasInitializedExpansion = true
        }
    }

    val utmMaroon = Color(0xFF800000)

    var taskToCancel by remember { mutableStateOf<Task?>(null) }
    var taskToMarkFinished by remember { mutableStateOf<Task?>(null) }
    var showReviewDialogForTask by remember { mutableStateOf<Task?>(null) }

    val proofPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && taskToMarkFinished != null) {
            taskViewModel.markTaskAsFinished(taskToMarkFinished!!.id, uri)
            taskToMarkFinished = null
        }
    }

    if (taskToMarkFinished != null) {
        AlertDialog(
            onDismissRequest = { taskToMarkFinished = null },
            title = { Text("Complete Task", fontWeight = FontWeight.Bold) },
            text = { Text("Please upload a proof of completion for '${taskToMarkFinished?.title}'.") },
            confirmButton = {
                Button(onClick = { proofPickerLauncher.launch("image/*") }) { Text("Upload & Finish") }
            },
            dismissButton = { TextButton(onClick = { taskToMarkFinished = null }) { Text("Cancel") } }
        )
    }

    if (showReviewDialogForTask != null) {
        var rating by remember { mutableIntStateOf(5) }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReviewDialogForTask = null },
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
                        label = { Text("Write a review") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    taskViewModel.addReview(Review(
                        taskId = showReviewDialogForTask!!.id,
                        reviewerId = user?.id ?: "",
                        revieweeId = showReviewDialogForTask!!.runnerId ?: "",
                        rating = rating,
                        comment = comment
                    ))
                    showReviewDialogForTask = null
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showReviewDialogForTask = null }) { Text("Skip") } }
        )
    }

    if (taskToCancel != null) {
        AlertDialog(
            onDismissRequest = { taskToCancel = null },
            title = { Text("Confirm Cancellation?") },
            text = { Text("Are you sure you want to cancel '${taskToCancel?.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    taskViewModel.cancelTask(taskToCancel!!.id)
                    taskToCancel = null
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { taskToCancel = null }) { Text("No") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(listOf(utmMaroon, Color(0xFFB30000))),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Column {
                Text("Creation Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("What would you like to post today?", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PostTypeCard("Need Help", "Post Request", Icons.Default.Handshake, utmMaroon, onCreateRequest, Modifier.weight(1f))
                    PostTypeCard("Have Skill", "Offer Service", Icons.Default.Work, Color(0xFF2E7D32), onCreateService, Modifier.weight(1f))
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // My Live Posts
            item { SectionLabel("My Live Posts") }
            item {
                val myRequests = allTasks.filter {
                    it.requesterId == user?.id &&
                    it.type == TaskType.REQUEST &&
                    it.status != TaskStatus.COMPLETED &&
                    it.status != TaskStatus.CANCELLED &&
                    it.status != TaskStatus.REMOVED &&
                    !it.hiddenByRequester &&
                    !it.cancelledByAdmin
                }
                ExpandableListSection("Requested Tasks", myRequests.size, requestsExpanded, { requestsExpanded = !requestsExpanded }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (myRequests.isEmpty()) EmptyHistoryItem("No live requests.")
                        else myRequests.forEach { task ->
                            CompactHistoryItem(
                                task,
                                { onTaskClick(task) },
                                onCancel = if (task.status == TaskStatus.OPEN) { { taskToCancel = task } } else null,
                                onHide = { taskViewModel.hideTaskForUser(task.id, user?.id ?: "") },
                                onAction = if (task.status == TaskStatus.WAITING_VERIFICATION) { {
                                    taskViewModel.completeTask(task.id)
                                    showReviewDialogForTask = task
                                } } else null,
                                actionLabel = if (task.status == TaskStatus.WAITING_VERIFICATION) "Confirm" else null
                            )
                        }
                    }
                }
            }
            item {
                val myServices = allTasks.filter {
                    it.requesterId == user?.id &&
                    it.type == TaskType.SERVICE &&
                    it.status != TaskStatus.COMPLETED &&
                    it.status != TaskStatus.CANCELLED &&
                    it.status != TaskStatus.REMOVED &&
                    !it.hiddenByRequester &&
                    !it.cancelledByAdmin
                }
                ExpandableListSection("Service Offers", myServices.size, servicesExpanded, { servicesExpanded = !servicesExpanded }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (myServices.isEmpty()) EmptyHistoryItem("No live services.")
                        else myServices.forEach { task ->
                            CompactHistoryItem(
                                task,
                                { onTaskClick(task) },
                                onCancel = if (task.status == TaskStatus.OPEN) { { taskToCancel = task } } else null,
                                onHide = { taskViewModel.hideTaskForUser(task.id, user?.id ?: "") },
                                onAction = if (task.status == TaskStatus.WAITING_VERIFICATION) { {
                                    taskViewModel.completeTask(task.id)
                                    showReviewDialogForTask = task
                                } } else null,
                                actionLabel = if (task.status == TaskStatus.WAITING_VERIFICATION) "Confirm" else null
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Applications Sent
            item { SectionLabel("Applications Sent") }
            item {
                val appliedRequests = allTasks.filter {
                    (it.interestedRunnerIds.contains(user?.id) || it.runnerId == user?.id) &&
                    it.type == TaskType.REQUEST &&
                    it.requesterId != user?.id &&
                    it.status != TaskStatus.CANCELLED &&
                    it.status != TaskStatus.REMOVED &&
                    !it.hiddenByRunner &&
                    !it.cancelledByAdmin
                }
                ExpandableListSection("Applications (Requests)", appliedRequests.size, applicationsRequestExpanded, { applicationsRequestExpanded = !applicationsRequestExpanded }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (appliedRequests.isEmpty()) EmptyHistoryItem("No applications sent for requests.")
                        else appliedRequests.forEach { task ->
                            val isChosen = task.runnerId == user?.id
                            val isRejected = task.runnerId != null && task.runnerId != user?.id
                            val isPending = !isChosen && !isRejected
                            CompactHistoryItem(
                                task,
                                { onTaskClick(task) },
                                if (isChosen) "ACCEPTED" else if (isRejected) "REJECTED" else "PENDING",
                                onCancel = if (isPending) { { taskViewModel.withdrawApplication(task.id, user?.id ?: "") } } else null,
                                onHide = { taskViewModel.hideTaskForUser(task.id, user?.id ?: "") },
                                onAction = if (isChosen && task.status == TaskStatus.ASSIGNED) { { taskToMarkFinished = task } } else null,
                                actionLabel = if (isChosen && task.status == TaskStatus.ASSIGNED) "Finish" else null
                            )
                        }
                    }
                }
            }
            item {
                val appliedServices = allTasks.filter {
                    (it.interestedRunnerIds.contains(user?.id) || it.requesterId == user?.id) &&
                    it.type == TaskType.SERVICE &&
                    it.runnerId != user?.id &&
                    it.status != TaskStatus.CANCELLED &&
                    it.status != TaskStatus.REMOVED &&
                    !it.hiddenByRequester &&
                    !it.cancelledByAdmin
                }
                ExpandableListSection("Applications (Services)", appliedServices.size, applicationsServiceExpanded, { applicationsServiceExpanded = !applicationsServiceExpanded }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (appliedServices.isEmpty()) EmptyHistoryItem("No applications sent for services.")
                        else appliedServices.forEach { task ->
                            val isChosen = task.requesterId == user?.id
                            val isRejected = task.requesterId != "" && task.requesterId != user?.id
                            val isPending = !isChosen && !isRejected
                            CompactHistoryItem(
                                task,
                                { onTaskClick(task) },
                                if (isChosen) "ACCEPTED" else if (isRejected) "REJECTED" else "PENDING",
                                onCancel = if (isPending) { { taskViewModel.withdrawApplication(task.id, user?.id ?: "") } } else null,
                                onHide = { taskViewModel.hideTaskForUser(task.id, user?.id ?: "") },
                                onAction = if (isChosen && task.status == TaskStatus.ASSIGNED) { { taskToMarkFinished = task } } else null,
                                actionLabel = if (isChosen && task.status == TaskStatus.ASSIGNED) "Finish" else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
fun PostTypeCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.height(140.dp).shadow(4.dp, RoundedCornerShape(24.dp)).clickable { onClick() },
        color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = color.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(24.dp)) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ExpandableListSection(title: String, count: Int, isExpanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                if (count > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                        Text("$count", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) { content() }
    }
}

@Composable
fun CompactHistoryItem(
    task: Task,
    onClick: () -> Unit,
    customStatus: String? = null,
    onCancel: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null
) {
    val statusColor = when (customStatus ?: task.status.name) {
        "ACCEPTED", "COMPLETED" -> Color(0xFF2E7D32)
        "REJECTED", "CANCELLED" -> Color(0xFFD32F2F)
        "PENDING", "OPEN", "WAITING_VERIFICATION" -> Color(0xFFFF9800)
        else -> Color.Gray
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (task.type == TaskType.REQUEST) Icons.Default.PersonSearch else Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Text(text = customStatus ?: task.status.name.lowercase().replaceFirstChar { it.uppercase() }, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (onAction != null && actionLabel != null) {
                Button(onClick = onAction, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), modifier = Modifier.height(32.dp)) {
                    Text(actionLabel, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (onCancel != null) {
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Cancel, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            } else if (onHide != null && (task.status == TaskStatus.CANCELLED || task.status == TaskStatus.COMPLETED || customStatus == "REJECTED")) {
                IconButton(onClick = onHide, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                }
            } else if (onAction == null) {
                Text("RM %.2f".format(Locale.getDefault(), task.paymentAmount), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun EmptyHistoryItem(text: String) {
    Text(text = text, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center, color = Color.LightGray, fontSize = 13.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    type: TaskType,
    taskToEdit: Task? = null,
    initialLatLng: com.google.android.gms.maps.model.LatLng? = null,
    initialAddress: String? = null,
    initialPickerTarget: String? = null,
    onOpenMap: (String, com.google.android.gms.maps.model.LatLng?) -> Unit,
    onLocationConsumed: () -> Unit,
    onBack: () -> Unit,
    onConfirm: (String, String, TaskCategory, String, String, String, Double?, List<Uri>, Double?, Double?, String?, Double?, Double?) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var desc by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var campus by remember { mutableStateOf(taskToEdit?.campus ?: "UTMKL") }
    var address by remember { mutableStateOf(taskToEdit?.address ?: "") }
    var destinationAddress by remember { mutableStateOf(taskToEdit?.destinationAddress ?: "") }
    var deadline by remember { mutableStateOf(taskToEdit?.deadline ?: "") }
    var amount by remember { mutableStateOf(taskToEdit?.paymentAmount?.toString() ?: "") }

    var lat by remember { mutableStateOf(taskToEdit?.latitude) }
    var lng by remember { mutableStateOf(taskToEdit?.longitude) }
    var destLat by remember { mutableStateOf(taskToEdit?.destinationLatitude) }
    var destLng by remember { mutableStateOf(taskToEdit?.destinationLongitude) }

    LaunchedEffect(initialLatLng, initialAddress, initialPickerTarget) {
        if (initialLatLng != null && initialPickerTarget != null) {
            if (initialPickerTarget == "START") {
                lat = initialLatLng.latitude
                lng = initialLatLng.longitude
                if (initialAddress != null) address = initialAddress
            } else if (initialPickerTarget == "DEST") {
                destLat = initialLatLng.latitude
                destLng = initialLatLng.longitude
                if (initialAddress != null) destinationAddress = initialAddress
            }
            onLocationConsumed()
        }
    }

    var hasPrice by remember { mutableStateOf(taskToEdit?.paymentAmount != 0.0) }
    var hasDeadline by remember { mutableStateOf(taskToEdit?.deadline?.isNotBlank() ?: true) }
    var hasDestination by remember { mutableStateOf(taskToEdit?.destinationAddress?.isNotBlank() ?: (taskToEdit?.category == TaskCategory.CARPOOL)) }

    var selectedCategory by remember { mutableStateOf(taskToEdit?.category ?: TaskCategory.GENERAL) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Auto-toggle destination for Carpool
    LaunchedEffect(selectedCategory) {
        if (selectedCategory == TaskCategory.CARPOOL) {
            hasDestination = true
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isMagicSuggesting by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Explicitly reset selection when opening to avoid old state issues
    LaunchedEffect(showDatePicker) {
        if (showDatePicker && taskToEdit == null) {
            // Optional: reset to today
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) selectedImages = (selectedImages + uris).take(7)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = if (taskToEdit != null) "Update Task"
                                   else if (type == TaskType.REQUEST) "Post Request"
                                   else "Offer Service"
                    Text(titleText, fontWeight = FontWeight.Bold)
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (taskToEdit == null) {
                Column {
                    Text("Attachments (${selectedImages.size}/7)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (selectedImages.size < 7) {
                            item {
                                Surface(modifier = Modifier.size(90.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))) {
                                    Box(modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) { Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                        items(selectedImages) { uri ->
                            Box(modifier = Modifier.size(90.dp)) {
                                AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                                IconButton(onClick = { selectedImages = selectedImages.filter { it != uri } }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).padding(4.dp).background(Color.Black.copy(0.4f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("E.g. Help buy food, Carpool to JB...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                trailingIcon = {
                    if (title.length > 5 && taskToEdit == null) {
                        if (isMagicSuggesting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    isMagicSuggesting = true
                                    val suggestion = AiAgentManager.suggestMetadata(title, desc)
                                    if (suggestion.category != null || suggestion.campus != null) {
                                        suggestion.category?.let { cat: com.example.taskgo.data.model.TaskCategory ->
                                            selectedCategory = cat
                                        }
                                        suggestion.campus?.let { cam: String ->
                                            campus = cam
                                        }
                                        android.widget.Toast.makeText(context, "Smart suggestions applied!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "No clear suggestions found.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    isMagicSuggesting = false
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Magic Suggest",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            )

            Column {
                Text("Category", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskCategory.entries.forEach { cat ->
                        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
            }

            Column {
                val startLabel = if (type == TaskType.REQUEST && selectedCategory == TaskCategory.CARPOOL) "Pick-up Location" else "Location"
                Text(startLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { campus = "UTMKL" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (campus == "UTMKL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (campus == "UTMKL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("UTMKL") }
                    Button(onClick = { campus = "UTMJB" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (campus == "UTMJB") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (campus == "UTMJB") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("UTMJB") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (type == TaskType.REQUEST && selectedCategory == TaskCategory.CARPOOL) "Start Address" else "Detailed Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            onOpenMap("START", if (lat != null && lng != null) com.google.android.gms.maps.model.LatLng(lat!!, lng!!) else null)
                        }) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Pick on Map",
                                tint = if (lat != null) Color(0xFF43A047) else MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                if (type == TaskType.REQUEST && selectedCategory != TaskCategory.CARPOOL) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasDestination, onCheckedChange = { hasDestination = it })
                        Text("Add a drop-off/delivery destination?", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (type == TaskType.REQUEST && hasDestination) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = destinationAddress,
                        onValueChange = { destinationAddress = it },
                        label = { Text(if (selectedCategory == TaskCategory.CARPOOL) "Destination Address" else "Delivery/Drop-off Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                onOpenMap("DEST", if (destLat != null && destLng != null) com.google.android.gms.maps.model.LatLng(destLat!!, destLng!!) else null)
                            }) {
                                Icon(
                                    Icons.Default.PinDrop,
                                    contentDescription = "Pick Destination",
                                    tint = if (destLat != null) Color(0xFF43A047) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }

            // Toggles for Price and Deadline (Service only)
            if (type == TaskType.SERVICE) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Include Price?", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = hasPrice, onCheckedChange = { hasPrice = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Include Deadline?", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = hasDeadline, onCheckedChange = { hasDeadline = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary))
                }
            }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (hasPrice) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Price (RM)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            prefix = { Text("RM ", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    if (hasDeadline) {
                        Surface(
                            modifier = Modifier
                                .weight(1.5f)
                                .height(56.dp) // Exactly matched to OutlinedTextField height
                                .clickable { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Deadline", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = deadline.ifBlank { "Select Date/Time" },
                                        fontSize = 13.sp,
                                        color = if (deadline.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp) }

            Button(
                onClick = {
                    val amt = if (hasPrice) amount.toDoubleOrNull() ?: 0.0 else 0.0
                    val finalDeadline = if (hasDeadline) deadline else ""
                    val finalDestAddr = if (type == TaskType.REQUEST && hasDestination) destinationAddress else null

                    val isAddressRequired = type == TaskType.REQUEST

                    if (title.isBlank() || (isAddressRequired && address.isBlank())) {
                        errorMessage = if (isAddressRequired) "Please fill all required fields (Title & Address)" else "Please enter a Title"
                    } else {
                        onConfirm(title, desc, selectedCategory, campus, address, finalDeadline, amt, selectedImages, lat, lng, finalDestAddr, destLat, destLng)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (taskToEdit != null) "Update Task" else "Publish Task", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = java.time.LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                        deadline = date.toString()
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                    deadline = "${deadline.substringBefore(" ")} $h:$m"
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
