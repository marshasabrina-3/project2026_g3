package com.example.taskgo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.data.model.UserRole
import com.example.taskgo.data.model.Review
import com.example.taskgo.data.model.Report
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import com.example.taskgo.util.ImageUtils
import java.util.Locale

@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    taskViewModel: TaskViewModel,
    onLogout: () -> Unit,
    onTaskClick: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val user by userViewModel.currentUser.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val allReviews by taskViewModel.allReviews.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    
    var screenState by remember { mutableStateOf("MAIN") }
    var showEnlargedImage by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showEditImageDialog by remember { mutableStateOf(false) }

    // Reporting & Reviewing state
    var selectedTaskForAction by remember { mutableStateOf<Task?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            showEditImageDialog = true
        }
    }

    BackHandler(screenState != "MAIN") {
        screenState = "MAIN"
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFAFAFA))) {
        when (screenState) {
            "MAIN" -> {
                ProfileMainContent(
                    user = user,
                    taskViewModel = taskViewModel,
                    onLogout = onLogout,
                    onNavigateToPosted = { screenState = "POSTED_HISTORY" },
                    onNavigateToCompleted = { screenState = "COMPLETED_HISTORY" },
                    onNavigateToReportIssue = { screenState = "REPORT_ISSUE" },
                    onEnlargeImage = { showEnlargedImage = true },
                    onEditProfile = { showEditProfileDialog = true }
                )
            }
            "POSTED_HISTORY" -> {
                TaskHistoryPage(
                    title = "My Posted Tasks",
                    tasks = allTasks.filter { it.requesterId == user?.id },
                    reviews = allReviews,
                    onTaskClick = onTaskClick,
                    onReviewRunner = { 
                        selectedTaskForAction = it
                        showReviewDialog = true
                    },
                    onReportRunner = {
                        selectedTaskForAction = it
                        showReportDialog = true
                    },
                    onBack = { screenState = "MAIN" }
                )
            }
            "COMPLETED_HISTORY" -> {
                TaskHistoryPage(
                    title = "Tasks I Completed",
                    tasks = allTasks.filter { it.runnerId == user?.id && it.status == TaskStatus.COMPLETED },
                    reviews = allReviews,
                    onTaskClick = onTaskClick,
                    onBack = { screenState = "MAIN" }
                )
            }
            "REPORT_ISSUE" -> {
                ReportIssuePage(onBack = { screenState = "MAIN" })
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }

    // Dialogs
    if (showReviewDialog && selectedTaskForAction != null) {
        ReviewDialog(
            task = selectedTaskForAction!!,
            onDismiss = { showReviewDialog = false },
            onConfirm = { rating, comment ->
                taskViewModel.addReview(
                    Review(
                        taskId = selectedTaskForAction!!.id,
                        reviewerId = user?.id ?: "",
                        revieweeId = selectedTaskForAction!!.runnerId ?: "",
                        rating = rating,
                        comment = comment
                    )
                )
                showReviewDialog = false
            }
        )
    }

    if (showReportDialog && selectedTaskForAction != null) {
        ReportRunnerDialog(
            task = selectedTaskForAction!!,
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                taskViewModel.addReport(
                    Report(
                        reporterId = user?.id ?: "",
                        reportedUserId = selectedTaskForAction!!.runnerId ?: "",
                        taskId = selectedTaskForAction!!.id,
                        description = reason
                    )
                )
                showReportDialog = false
            }
        )
    }

    if (showEnlargedImage) {
        EnlargedImageDialog(
            user = user,
            onDismiss = { showEnlargedImage = false },
            onEdit = { 
                showEnlargedImage = false
                photoPickerLauncher.launch("image/*")
            }
        )
    }

    if (showEditProfileDialog) {
        EditProfilePhoneDialog(
            currentPhone = user?.phoneNumber ?: "",
            onDismiss = { showEditProfileDialog = false },
            onSave = { newPhone ->
                userViewModel.updateProfile(user?.name ?: "", user?.email ?: "", newPhone)
                showEditProfileDialog = false
            }
        )
    }

    if (showEditImageDialog && selectedUri != null) {
        EditImageDialog(
            imageUri = selectedUri!!,
            onDismiss = { showEditImageDialog = false },
            onSave = { uri ->
                userViewModel.uploadProfileImage(uri)
                showEditImageDialog = false
            }
        )
    }
}

