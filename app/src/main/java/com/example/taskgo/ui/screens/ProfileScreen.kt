    package com.example.taskgo.ui.screens

    import androidx.activity.compose.BackHandler
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.isSystemInDarkTheme
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
    import com.example.taskgo.ui.viewmodel.ThemeViewModel
    import com.example.taskgo.ui.viewmodel.AppTheme
    import com.example.taskgo.util.ImageUtils
    import androidx.lifecycle.viewmodel.compose.viewModel
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale

    @Composable
    fun ProfileScreen(
        userViewModel: UserViewModel,
        taskViewModel: TaskViewModel,
        onLogout: () -> Unit,
        onTaskClick: (Task) -> Unit = {},
        onUserClick: (String) -> Unit = {},
        modifier: Modifier = Modifier,
        viewedUserId: String? = null
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val themeViewModel: ThemeViewModel = viewModel(
            viewModelStoreOwner = context as androidx.activity.ComponentActivity
        )
        val currentUser by userViewModel.currentUser.collectAsState()
        val allUsers by userViewModel.allUsers.collectAsState()
        val allTasks by taskViewModel.allTasks.collectAsState()
        val allReviews by taskViewModel.allReviews.collectAsState()
        val isLoading by userViewModel.isLoading.collectAsState()

        val isMe = viewedUserId == null || viewedUserId == currentUser?.id
        val userToShow = if (isMe) currentUser else allUsers.find { it.id == viewedUserId }

        var screenState by remember { mutableStateOf("MAIN") }
        var showEnlargedImage by remember { mutableStateOf(false) }
        var showEditProfileDialog by remember { mutableStateOf(false) }
        var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
        var showEditImageDialog by remember { mutableStateOf(false) }
        var showThemeDialog by remember { mutableStateOf(false) }
        var showReportUserDialog by remember { mutableStateOf(false) }

        // Fetch user records if we are viewing someone else and don't have them
        LaunchedEffect(viewedUserId) {
            if (!isMe && allUsers.isEmpty()) {
                userViewModel.fetchAllUserRecords()
            }
        }

        // Reporting & Reviewing state
        var selectedTaskForAction by remember { mutableStateOf<Task?>(null) }
        var showReviewDialog by remember { mutableStateOf(false) }
        var showReportDialog by remember { mutableStateOf(false) }
        var showProfileReviewDialog by remember { mutableStateOf(false) }

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

        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (userToShow == null && !isMe) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PersonOff, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                            Text("User not found or fetch failed.", color = Color.Gray)
                            Button(onClick = { userViewModel.fetchAllUserRecords() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            } else {
                when (screenState) {
                    "MAIN" -> {
                        ProfileMainContent(
                            user = userToShow,
                            taskViewModel = taskViewModel,
                            onLogout = onLogout,
                            onNavigateToPosted = { screenState = "POSTED_HISTORY" },
                            onNavigateToCompleted = { screenState = "COMPLETED_HISTORY" },
                            onNavigateToReportIssue = { screenState = "REPORT_ISSUE" },
                            onEnlargeImage = { showEnlargedImage = true },
                            onEditProfile = { showEditProfileDialog = true },
                            onSelectTheme = { showThemeDialog = true },
                            onNavigateToAdmin = { screenState = "ADMIN_PANEL" },
                            isMe = isMe,
                            onTaskClick = onTaskClick,
                            onReportUser = { showReportUserDialog = true },
                            onWriteReview = { showProfileReviewDialog = true }
                        )
                    }
                    "ADMIN_PANEL" -> {
                        AdminHomeScreen(
                            taskViewModel = taskViewModel,
                            userViewModel = userViewModel,
                            onLogout = onLogout,
                            isEmbedded = true,
                            onBack = { screenState = "MAIN" },
                            onViewUserProfile = onUserClick
                        )
                    }
                    "POSTED_HISTORY" -> {
                        TaskHistoryPage(
                            title = if (isMe) "My Posted Tasks" else "${userToShow?.name}'s Posted Tasks",
                            tasks = allTasks.filter { it.requesterId == userToShow?.id },
                            reviews = allReviews,
                            onTaskClick = onTaskClick,
                            onReviewRunner = if (isMe) {
                                {
                                    selectedTaskForAction = it
                                    showReviewDialog = true
                                }
                            } else null,
                            onReportRunner = if (isMe) {
                                {
                                    selectedTaskForAction = it
                                    showReportDialog = true
                                }
                            } else null,
                            onBack = { screenState = "MAIN" }
                        )
                    }
                    "COMPLETED_HISTORY" -> {
                        TaskHistoryPage(
                            title = if (isMe) "Tasks I Completed" else "${userToShow?.name}'s Completed Tasks",
                            tasks = allTasks.filter { it.runnerId == userToShow?.id && it.status == TaskStatus.COMPLETED },
                            reviews = allReviews,
                            onTaskClick = onTaskClick,
                            onBack = { screenState = "MAIN" }
                        )
                    }
                    "REPORT_ISSUE" -> {
                        ReportIssuePage(taskViewModel = taskViewModel, user = currentUser, onBack = { screenState = "MAIN" })
                    }
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

        if (showProfileReviewDialog && userToShow != null) {
            AlertDialog(
                onDismissRequest = { showProfileReviewDialog = false },
                title = { Text("Review ${userToShow!!.name}", fontWeight = FontWeight.Bold) },
                text = {
                    var rating by remember { mutableIntStateOf(5) }
                    var comment by remember { mutableStateOf("") }
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
                            label = { Text("Share your experience...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                taskViewModel.addReview(
                                    Review(
                                        reviewerId = currentUser?.id ?: "",
                                        revieweeId = userToShow!!.id,
                                        rating = rating,
                                        comment = comment,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                                showProfileReviewDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = comment.isNotBlank()
                        ) { Text("Submit Review") }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showProfileReviewDialog = false }) { Text("Cancel") } }
            )
        }

        if (showReviewDialog && selectedTaskForAction != null) {
            ReviewDialog(
                task = selectedTaskForAction!!,
                onDismiss = { showReviewDialog = false },
                onConfirm = { rating, comment ->
                    taskViewModel.addReview(
                        Review(
                            taskId = selectedTaskForAction!!.id,
                            reviewerId = currentUser?.id ?: "",
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
                            reporterId = currentUser?.id ?: "",
                            reportedUserId = selectedTaskForAction!!.runnerId ?: "",
                            taskId = selectedTaskForAction!!.id,
                            description = reason
                        )
                    )
                    showReportDialog = false
                }
            )
        }

        if (showReportUserDialog && userToShow != null) {
            ReportUserDialog(
                user = userToShow!!,
                onDismiss = { showReportUserDialog = false },
                onConfirm = { category, details ->
                    taskViewModel.addReport(
                        Report(
                            reporterId = currentUser?.id ?: "",
                            reportedUserId = userToShow!!.id,
                            reason = "User Report: $category",
                            description = "[$category]: \"$details\""
                        )
                    )
                    // Increment report count
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("Users").document(userToShow!!.id)
                        .update("reportCount", userToShow!!.reportCount + 1)
                    showReportUserDialog = false
                }
            )
        }

        if (showEnlargedImage) {
            EnlargedImageDialog(
                user = userToShow,
                onDismiss = { showEnlargedImage = false },
                onEdit = if (isMe) { {
                    showEnlargedImage = false
                    photoPickerLauncher.launch("image/*")
                } } else null
            )
        }

        if (showEditProfileDialog) {
            EditProfilePhoneDialog(
                currentPhone = currentUser?.phoneNumber ?: "",
                onDismiss = { showEditProfileDialog = false },
                onSave = { newPhone ->
                    userViewModel.updateProfile(currentUser?.name ?: "", currentUser?.email ?: "", newPhone)
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

        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = themeViewModel.themeState.value,
                onDismiss = { showThemeDialog = false },
                onSelect = {
                    themeViewModel.setTheme(it)
                    showThemeDialog = false
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
        onEditProfile: () -> Unit,
        onSelectTheme: () -> Unit,
        onNavigateToAdmin: () -> Unit,
        isMe: Boolean,
        onTaskClick: (Task) -> Unit = {},
        onReportUser: () -> Unit = {},
        onWriteReview: () -> Unit = {}
    ) {
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
                        brush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))),
                        shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (user != null) {
                            // Account Status Badge at the Top (Transparent color)
                            Surface(
                                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                                color = when(user.status) {
                                    com.example.taskgo.data.model.UserStatus.ACTIVE -> Color(0xFF4CAF50)
                                    com.example.taskgo.data.model.UserStatus.SUSPENDED -> Color(0xFFFF9800)
                                    com.example.taskgo.data.model.UserStatus.BANNED -> Color(0xFFF44336)
                                }.copy(alpha = 0.3f), // Transparent alpha
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = user.status.name,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            if (!isMe) {
                                IconButton(
                                    onClick = onReportUser,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Report, contentDescription = "Report User", tint = Color.White)
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.size(100.dp).align(Alignment.Center).clickable { onEnlargeImage() }.shadow(8.dp, CircleShape),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface
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
                                    Text(text = user?.name?.take(1) ?: "?", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally // CENTERING CONTENT
                    ) {
                        Text(
                            text = user?.name ?: "User", 
                            style = MaterialTheme.typography.headlineSmall, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = MaterialTheme.colorScheme.onSurface, 
                            textAlign = TextAlign.Center, // CENTERING TEXT
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = user?.email ?: "", 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, // CENTERING TEXT
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (user?.phoneNumber?.isNotBlank() == true) {
                            Text(
                                text = user.phoneNumber, 
                                style = MaterialTheme.typography.bodyMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center, // CENTERING TEXT
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isMe) {
                    Text("Account Settings", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileMenuButton(text = "Edit Phone Number", icon = Icons.Default.Phone, onClick = onEditProfile)
                    ProfileMenuButton(text = "Appearance (Dark/Light)", icon = Icons.Default.Brightness6, onClick = onSelectTheme)
                }

                val postedLabel = if (isMe) "My Posted Tasks History" else "${user?.name}'s Posted Tasks"
                val completedLabel = if (isMe) "Completed Tasks (Runner)" else "${user?.name}'s Completed Work"

                if (isMe) {
                    ProfileMenuButton(text = postedLabel, icon = Icons.Default.History, onClick = onNavigateToPosted)
                    ProfileMenuButton(text = completedLabel, icon = Icons.Default.CheckCircle, onClick = onNavigateToCompleted)
                    ProfileMenuButton(text = "Report App Issues", icon = Icons.Default.BugReport, onClick = onNavigateToReportIssue)
                } else {
                    // Show Tasks Inline for public profile
                    val allTasks = taskViewModel.allTasks.collectAsState().value
                    val postedTasks = allTasks.filter { it.requesterId == user?.id }
                    val completedTasks = allTasks.filter { it.runnerId == user?.id && it.status == TaskStatus.COMPLETED }

                    if (postedTasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(postedLabel, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        postedTasks.take(3).forEach { task ->
                            PostedTaskHistoryItem(
                                task = task, 
                                review = null, 
                                onClick = { onTaskClick(task) }, 
                                onReview = {}, 
                                onReport = {},
                                hideActions = true // Always hide on public profile summary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (postedTasks.size > 3) {
                            TextButton(onClick = onNavigateToPosted) { Text("See all ${postedTasks.size} tasks") }
                        }
                    }

                    if (completedTasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(completedLabel, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        completedTasks.take(3).forEach { task ->
                            CompletedTaskItem(task = task, review = null, onClick = { onTaskClick(task) })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (completedTasks.size > 3) {
                            TextButton(onClick = onNavigateToCompleted) { Text("See all ${completedTasks.size} completed tasks") }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Ratings & Reviews Section
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ratings & Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    if (!isMe) {
                        TextButton(onClick = onWriteReview) {
                            Icon(Icons.Default.RateReview, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Write Review", fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                val reviewsByModel = taskViewModel.allReviews.collectAsState().value.filter { it.revieweeId == user?.id }
                
                // Review Filtering & Sorting States
                var starFilter by remember { mutableIntStateOf(0) } // 0 = All
                var sortNewest by remember { mutableStateOf(true) }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Star Filter
                    var expandedFilter by remember { mutableStateOf(false) }
                    Box {
                        AssistChip(
                            onClick = { expandedFilter = true },
                            label = { Text(if (starFilter == 0) "All Stars" else "$starFilter Stars") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        DropdownMenu(expanded = expandedFilter, onDismissRequest = { expandedFilter = false }) {
                            DropdownMenuItem(text = { Text("All Stars") }, onClick = { starFilter = 0; expandedFilter = false })
                            (5 downTo 1).forEach { stars ->
                                DropdownMenuItem(text = { Text("$stars Stars") }, onClick = { starFilter = stars; expandedFilter = false })
                            }
                        }
                    }
                    
                    // Newest/Oldest Toggle
                    AssistChip(
                        onClick = { sortNewest = !sortNewest },
                        label = { Text(if (sortNewest) "Newest First" else "Oldest First") },
                        leadingIcon = { Icon(if (sortNewest) Icons.Default.Sort else Icons.Default.History, null, modifier = Modifier.size(16.dp)) }
                    )
                }
                
                val filteredReviews = remember(reviewsByModel, starFilter, sortNewest) {
                    reviewsByModel
                        .filter { starFilter == 0 || it.rating == starFilter }
                        .let { if (sortNewest) it.sortedByDescending { r -> r.timestamp } else it.sortedBy { r -> r.timestamp } }
                }

                if (filteredReviews.isEmpty()) {
                    Text("No reviews matching filters.", modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                } else {
                    val displayedReviews = if (isMe) filteredReviews.take(5) else filteredReviews
                    displayedReviews.forEach { review ->
                        ReviewItem(review)
                    }
                    if (isMe && filteredReviews.size > 5) {
                        TextButton(onClick = { /* Could navigate to full reviews page */ }) {
                            Text("See all ${filteredReviews.size} reviews")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (isMe) {
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSystemInDarkTheme()) Color(0xFF3B1E1E) else Color(0xFFFFF1F1),
                            contentColor = Color.Red
                        ),
                        border = if (!isSystemInDarkTheme()) BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)) else null
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
    fun ProfileMenuButton(
        text: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit,
        color: Color = MaterialTheme.colorScheme.primary
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
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
        val totalIncome = remember(tasks) { tasks.filter { it.status == TaskStatus.COMPLETED }.sumOf { it.paymentAmount } }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (title.contains("Completed", ignoreCase = true)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Income Earned", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("RM ${String.format(Locale.getDefault(), "%.2f", totalIncome)}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Text("No history found.", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(tasks) { task ->
                            val existingReview = reviews?.find { it.taskId == task.id && it.reviewerId != task.runnerId }
                            val isPostedHistory = title.contains("Posted", ignoreCase = true)

                            if (isPostedHistory) {
                                PostedTaskHistoryItem(
                                    task = task,
                                    review = existingReview,
                                    onClick = { onTaskClick(task) },
                                    onReview = { onReviewRunner?.invoke(task) },
                                    onReport = { onReportRunner?.invoke(task) },
                                    hideActions = (onReviewRunner == null)
                                )
                            } else {
                                val runnerReview = reviews?.find { it.taskId == task.id && it.revieweeId == task.runnerId }
                                CompletedTaskItem(
                                    task = task,
                                    review = runnerReview,
                                    onClick = { onTaskClick(task) },
                                    showDetails = true
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
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
        onReport: () -> Unit,
        hideActions: Boolean = false
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("RM %.2f".format(Locale.getDefault(), task.paymentAmount), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                }

                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = task.status.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (task.status == TaskStatus.COMPLETED && task.runnerId != null && !hideActions) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "Task completed by: ${task.runnerName ?: "User"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (review != null) {
                        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { i ->
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = if (i < review.rating) Color(0xFFFFB300) else Color.LightGray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(review.comment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
    fun CompletedTaskItem(task: Task, review: Review?, onClick: () -> Unit, showDetails: Boolean = false) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (showDetails) {
                        Text("RM %.2f".format(Locale.getDefault(), task.paymentAmount), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Completed", color = Color(0xFF43A047), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (showDetails) {
                        Text(" • Posted by ${task.requesterName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                review?.let {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text("Your review:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = if (i < it.rating) Color(0xFFFFB300) else Color.LightGray)
                        }
                    }
                    Text(it.comment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun ReportUserDialog(user: com.example.taskgo.data.model.User, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
        var selectedCategory by remember { mutableStateOf("Scam") }
        var details by remember { mutableStateOf("") }
        val categories = listOf("Scam", "Bullying", "Fraud", "Unprofessional", "Others")

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Report User: ${user.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select a reason for this user report:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Provide details...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirm(selectedCategory, details) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = details.isNotBlank()
                ) { Text("Submit User Report") }
            },
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

    @Composable
    fun ThemeSelectionDialog(currentTheme: AppTheme, onDismiss: () -> Unit, onSelect: (AppTheme) -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Appearance", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AppTheme.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(theme) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = theme == currentTheme, onClick = { onSelect(theme) })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when(theme) {
                                    AppTheme.LIGHT -> "Light Mode"
                                    AppTheme.DARK -> "Dark Mode"
                                    AppTheme.SYSTEM -> "Follow System (Default)"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }
    @Composable
    fun ReviewItem(review: Review) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (i < review.rating) Color(0xFFFFB300) else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val date = remember(review.timestamp) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(review.timestamp))
                    }
                    Text(date, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (review.comment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(review.comment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ReportIssuePage(taskViewModel: TaskViewModel, user: com.example.taskgo.data.model.User?, onBack: () -> Unit) {
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
                Button(
                    onClick = {
                        taskViewModel.addReport(Report(
                            reporterId = user?.id ?: "",
                            reason = category,
                            description = reason
                        ))
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000)),
                    enabled = reason.isNotBlank()
                ) { Text("Submit Report") }
            }
        }
    }

    @Composable
    fun EnlargedImageDialog(user: com.example.taskgo.data.model.User?, onDismiss: () -> Unit, onEdit: (() -> Unit)? = null) {
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
                    if (onEdit != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = onEdit, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("Change Picture") }
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
