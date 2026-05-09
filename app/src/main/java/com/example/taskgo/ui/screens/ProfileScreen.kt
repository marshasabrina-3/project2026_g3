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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.data.model.UserRole
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import java.util.Locale
import coil.compose.AsyncImage

@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    taskViewModel: TaskViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by userViewModel.currentUser.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val allReviews by taskViewModel.allReviews.collectAsState()
    
    var screenState by remember { mutableStateOf("MAIN") } // MAIN, POSTED_HISTORY, COMPLETED_HISTORY, REPORT_ISSUE
    var showEnlargedImage by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showEditImageDialog by remember { mutableStateOf(false) }

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

    when (screenState) {
        "MAIN" -> {
            ProfileMainContent(
                user = user,
                onLogout = onLogout,
                onNavigateToPosted = { screenState = "POSTED_HISTORY" },
                onNavigateToCompleted = { screenState = "COMPLETED_HISTORY" },
                onNavigateToReportIssue = { screenState = "REPORT_ISSUE" },
                onEnlargeImage = { showEnlargedImage = true },
                onEditProfile = { showEditProfileDialog = true },
                modifier = modifier
            )
        }
        "POSTED_HISTORY" -> {
            TaskHistoryPage(
                title = "My Posted Tasks",
                tasks = allTasks.filter { it.requesterId == user?.id },
                onBack = { screenState = "MAIN" }
            )
        }
        "COMPLETED_HISTORY" -> {
            TaskHistoryPage(
                title = "Completed Tasks",
                tasks = allTasks.filter { it.runnerId == user?.id && it.status == TaskStatus.COMPLETED },
                reviews = allReviews,
                onBack = { screenState = "MAIN" }
            )
        }
        "REPORT_ISSUE" -> {
            ReportIssuePage(onBack = { screenState = "MAIN" })
        }
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
            onSave = { uri, _, _ ->
                userViewModel.updateProfileImage(uri.toString())
                showEditImageDialog = false
            }
        )
    }
}

@Composable
fun ProfileMainContent(
    user: com.example.taskgo.data.model.User?,
    onLogout: () -> Unit,
    onNavigateToPosted: () -> Unit,
    onNavigateToCompleted: () -> Unit,
    onNavigateToReportIssue: () -> Unit,
    onEnlargeImage: () -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier
) {
    val utmMaroon = Color(0xFF800000)

    Scaffold(containerColor = Color(0xFFFAFAFA)) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(listOf(utmMaroon, Color(0xFFB30000))),
                        shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable { onEnlargeImage() }
                            .shadow(8.dp, CircleShape),
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        if (user?.profileImageUrl != null) {
                            AsyncImage(
                                model = user.profileImageUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user?.name?.take(1) ?: "?",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = utmMaroon,
                                    fontWeight = FontWeight.Bold
                                )
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
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-20).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = user?.name ?: "Unknown User",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            text = user?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = user?.phoneNumber ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Account Settings",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))

                ProfileMenuButton(text = "Edit Phone Number", icon = Icons.Default.Phone, onClick = onEditProfile)
                ProfileMenuButton(text = "My Posted Tasks History", icon = Icons.Default.History, onClick = onNavigateToPosted)
                ProfileMenuButton(text = "Completed Tasks (Runner)", icon = Icons.Default.CheckCircle, onClick = onNavigateToCompleted)
                ProfileMenuButton(text = "Report App Issues", icon = Icons.Default.BugReport, onClick = onNavigateToReportIssue)

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
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
}

@Composable
fun ProfileMenuButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF800000).copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
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
    reviews: List<com.example.taskgo.data.model.Review>? = null,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Text("No history found.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tasks) { task ->
                    val review = reviews?.find { it.taskId == task.id }
                    if (reviews == null) PostedTaskItem(task) else CompletedTaskItem(task, review)
                }
            }
        }
    }
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
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("What type of issue are you facing?", fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(category)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expanded = false })
                    }
                }
            }
            
            Text("Detailed Description", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Please describe the issue in detail...") }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))
            ) {
                Text("Submit Report", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EnlargedImageDialog(user: com.example.taskgo.data.model.User?, onDismiss: () -> Unit, onEdit: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(modifier = Modifier.size(300.dp), shape = CircleShape, color = Color.White) {
                    if (user?.profileImageUrl != null) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = user?.name?.take(1) ?: "", style = MaterialTheme.typography.displayLarge, fontSize = 120.sp, color = Color(0xFF800000))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onEdit, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Picture")
                }
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
        text = {
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        },
        confirmButton = {
            Button(onClick = { onSave(phone) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditImageDialog(imageUri: android.net.Uri, onDismiss: () -> Unit, onSave: (android.net.Uri, Boolean, Boolean) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onSave(imageUri, false, false) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Set as Profile Picture", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PostedTaskItem(task: Task) {
    val statusLabel = when (task.status) {
        TaskStatus.OPEN -> "Open"
        TaskStatus.PENDING_APPROVAL -> "Pending Approval"
        TaskStatus.ASSIGNED -> "Ongoing"
        TaskStatus.WAITING_VERIFICATION -> "Pending Verification"
        TaskStatus.COMPLETED -> "Completed"
        TaskStatus.CANCELLED -> "Cancelled"
    }
    
    val statusColor = when (task.status) {
        TaskStatus.OPEN -> Color(0xFF4CAF50)
        TaskStatus.ASSIGNED -> Color(0xFF2196F3)
        TaskStatus.PENDING_APPROVAL -> Color(0xFFFF9800)
        TaskStatus.WAITING_VERIFICATION -> Color(0xFF9C27B0)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)}", color = Color(0xFF800000), fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CompletedTaskItem(task: Task, review: com.example.taskgo.data.model.Review?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            review?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < it.rating) Color(0xFFFFCC00) else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${it.rating}.0", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Text(it.comment, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
            } ?: Text("No review received", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