@Composable
fun ProfileMainContent(
    user: com.example.taskgo.data.model.User?,
    taskViewModel: TaskViewModel,
    onLogout: () -> Unit,
    onNavigateToPosted: () -> Unit,
    onNavigateToCompleted: () -> Unit,
    onNavigateToReportIssue: () -> Unit,
    onEnlargeImage: () -> Unit,
    onEditProfile: () -> Unit
) {
    val utmMaroon = Color(0xFF800000)
    val averageRating = remember(user) { user?.id?.let { taskViewModel.getUserRating(it) } ?: 0.0 }
    val reportCount = remember(user) { user?.id?.let { taskViewModel.getUserReportCount(it) } ?: 0 }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    brush = Brush.verticalGradient(listOf(utmMaroon, Color(0xFFB30000))),
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(100.dp).clickable { onEnlargeImage() }.shadow(8.dp, CircleShape),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    if (!user?.profileImageUrl.isNullOrEmpty()) {
                        val imageBytes = remember(user!!.profileImageUrl) { ImageUtils.decodeBase64ToByteArray(user.profileImageUrl) }
                        AsyncImage(
                            model = imageBytes,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = user?.name?.take(1) ?: "?", style = MaterialTheme.typography.displayMedium, color = utmMaroon, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (user?.role == UserRole.ADMIN) "Administrator" else "UTM Student",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(" %.1f".format(Locale.getDefault(), averageRating), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Report, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(" $reportCount Reports", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp).offset(y = (-20).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = user?.name ?: "User", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.Black, textAlign = TextAlign.Center)
                    Text(text = user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(text = user?.phoneNumber ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Account Settings", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(12.dp))

            ProfileMenuButton(text = "Edit Phone Number", icon = Icons.Default.Phone, onClick = onEditProfile)
            ProfileMenuButton(text = "My Posted Tasks History", icon = Icons.Default.History, onClick = onNavigateToPosted)
            ProfileMenuButton(text = "Completed Tasks (Runner)", icon = Icons.Default.CheckCircle, onClick = onNavigateToCompleted)
            ProfileMenuButton(text = "Report App Issues", icon = Icons.Default.BugReport, onClick = onNavigateToReportIssue)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEEBEC), contentColor = Color.Red)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Logout", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileMenuButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color(0xFF800000).copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF800000))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskHistoryPage(
    title: String, 
    tasks: List<Task>, 
    reviews: List<Review>? = null, 
    onTaskClick: (Task) -> Unit,
    onReviewRunner: ((Task) -> Unit)? = null,
    onReportRunner: ((Task) -> Unit)? = null,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("No history found.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tasks) { task ->
                    val existingReview = reviews?.find { it.taskId == task.id && it.reviewerId != task.runnerId }
                    
                    if (onReviewRunner != null) {
                        PostedTaskHistoryItem(
                            task = task,
                            review = existingReview,
                            onClick = { onTaskClick(task) },
                            onReview = { onReviewRunner(task) },
                            onReport = { onReportRunner?.invoke(task) }
                        )
                    } else {
                        val runnerReview = reviews?.find { it.taskId == task.id && it.revieweeId == task.runnerId }
                        CompletedTaskItem(
                            task = task, 
                            review = runnerReview,
                            onClick = { onTaskClick(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostedTaskHistoryItem(
    task: Task, 
    review: Review?,
    onClick: () -> Unit,
    onReview: () -> Unit,
    onReport: () -> Unit
) {
    val statusColor = when (task.status) {
        TaskStatus.OPEN -> Color(0xFF4CAF50)
        TaskStatus.ASSIGNED -> Color(0xFF2196F3)
        TaskStatus.PENDING_APPROVAL -> Color(0xFFFF9800)
        TaskStatus.WAITING_VERIFICATION -> Color(0xFF9C27B0)
        TaskStatus.COMPLETED -> Color(0xFF43A047)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("RM %.2f".format(Locale.getDefault(), task.paymentAmount), color = Color(0xFF800000), fontWeight = FontWeight.ExtraBold)
            }
            
            Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text(text = task.status.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            if (task.status == TaskStatus.COMPLETED && task.runnerId != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                
                Text(
                    text = "Task completed by: ${task.runnerName ?: "User"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                if (review != null) {
                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = if (i < review.rating) Color(0xFFFFB300) else Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(review.comment, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onReport() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red)
                        ) {
                            Text("Report Runner", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onReview() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))
                        ) {
                            Text("Write Review", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedTaskItem(task: Task, review: Review?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Completed", color = Color(0xFF43A047), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            
            review?.let {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                Text("Your review:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = if (i < it.rating) Color(0xFFFFB300) else Color.LightGray)
                    }
                }
                Text(it.comment, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun ReviewDialog(task: Task, onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate ${task.runnerName ?: "Runner"}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
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
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("How was the service?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(rating, comment) }) { Text("Submit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ReportRunnerDialog(task: Task, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report ${task.runnerName ?: "Runner"}") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason for reporting") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = { Button(onClick = { onConfirm(reason) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Report") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssuePage(onBack: () -> Unit) {
    var category by remember { mutableStateOf("Bugs") }
    var reason by remember { mutableStateOf("") }
    val categories = listOf("Bugs", "UI issues", "Performance", "Safety", "Others")
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report App Issues", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("What type of issue are you facing?", fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expanded = false }) }
                }
            }
            OutlinedTextField(value = reason, onValueChange = { reason = it }, modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(12.dp), placeholder = { Text("Describe the issue...") })
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))) { Text("Submit Report") }
        }
    }
}

@Composable
fun EnlargedImageDialog(user: com.example.taskgo.data.model.User?, onDismiss: () -> Unit, onEdit: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(modifier = Modifier.size(300.dp), shape = CircleShape, color = Color.White) {
                    if (!user?.profileImageUrl.isNullOrEmpty()) {
                        val imageBytes = remember(user!!.profileImageUrl) { ImageUtils.decodeBase64ToByteArray(user.profileImageUrl) }
                        AsyncImage(model = imageBytes, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(contentAlignment = Alignment.Center) { Text(text = user?.name?.take(1) ?: "", fontSize = 120.sp, color = Color(0xFF800000)) }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onEdit, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("Change Picture") }
            }
        }
    }
}

@Composable
fun EditProfilePhoneDialog(currentPhone: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var phone by remember { mutableStateOf(currentPhone) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Phone Number") },
        text = { OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { onSave(phone) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditImageDialog(imageUri: android.net.Uri, onDismiss: () -> Unit, onSave: (android.net.Uri) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(300.dp).clip(CircleShape).background(Color.Gray)) {
                    AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { onSave(imageUri) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("Set as Profile Picture") }
            }
        }
    }
}
