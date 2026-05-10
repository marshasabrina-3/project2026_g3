package com.example.taskgo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskgo.data.model.Task
import com.example.taskgo.ui.viewmodel.ChatSummary
import com.example.taskgo.ui.viewmodel.ChatViewModel
import com.example.taskgo.ui.viewmodel.TaskViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainContainerScreen(
    userViewModel: UserViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val taskViewModel: TaskViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    
    val currentUser by userViewModel.currentUser.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val activeChats by chatViewModel.activeChats.collectAsState()
    
    var activeChatParams by remember { mutableStateOf<ChatParams?>(null) }
    var showInbox by remember { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(currentUser) {
        currentUser?.id?.let { chatViewModel.listenForInbox(it) }
    }

    val totalUnreadCount = remember(activeChats, currentUser) {
        activeChats.sumOf { chat ->
            if (chat.requesterId == currentUser?.id) chat.unreadCountRequester else chat.unreadCountRunner
        }
    }

    val utmMaroon = Color(0xFF800000)

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (selectedTaskForDetail != null) {
            val currentTask = allTasks.find { it.id == selectedTaskForDetail?.id } ?: selectedTaskForDetail!!
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
                    activeChatParams = ChatParams(
                        taskId = currentTask.id,
                        taskTitle = currentTask.title,
                        otherUserId = otherId,
                        isRequester = currentTask.requesterId == currentUser?.id,
                        runnerId = if (currentTask.requesterId == currentUser?.id) otherId else currentUser?.id ?: ""
                    )
                }
            )
        } else if (activeChatParams != null) {
            ChatScreen(
                chatViewModel = chatViewModel,
                userViewModel = userViewModel,
                otherUserId = activeChatParams!!.otherUserId,
                taskId = activeChatParams!!.taskId,
                taskTitle = activeChatParams!!.taskTitle,
                onBack = { activeChatParams = null },
                onNavigateToTask = { taskId ->
                    val task = allTasks.find { it.id == taskId }
                    if (task != null) {
                        selectedTaskForDetail = task
                        activeChatParams = null
                    }
                }
            )
        } else {
            Scaffold(
                containerColor = Color.White,
                bottomBar = {
                    Surface(tonalElevation = 12.dp, shadowElevation = 12.dp, color = Color.White) {
                        NavigationBar(containerColor = Color.White, modifier = Modifier.height(72.dp)) {
                            val tabs = listOf(Triple(0, Icons.Default.Home, "Home"), Triple(1, Icons.Default.AddCircle, "Post"), Triple(2, Icons.Default.Person, "Profile"))
                            tabs.forEach { (index, icon, label) ->
                                val selected = selectedTab == index
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = index },
                                    icon = { Icon(icon, label, modifier = Modifier.size(26.dp)) },
                                    label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = utmMaroon, selectedTextColor = utmMaroon, indicatorColor = utmMaroon.copy(0.1f), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                val modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                when (selectedTab) {
                    0 -> MarketplaceScreen(taskViewModel, userViewModel, onChat = { tid, uid, title -> 
                        val task = allTasks.find { it.id == tid }
                        activeChatParams = ChatParams(tid, title, uid, task?.requesterId == currentUser?.id, if (task?.requesterId == currentUser?.id) uid else currentUser?.id ?: "")
                    }, modifier = modifier)
                    1 -> SearchPostScreen(taskViewModel, userViewModel, onChat = { tid, uid, title -> 
                        val task = allTasks.find { it.id == tid }
                        activeChatParams = ChatParams(tid, title, uid, task?.requesterId == currentUser?.id, if (task?.requesterId == currentUser?.id) uid else currentUser?.id ?: "")
                    }, modifier = modifier)
                    2 -> ProfileScreen(userViewModel, taskViewModel, onLogout = onLogout, modifier = modifier)
                }
            }

            if (selectedTab != 2) {
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 100.dp)) {
                    FloatingActionButton(onClick = { showInbox = true }, containerColor = utmMaroon, contentColor = Color.White, shape = CircleShape) {
                        BadgedBox(badge = { if (totalUnreadCount > 0) Badge(containerColor = Color.Red) { Text("$totalUnreadCount") } }) {
                            Icon(Icons.AutoMirrored.Filled.Message, "Inbox")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = showInbox, enter = slideInVertically(initialOffsetY = { it }), exit = slideOutVertically(targetOffsetY = { it })) {
            InboxOverlay(
                currentUserId = currentUser?.id ?: "",
                activeChats = activeChats,
                taskViewModel = taskViewModel,
                onChatSelect = { summary ->
                    val otherId = if (summary.requesterId == currentUser?.id) summary.runnerId else summary.requesterId
                    activeChatParams = ChatParams(summary.taskId, summary.taskTitle, otherId, summary.requesterId == currentUser?.id, summary.runnerId)
                    showInbox = false
                },
                onClose = { showInbox = false }
            )
        }
    }
}

@Composable
fun InboxOverlay(currentUserId: String, activeChats: List<ChatSummary>, taskViewModel: TaskViewModel, onChatSelect: (ChatSummary) -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(0.4f)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f).clickable { onClose() })
            Card(modifier = Modifier.fillMaxWidth().height(550.dp), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Your Messages", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF800000))
                        IconButton(onClick = onClose, modifier = Modifier.background(Color(0xFFF5F5F5), CircleShape)) { Icon(Icons.Default.Close, null) }
                    }
                    if (activeChats.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No messages yet.", color = Color.Gray) }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(activeChats) { chat ->
                                InboxItem(chat, currentUserId, taskViewModel, onClick = { onChatSelect(chat) })
                                HorizontalDivider(color = Color.LightGray.copy(0.2f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InboxItem(chat: ChatSummary, currentUserId: String, taskViewModel: TaskViewModel, onClick: () -> Unit) {
    val otherId = if (chat.requesterId == currentUserId) chat.runnerId else chat.requesterId
    val otherName = if (chat.requesterId == currentUserId) chat.runnerName else chat.requesterName
    val unreadCount = if (chat.requesterId == currentUserId) chat.unreadCountRequester else chat.unreadCountRunner
    
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = Color(0xFFF5F5F5)) {
            Box(contentAlignment = Alignment.Center) { Text(otherName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF800000), fontSize = 20.sp) }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                NameWithRating(
                    name = otherName.ifBlank { "User" },
                    rating = taskViewModel.getUserRating(otherId),
                    fontSize = 15.sp,
                    fontWeight = if (unreadCount > 0) FontWeight.ExtraBold else FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.timestamp))
                Text(time, fontSize = 11.sp, color = if (unreadCount > 0) Color(0xFF800000) else Color.Gray)
            }
            Text(chat.taskTitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF800000), fontWeight = FontWeight.Bold, maxLines = 1)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(chat.lastMessage, style = MaterialTheme.typography.bodySmall, color = if (unreadCount > 0) Color.Black else Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (unreadCount > 0) {
                    Surface(color = Color.Red, shape = CircleShape, modifier = Modifier.size(20.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("$unreadCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                    }
                }
            }
        }
    }
}

data class ChatParams(val taskId: String, val taskTitle: String, val otherUserId: String, val isRequester: Boolean, val runnerId: String)
