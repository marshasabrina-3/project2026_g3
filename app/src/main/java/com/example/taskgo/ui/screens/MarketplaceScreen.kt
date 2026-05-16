package com.example.taskgo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.taskgo.R
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskCategory
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.data.model.TaskType
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import com.example.taskgo.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MarketplaceScreen(
    taskViewModel: TaskViewModel,
    userViewModel: UserViewModel,
    onChat: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by taskViewModel.searchQuery.collectAsState()
    val selectedCategory by taskViewModel.selectedCategory.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<Task?>(null) }
    var selectedCampus by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<TaskType?>(null) }

    val displayedTasks = remember(allTasks, searchQuery, selectedCategory, selectedCampus, selectedType) {
        allTasks.filter { task ->
            val isLive = task.status == TaskStatus.OPEN
            val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) || 
                               task.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || task.category == selectedCategory
            val matchesCampus = selectedCampus == null || task.campus == selectedCampus
            val matchesType = selectedType == null || task.type == selectedType
            
            isLive && matchesSearch && matchesCategory && matchesCampus && matchesType
        }
    }

    val utmMaroon = Color(0xFF800000)

    if (selectedTaskForDetail != null) {
        val allTasks by taskViewModel.allTasks.collectAsState()
        val currentTask = allTasks.find { it.id == selectedTaskForDetail?.id } ?: selectedTaskForDetail!!
        val currentUser by userViewModel.currentUser.collectAsState()

        BackHandler { selectedTaskForDetail = null }

        TaskDetailScreen(
            task = currentTask,
            userViewModel = userViewModel,
            taskViewModel = taskViewModel,
            onBack = { selectedTaskForDetail = null },
            onAccept = {
                currentUser?.id?.let { runnerId ->
                    taskViewModel.applyForTask(currentTask.id, runnerId)
                }
            },
            onReport = { /* Handle report */ },
            onChat = { otherId, title -> 
                onChat(currentTask.id, otherId, title)
            },
            modifier = modifier
        )
    } else {
        Column(modifier = modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
            // Header Section - Lowered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(listOf(utmMaroon, Color(0xFFB30000))),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(horizontal = 20.dp)
                    .padding(top = 48.dp, bottom = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Explore Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Find or provide services", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        }
                        Surface(modifier = Modifier.size(45.dp).shadow(4.dp, CircleShape), shape = CircleShape, color = Color.White) {
                            Image(painter = painterResource(id = R.drawable.utmlogo), contentDescription = "Logo", modifier = Modifier.fillMaxSize().padding(6.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color.White) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = utmMaroon, modifier = Modifier.size(20.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { taskViewModel.onSearchQueryChange(it) },
                                placeholder = { Text("What are you looking for?") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                                singleLine = true
                            )
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = utmMaroon, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(Icons.Default.Tune, contentDescription = "Filter", tint = utmMaroon, modifier = Modifier.size(20.dp))
                            }
                            
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                TaskViewModel.SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(when(option) {
                                            TaskViewModel.SortOption.LATEST -> "Newest First"
                                            TaskViewModel.SortOption.ALPHA_ASC -> "A - Z"
                                            TaskViewModel.SortOption.ALPHA_DESC -> "Z - A"
                                            TaskViewModel.SortOption.PRICE_HIGH_LOW -> "Highest Price"
                                            TaskViewModel.SortOption.PRICE_LOW_HIGH -> "Lowest Price"
                                        }) },
                                        onClick = {
                                            taskViewModel.onSortOptionChange(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dropdown Filters
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var typeExpanded by remember { mutableStateOf(false) }
                var categoryExpanded by remember { mutableStateOf(false) }
                var campusExpanded by remember { mutableStateOf(false) }

                // Type Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    FilterDropdown(
                        label = "Type",
                        selected = when(selectedType) {
                            TaskType.REQUEST -> "Requests"
                            TaskType.SERVICE -> "Services"
                            else -> "All Tasks"
                        },
                        onClick = { typeExpanded = true }
                    )
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Tasks") }, onClick = { selectedType = null; typeExpanded = false })
                        DropdownMenuItem(text = { Text("Requests") }, onClick = { selectedType = TaskType.REQUEST; typeExpanded = false })
                        DropdownMenuItem(text = { Text("Services") }, onClick = { selectedType = TaskType.SERVICE; typeExpanded = false })
                    }
                }

                // Category Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    FilterDropdown(
                        label = "Category",
                        selected = selectedCategory?.name?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All",
                        onClick = { categoryExpanded = true }
                    )
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Categories") }, onClick = { taskViewModel.onCategoryChange(null); categoryExpanded = false })
                        TaskCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { taskViewModel.onCategoryChange(cat); categoryExpanded = false }
                            )
                        }
                    }
                }

                // Campus Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    FilterDropdown(
                        label = "Campus",
                        selected = selectedCampus ?: "Both",
                        onClick = { campusExpanded = true }
                    )
                    DropdownMenu(expanded = campusExpanded, onDismissRequest = { campusExpanded = false }) {
                        DropdownMenuItem(text = { Text("Both Campus") }, onClick = { selectedCampus = null; campusExpanded = false })
                        DropdownMenuItem(text = { Text("UTMKL") }, onClick = { selectedCampus = "UTMKL"; campusExpanded = false })
                        DropdownMenuItem(text = { Text("UTMJB") }, onClick = { selectedCampus = "UTMJB"; campusExpanded = false })
                    }
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

            // Task List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (displayedTasks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No tasks available", color = Color.Gray)
                        }
                    }
                } else {
                    val requests = displayedTasks.filter { it.type == TaskType.REQUEST }
                    val services = displayedTasks.filter { it.type == TaskType.SERVICE }

                    if (requests.isNotEmpty()) {
                        item { SectionHeader("Requests", utmMaroon) }
                        items(requests) { task ->
                            ModernTaskItem(task = task, taskViewModel = taskViewModel, onClick = { selectedTaskForDetail = task })
                        }
                    }

                    if (services.isNotEmpty()) {
                        if (requests.isNotEmpty()) {
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                        item { SectionHeader("Services", utmMaroon) }
                        items(services) { task ->
                            ModernTaskItem(task = task, taskViewModel = taskViewModel, onClick = { selectedTaskForDetail = task })
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(taskViewModel = taskViewModel, onDismiss = { showFilterSheet = false })
    }
}

@Composable
fun FilterDropdown(label: String, selected: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color(0xFF800000),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = selected,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ModernTaskItem(task: Task, taskViewModel: TaskViewModel, onClick: () -> Unit) {
    val utmMaroon = Color(0xFF800000)
    val formattedPrice = "RM %.2f".format(Locale.getDefault(), task.paymentAmount)
    val postDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(task.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Task Image Thumbnail
            Box(
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (task.images.isNotEmpty()) {
                    val imageBytes = remember(task.images.first()) { ImageUtils.decodeBase64ToByteArray(task.images.first()) }
                    AsyncImage(
                        model = imageBytes,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(30.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = task.category.name.replace("_", " "), fontSize = 9.sp, color = utmMaroon, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = task.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = formattedPrice, fontSize = 14.sp, color = utmMaroon, fontWeight = FontWeight.ExtraBold)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                    Text(text = " ${task.campus}", fontSize = 10.sp, color = Color.Gray)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "By: ", fontSize = 10.sp, color = Color.Gray)
                    NameWithRating(
                        name = task.requesterName.ifBlank { "User" },
                        rating = taskViewModel.getUserRating(task.requesterId),
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Deadline and Post Date Segment
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                if (task.deadline.isNotBlank()) {
                    Text(text = "Deadline:", fontSize = 8.sp, color = Color.Gray)
                    Text(text = task.deadline, fontSize = 9.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Posted on: $postDate", fontSize = 8.sp, color = Color.LightGray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(taskViewModel: TaskViewModel, onDismiss: () -> Unit) {
    val priceRange by taskViewModel.priceRange.collectAsState()
    val formattedRange = "Price: RM %.2f - %s".format(
        Locale.getDefault(), 
        priceRange.start, 
        if (priceRange.endInclusive >= 100.0) "100+" else "%.2f".format(Locale.getDefault(), priceRange.endInclusive)
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text("Filter by Price", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = formattedRange)
            RangeSlider(
                value = priceRange.start.toFloat()..priceRange.endInclusive.toFloat(),
                onValueChange = { taskViewModel.onPriceRangeChange(it.start.toDouble()..it.endInclusive.toDouble()) },
                valueRange = 0.2f..100f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF800000), activeTrackColor = Color(0xFF800000))
            )
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 24.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))) { Text("Apply Filters") }
        }
    }
}
