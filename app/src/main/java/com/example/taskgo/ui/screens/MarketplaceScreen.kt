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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskgo.R
import com.example.taskgo.data.model.Task
import com.example.taskgo.data.model.TaskCategory
import com.example.taskgo.data.model.TaskStatus
import com.example.taskgo.data.model.TaskType
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    taskViewModel: TaskViewModel,
    userViewModel: UserViewModel,
    modifier: Modifier = Modifier
) {
    val filteredTasks by taskViewModel.filteredTasks.collectAsState(initial = emptyList())
    val searchQuery by taskViewModel.searchQuery.collectAsState()
    val selectedCategory by taskViewModel.selectedCategory.collectAsState()
    val sortOption by taskViewModel.sortOption.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<Task?>(null) }
    
    // Local filters for Location
    var selectedCampus by remember { mutableStateOf<String?>(null) } // null = Both, "UTMKL", "UTMJB"

    val displayedTasks = filteredTasks.filter { task ->
        selectedCampus == null || task.campus == selectedCampus
    }

    val utmMaroon = Color(0xFF800000)

    if (selectedTaskForDetail == null) {
        Column(modifier = modifier.fillMaxSize().background(Color(0xFFFAFAFA))) {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(listOf(utmMaroon, Color(0xFFB30000))),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 32.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Explore Tasks",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Find or offer services in UTM",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .size(60.dp)
                                .shadow(8.dp, CircleShape),
                            shape = CircleShape,
                            color = Color.White
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.utmlogo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Enhanced Search Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = utmMaroon)
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { taskViewModel.onSearchQueryChange(it) },
                                placeholder = { Text("What are you looking for?") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(Icons.Default.Tune, contentDescription = "Filter", tint = utmMaroon)
                            }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = utmMaroon)
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    TaskViewModel.SortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    when (option) {
                                                        TaskViewModel.SortOption.LATEST -> "Newest First"
                                                        TaskViewModel.SortOption.ALPHA_ASC -> "A - Z"
                                                        TaskViewModel.SortOption.ALPHA_DESC -> "Z - A"
                                                        TaskViewModel.SortOption.PRICE_LOW_HIGH -> "Price: Low to High"
                                                        TaskViewModel.SortOption.PRICE_HIGH_LOW -> "Price: High to Low"
                                                    }
                                                )
                                            },
                                            onClick = {
                                                taskViewModel.onSortOptionChange(option)
                                                showSortMenu = false
                                            },
                                            leadingIcon = {
                                                if (sortOption == option) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = utmMaroon)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                // Category Selector
                Text(
                    "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        CategoryChip(
                            label = "All",
                            isSelected = selectedCategory == null,
                            icon = Icons.Default.AllInclusive,
                            onClick = { taskViewModel.onCategoryChange(null) }
                        )
                    }
                    items(TaskCategory.entries) { cat ->
                        CategoryChip(
                            label = cat.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            isSelected = selectedCategory == cat,
                            icon = when(cat) {
                                TaskCategory.FOOD_DELIVERY -> Icons.Default.Restaurant
                                TaskCategory.CARPOOL -> Icons.Default.DirectionsCar
                                TaskCategory.PRINTING -> Icons.Default.Print
                                else -> Icons.Default.Category
                            },
                            onClick = { taskViewModel.onCategoryChange(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campus Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCampus == null,
                        onClick = { selectedCampus = null },
                        label = { Text("Both Campus") },
                        leadingIcon = if (selectedCampus == null) { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                    )
                    FilterChip(
                        selected = selectedCampus == "UTMKL",
                        onClick = { selectedCampus = "UTMKL" },
                        label = { Text("UTMKL") }
                    )
                    FilterChip(
                        selected = selectedCampus == "UTMJB",
                        onClick = { selectedCampus = "UTMJB" },
                        label = { Text("UTMJB") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Task Lists
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    val requests = displayedTasks.filter { it.type == TaskType.REQUEST }
                    val services = displayedTasks.filter { it.type == TaskType.SERVICE }
                    
                    if (requests.isNotEmpty()) {
                        item {
                            MarketplaceSection(title = "Requests", tasks = requests, onClick = { selectedTaskForDetail = it })
                        }
                    }
                    if (services.isNotEmpty()) {
                        item {
                            MarketplaceSection(title = "Services", tasks = services, onClick = { selectedTaskForDetail = it })
                        }
                    }
                    
                    if (requests.isEmpty() && services.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("No tasks match your filters.", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    } else {
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
                    taskViewModel.updateTask(currentTask.copy(status = TaskStatus.PENDING_APPROVAL))
                }
            },
            onReport = { /* Handle report */ },
            modifier = modifier
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(taskViewModel = taskViewModel, onDismiss = { showFilterSheet = false })
    }
}

@Composable
fun CategoryChip(label: String, isSelected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val utmMaroon = Color(0xFF800000)
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(16.dp)),
        color = if (isSelected) utmMaroon else Color.White,
        border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) Color.White else utmMaroon
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun MarketplaceSection(title: String, tasks: List<Task>, onClick: (Task) -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    val utmMaroon = Color(0xFF800000)
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title (${tasks.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = utmMaroon
            )
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = utmMaroon
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                tasks.forEach { task ->
                    ModernTaskItem(task = task, onClick = { onClick(task) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTaskItem(task: Task, onClick: () -> Unit) {
    val utmMaroon = Color(0xFF800000)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = utmMaroon.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = task.category.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = utmMaroon,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = if (task.type == TaskType.REQUEST) Color(0xFF2196F3).copy(alpha = 0.1f) else Color(0xFFFF9800).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = task.type.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.type == TaskType.REQUEST) Color(0xFF2196F3) else Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "RM ${String.format(Locale.getDefault(), "%.2f", task.paymentAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = utmMaroon,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${task.campus} • ${task.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = Color.LightGray) {
                     Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(4.dp), tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "By: ${task.requesterId}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "View Details",
                    style = MaterialTheme.typography.labelLarge,
                    color = utmMaroon,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = utmMaroon, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(taskViewModel: TaskViewModel, onDismiss: () -> Unit) {
    val priceRange by taskViewModel.priceRange.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text("Filter by Price", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF800000))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Price Range: RM ${String.format(Locale.getDefault(), "%.2f", priceRange.start)} - ${if (priceRange.endInclusive >= 100.0) "100+" else String.format(Locale.getDefault(), "%.2f", priceRange.endInclusive)}", style = MaterialTheme.typography.titleMedium)
            RangeSlider(
                value = priceRange.start.toFloat()..priceRange.endInclusive.toFloat(),
                onValueChange = { taskViewModel.onPriceRangeChange(it.start.toDouble()..it.endInclusive.toDouble()) },
                valueRange = 0.2f..100f,
                steps = 0,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF800000), activeTrackColor = Color(0xFF800000))
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))
            ) { Text("Apply") }
        }
    }
}
