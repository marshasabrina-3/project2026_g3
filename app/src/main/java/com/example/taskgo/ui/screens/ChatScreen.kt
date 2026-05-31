package com.example.taskgo.ui.screens

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.ui.window.Dialog
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

    var showMapSelector by remember { mutableStateOf(false) }
    var locationToShare by remember { mutableStateOf<com.google.android.gms.maps.model.LatLng?>(null) }
    var addressToShare by remember { mutableStateOf("") }
    var showLocationConfirmDialog by remember { mutableStateOf(false) }

    var replyingTo by remember { mutableStateOf<com.example.taskgo.data.model.ChatMessage?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

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
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
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
                        isPaymentProof = message.isPaymentProof,
                        isLocation = message.isLocation,
                        latitude = message.latitude,
                        longitude = message.longitude,
                        status = message.status,
                        replyToText = message.replyToText,
                        onReply = { replyingTo = message },
                        onCopy = { clipboardManager.setText(AnnotatedString(message.text)) },
                        onOpenMap = {
                            if (message.latitude != null && message.longitude != null) {
                                val uri = "geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}(${message.text})"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            Surface(tonalElevation = 2.dp) {
                Column {
                    if (replyingTo != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Reply, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Replying to", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(replyingTo!!.text.ifBlank { "[Media]" }, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
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
                        IconButton(onClick = { showMapSelector = true }) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Share Location", tint = Color(0xFF1976D2))
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
                                if ((messageText.isNotBlank() || replyingTo != null) && currentUser != null) {
                                    chatViewModel.sendMessage(
                                        senderId = currentUser!!.id,
                                        senderName = currentUser!!.name,
                                        receiverId = otherUserId,
                                        receiverName = if (isSenderRequester) (currentSummary?.runnerName ?: "Runner") else (currentSummary?.requesterName ?: "Requester"),
                                        taskId = taskId,
                                        taskTitle = taskTitle,
                                        isSenderRequester = isSenderRequester,
                                        text = messageText,
                                        replyToId = replyingTo?.id,
                                        replyToText = replyingTo?.text?.ifBlank { "[Media]" }
                                    )
                                    messageText = ""
                                    replyingTo = null
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }

    if (showMapSelector) {
        Dialog(onDismissRequest = { showMapSelector = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                MapSelectorScreen(
                    onLocationSelected = { latLng, address ->
                        locationToShare = latLng
                        addressToShare = address
                        showMapSelector = false
                        showLocationConfirmDialog = true
                    },
                    onBack = { showMapSelector = false }
                )
            }
        }
    }

    if (showLocationConfirmDialog && locationToShare != null) {
        AlertDialog(
            onDismissRequest = { showLocationConfirmDialog = false },
            title = { Text("Share Location", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Confirm the address you want to share:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addressToShare,
                        onValueChange = { addressToShare = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Address") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (currentUser != null) {
                        chatViewModel.sendMessage(
                            senderId = currentUser!!.id,
                            senderName = currentUser!!.name,
                            receiverId = otherUserId,
                            receiverName = if (isSenderRequester) (currentSummary?.runnerName ?: "Runner") else (currentSummary?.requesterName ?: "Requester"),
                            taskId = taskId,
                            taskTitle = taskTitle,
                            isSenderRequester = isSenderRequester,
                            text = addressToShare,
                            isLocation = true,
                            latitude = locationToShare!!.latitude,
                            longitude = locationToShare!!.longitude
                        )
                    }
                    showLocationConfirmDialog = false
                }) { Text("Share Location") }
            },
            dismissButton = { TextButton(onClick = { showLocationConfirmDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TaskReferenceHeader(taskTitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Discussing task:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(taskTitle, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChatBubble(
    text: String,
    imageUrl: String?,
    timestamp: Long,
    isMe: Boolean,
    isPaymentProof: Boolean = false,
    isLocation: Boolean = false,
    latitude: Double? = null,
    longitude: Double? = null,
    status: com.example.taskgo.data.model.MessageStatus = com.example.taskgo.data.model.MessageStatus.SENT,
    replyToText: String? = null,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onOpenMap: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    val backgroundColor = if (isPaymentProof) {
        if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1B2E1B) else Color(0xFFE8F5E9)
    } else if (isLocation) {
        if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1A237E) else Color(0xFFE3F2FD)
    } else if (isMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isPaymentProof) {
        if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
    } else if (isLocation) {
        if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF90CAF9) else Color(0xFF1565C0)
    } else if (isMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50) onReply() // Swipe right to reply
                }
            },
        horizontalAlignment = alignment
    ) {
        Surface(
            color = backgroundColor,
            shape = shape,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clickable { showOptions = true }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (replyToText != null) {
                    Surface(
                        color = contentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(contentColor.copy(alpha = 0.5f)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(replyToText, fontSize = 11.sp, color = contentColor.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (isPaymentProof) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(Icons.Default.Verified, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PAYMENT PROOF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }

                if (isLocation) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(Icons.Default.Map, null, tint = contentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LOCATION SHARED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = contentColor)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onOpenMap, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.OpenInNew, null, tint = contentColor, modifier = Modifier.size(16.dp))
                        }
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

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                    Text(
                        text = time,
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val statusIcon = when (status) {
                            com.example.taskgo.data.model.MessageStatus.SENT -> Icons.Default.Done
                            com.example.taskgo.data.model.MessageStatus.DELIVERED -> Icons.Default.DoneAll
                            com.example.taskgo.data.model.MessageStatus.SEEN -> Icons.Default.DoneAll
                        }
                        val statusTint = if (status == com.example.taskgo.data.model.MessageStatus.SEEN) Color.Cyan else contentColor.copy(alpha = 0.6f)
                        Icon(statusIcon, null, modifier = Modifier.size(12.dp), tint = statusTint)
                    }
                }
            }
        }
    }

    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("Message Options", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Reply") },
                        leadingContent = { Icon(Icons.Default.Reply, null) },
                        modifier = Modifier.clickable { onReply(); showOptions = false }
                    )
                    ListItem(
                        headlineContent = { Text("Copy Text") },
                        leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                        modifier = Modifier.clickable { onCopy(); showOptions = false }
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showOptions = false }) { Text("Close") } }
        )
    }
}
