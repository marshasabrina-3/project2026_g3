package com.example.taskgo.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.taskgo.ui.viewmodel.ChatViewModel
import com.example.taskgo.ui.viewmodel.UserViewModel
import com.example.taskgo.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    userViewModel: UserViewModel,
    otherUserId: String,
    taskId: String,
    taskTitle: String,
    onBack: () -> Unit,
    onNavigateToTask: (String) -> Unit
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val isUploading by chatViewModel.isUploading.collectAsState()
    val allChats by chatViewModel.activeChats.collectAsState()
    
    val currentSummary = allChats.find { it.taskId == taskId && (it.runnerId == currentUser?.id || it.requesterId == currentUser?.id) }
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isSenderRequester = currentSummary?.requesterId == currentUser?.id

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            chatViewModel.uploadImage(
                uri = it,
                senderId = currentUser?.id ?: "",
                senderName = currentUser?.name ?: "User",
                receiverId = otherUserId,
                receiverName = if (isSenderRequester) (currentSummary?.runnerName ?: "Runner") else (currentSummary?.requesterName ?: "Requester"),
                taskId = taskId,
                taskTitle = taskTitle,
                isRequester = isSenderRequester
            ) 
        }
    }

    val paymentProofLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            chatViewModel.uploadImage(
                uri = it,
                senderId = currentUser?.id ?: "",
                senderName = currentUser?.name ?: "User",
                receiverId = otherUserId,
                receiverName = if (isSenderRequester) (currentSummary?.runnerName ?: "Runner") else (currentSummary?.requesterName ?: "Requester"),
                taskId = taskId,
                taskTitle = taskTitle,
                isRequester = isSenderRequester,
                isPaymentProof = true
            ) 
        }
    }

    LaunchedEffect(otherUserId, taskId, currentUser?.id) {
        currentUser?.id?.let { uid ->
            val runnerId = if (isSenderRequester) otherUserId else uid
            chatViewModel.listenForMessages(taskId, runnerId, uid)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size + 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Chat", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = taskTitle, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToTask(taskId) }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF800000))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                if (messages.isNotEmpty()) {
                    item {
                        TaskReferenceHeader(taskTitle = taskTitle, onClick = { onNavigateToTask(taskId) })
                    }
                }

                items(messages) { message ->
                    val isMe = message.senderId == currentUser?.id
                    ChatBubble(
                        text = message.text,
                        imageUrl = message.imageUrl,
                        timestamp = message.timestamp,
                        isMe = isMe,
                        isPaymentProof = message.isPaymentProof
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSenderRequester) {
                        IconButton(onClick = { paymentProofLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Payments, contentDescription = "Send Payment Proof", tint = Color(0xFF43A047))
                        }
                    }
                    IconButton(onClick = { imageLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Send Image", tint = Color.Gray)
                    }
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && currentUser != null) {
                                chatViewModel.sendMessage(
                                    senderId = currentUser!!.id,
                                    senderName = currentUser!!.name,
                                    receiverId = otherUserId,
                                    receiverName = if (isSenderRequester) (currentSummary?.runnerName ?: "Runner") else (currentSummary?.requesterName ?: "Requester"),
                                    taskId = taskId,
                                    taskTitle = taskTitle,
                                    isSenderRequester = isSenderRequester,
                                    text = messageText
                                )
                                messageText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF800000), contentColor = Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun TaskReferenceHeader(taskTitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFF800000), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Discussing task:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(taskTitle, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
fun ChatBubble(
    text: String,
    imageUrl: String?,
    timestamp: Long,
    isMe: Boolean,
    isPaymentProof: Boolean = false
) {
    val backgroundColor = if (isPaymentProof) Color(0xFFE8F5E9) else if (isMe) Color(0xFF800000) else Color(0xFFF0F0F0)
    val contentColor = if (isPaymentProof) Color(0xFF2E7D32) else if (isMe) Color.White else Color.Black
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = backgroundColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isPaymentProof) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(Icons.Default.Verified, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PAYMENT PROOF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
                if (!imageUrl.isNullOrEmpty()) {
                    val imageBytes = remember(imageUrl) { ImageUtils.decodeBase64ToByteArray(imageUrl) }
                    AsyncImage(
                        model = imageBytes,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (text.isNotBlank()) {
                    Text(text = text, color = contentColor, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                Text(
                    text = time,
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
