package com.example.taskgo.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import java.util.*

@Composable
fun SearchPostScreen(
    taskViewModel: TaskViewModel,
    userViewModel: UserViewModel,
    onChat: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var screenState by remember { mutableStateOf("MAIN") }
    val user by userViewModel.currentUser.collectAsState()
    var selectedTaskForDetail by remember { mutableStateOf<Task?>(null) }
    val isPosting by taskViewModel.isPosting.collectAsState()

    val utmMaroon = Color(0xFF800000)

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFAFAFA))) {
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
                }
            )
        } else if (screenState == "MAIN") {
            PostMainScreen(
                taskViewModel = taskViewModel,
                user = user,
                onCreateRequest = { screenState = "CREATE_REQUEST" },
                onCreateService = { screenState = "CREATE_SERVICE" },
                onTaskClick = { selectedTaskForDetail = it }
            )
        } else {
            BackHandler { screenState = "MAIN" }
            CreateTaskScreen(
                type = if (screenState == "CREATE_REQUEST") TaskType.REQUEST else TaskType.SERVICE,
                onBack = { screenState = "MAIN" },
                onConfirm = { title, desc, cat, campus, addr, dead, amt, images ->
                    taskViewModel.addTask(
                        title = title,
                        description = desc,
                        category = cat,
                        type = if (screenState == "CREATE_REQUEST") TaskType.REQUEST else TaskType.SERVICE,
                        campus = campus,
                        address = addr,
                        deadline = dead,
                        paymentAmount = amt ?: 0.0,
                        requesterId = user?.id ?: "unknown",
                        requesterName = user?.name ?: "Unknown",
                        imageUris = images
                    )
                    screenState = "MAIN"
                }
            )
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = utmMaroon, strokeWidth = 4.dp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Creating Task...", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Uploading images to cloud", fontSize = 12.sp, color = Color.Gray)
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
    var requestsExpanded by remember { mutableStateOf(true) }
    var servicesExpanded by remember { mutableStateOf(false) }
    var applicationsRequestExpanded by remember { mutableStateOf(false) }
    var applicationsServiceExpanded by remember { mutableStateOf(false) }
    val utmMaroon = Color(0xFF800000)

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
                val myRequests = allTasks.filter { it.requesterId == user?.id && it.type == TaskType.REQUEST && it.status != TaskStatus.COMPLETED }
                ExpandableListSection("Requested Tasks", myRequests.size, requestsExpanded, { requestsExpanded = !requestsExpanded }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (myRequests.isEmpty()) EmptyHistoryItem("No live requests.")
                        else myRequests.forEach { CompactHistoryItem(it, { onTaskClick(it) }) }
                    }
                }
            }
            item {
                val myServices = allTasks.filter { it.requesterId == user?.id && it.type == TaskType.SERVICE && it.status != TaskStatus.COMPLETED }
                ExpandableListSection("Service Offers", myServices.size, servicesExpanded, { servicesExpanded = !servicesExpanded }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (myServices.isEmpty()) EmptyHistoryItem("No live services.")
                        else myServices.forEach { CompactHistoryItem(it, { onTaskClick(it) }) }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Applications Sent
            item { SectionLabel("Applications Sent") }
            item {
                val appliedRequests = allTasks.filter { (it.interestedRunnerIds.contains(user?.id) || it.runnerId == user?.id) && it.type == TaskType.REQUEST }
                ExpandableListSection("Applications (Requests)", appliedRequests.size, applicationsRequestExpanded, { applicationsRequestExpanded = !applicationsRequestExpanded }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (appliedRequests.isEmpty()) EmptyHistoryItem("No applications sent for requests.")
                        else appliedRequests.forEach { task ->
                            val isChosen = task.runnerId == user?.id
                            val isRejected = task.runnerId != null && task.runnerId != user?.id
                            CompactHistoryItem(task, { onTaskClick(task) }, if (isChosen) "ACCEPTED" else if (isRejected) "REJECTED" else "PENDING")
                        }
                    }
                }
            }
            item {
                val appliedServices = allTasks.filter { (it.interestedRunnerIds.contains(user?.id) || it.runnerId == user?.id) && it.type == TaskType.SERVICE }
                ExpandableListSection("Applications (Services)", appliedServices.size, applicationsServiceExpanded, { applicationsServiceExpanded = !applicationsServiceExpanded }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (appliedServices.isEmpty()) EmptyHistoryItem("No applications sent for services.")
                        else appliedServices.forEach { task ->
                            val isChosen = task.runnerId == user?.id
                            val isRejected = task.runnerId != null && task.runnerId != user?.id
                            CompactHistoryItem(task, { onTaskClick(task) }, if (isChosen) "ACCEPTED" else if (isRejected) "REJECTED" else "PENDING")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
fun PostTypeCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.height(140.dp).shadow(4.dp, RoundedCornerShape(24.dp)).clickable { onClick() },
        color = Color.White, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = color.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(24.dp)) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
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
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (count > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = Color(0xFF800000), shape = CircleShape) {
                        Text("$count", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
        }
        AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) { content() }
    }
}

@Composable
fun CompactHistoryItem(task: Task, onClick: () -> Unit, customStatus: String? = null) {
    val statusColor = when (customStatus ?: task.status.name) {
        "ACCEPTED", "COMPLETED" -> Color(0xFF2E7D32)
        "REJECTED", "CANCELLED" -> Color(0xFFD32F2F)
        "PENDING", "OPEN", "WAITING_VERIFICATION" -> Color(0xFFFF9800)
        else -> Color.Gray
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = Color(0xFFF5F5F5)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (task.type == TaskType.REQUEST) Icons.Default.PersonSearch else Icons.Default.Storefront, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = customStatus ?: task.status.name.lowercase().replaceFirstChar { it.uppercase() }, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("RM %.2f".format(Locale.getDefault(), task.paymentAmount), fontWeight = FontWeight.Black, color = Color(0xFF800000), fontSize = 14.sp)
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
    onBack: () -> Unit,
    onConfirm: (String, String, TaskCategory, String, String, String, Double?, List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var campus by remember { mutableStateOf("UTMKL") }
    var address by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    
    var hasPrice by remember { mutableStateOf(true) }
    var hasDeadline by remember { mutableStateOf(true) }
    
    var selectedCategory by remember { mutableStateOf(TaskCategory.GENERAL) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) selectedImages = (selectedImages + uris).take(7)
    }

    fun showDateTimePicker() {
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                deadline = "%02d/%02d/%d %02d:%02d".format(day, month + 1, year, hour, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (type == TaskType.REQUEST) "Post Request" else "Offer Service", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
                Text("Attachments (${selectedImages.size}/7)", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selectedImages.size < 7) {
                        item {
                            Surface(modifier = Modifier.size(90.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFFF5F5F5), border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))) {
                                Box(modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) { Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray) }
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

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), placeholder = { Text("E.g. Help buy food, Carpool to JB...") })

            Column {
                Text("Category", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskCategory.entries.forEach { cat ->
                        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
            }

            Column {
                Text("Location", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { campus = "UTMKL" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (campus == "UTMKL") Color(0xFF800000) else Color(0xFFF5F5F5), contentColor = if (campus == "UTMKL") Color.White else Color.Gray)) { Text("UTMKL") }
                    Button(onClick = { campus = "UTMJB" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (campus == "UTMJB") Color(0xFF800000) else Color(0xFFF5F5F5), contentColor = if (campus == "UTMJB") Color.White else Color.Gray)) { Text("UTMJB") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Detailed Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }

            // Toggles for Price and Deadline (Service only)
            if (type == TaskType.SERVICE) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Include Price?", fontWeight = FontWeight.Medium)
                    Switch(checked = hasPrice, onCheckedChange = { hasPrice = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF800000)))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Include Deadline?", fontWeight = FontWeight.Medium)
                    Switch(checked = hasDeadline, onCheckedChange = { hasDeadline = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF800000)))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (hasPrice) {
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Price (RM)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), prefix = { Text("RM ") })
                }
                if (hasDeadline) {
                    OutlinedTextField(value = deadline, onValueChange = { }, label = { Text("Deadline") }, modifier = Modifier.weight(1.5f).clickable { showDateTimePicker() }, readOnly = true, shape = RoundedCornerShape(12.dp), trailingIcon = { Icon(Icons.Default.CalendarMonth, null) })
                }
            }

            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = RoundedCornerShape(12.dp))

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp) }

            Button(
                onClick = {
                    val amt = if (hasPrice) amount.toDoubleOrNull() ?: 0.0 else 0.0
                    val finalDeadline = if (hasDeadline) deadline else ""
                    if (title.isBlank() || desc.isBlank() || address.isBlank()) errorMessage = "Please fill all required fields"
                    else onConfirm(title, desc, selectedCategory, campus, address, finalDeadline, amt, selectedImages)
                }, 
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))
            ) { Text("Publish Task", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
