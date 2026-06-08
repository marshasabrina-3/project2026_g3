package com.example.taskgo.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log // Added for debugging
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskgo.data.model.ChatMessage
import com.example.taskgo.util.ImageUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatSummary(
    val convId: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0,
    val participants: List<String> = emptyList(),
    val requesterId: String = "",
    val requesterName: String = "",
    val runnerId: String = "",
    val runnerName: String = "",
    val unreadCountRequester: Int = 0,
    val unreadCountRunner: Int = 0
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val firestore = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _activeChats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val activeChats = _activeChats.asStateFlow()

    private var messageListener: ListenerRegistration? = null
    private var inboxListener: ListenerRegistration? = null

    fun listenForMessages(taskId: String, runnerId: String, currentUserId: String) {
        val convId = "${taskId}_$runnerId"
        messageListener?.remove()

        markAsRead(convId, currentUserId)

        messageListener = firestore.collection("Conversations").document(convId)
            .collection("Messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Messages listener error", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _messages.value = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(com.example.taskgo.data.model.ChatMessage::class.java)
                        } catch (ex: Exception) {
                            Log.e("ChatViewModel", "Failed to map Message: ${doc.id}", ex)
                            null
                        }
                    }
                }
            }
    }

    fun listenForInbox(currentUserId: String) {
        inboxListener?.remove()
        inboxListener = firestore.collection("Conversations")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Inbox listener error", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val summaries = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(ChatSummary::class.java)?.copy(convId = doc.id)
                        } catch (ex: Exception) {
                            Log.e("ChatViewModel", "Failed to map ChatSummary: ${doc.id}", ex)
                            null
                        }
                    }.sortedByDescending { it.timestamp }
                    _activeChats.value = summaries
                }
            }
    }

    fun sendMessage(
        senderId: String,
        senderName: String,
        receiverId: String,
        receiverName: String,
        taskId: String,
        taskTitle: String,
        isSenderRequester: Boolean,
        text: String,
        imageUrl: String? = null,
        isPaymentProof: Boolean = false,
        isLocation: Boolean = false,
        latitude: Double? = null,
        longitude: Double? = null,
        replyToId: String? = null,
        replyToText: String? = null
    ) {
        if (text.isBlank() && imageUrl == null && !isLocation) return

        val runnerId = if (isSenderRequester) receiverId else senderId
        val runnerName = if (isSenderRequester) receiverName else senderName
        val requesterId = if (isSenderRequester) senderId else receiverId
        val requesterName = if (isSenderRequester) senderName else receiverName

        val convId = "${taskId}_$runnerId"
        val timestamp = System.currentTimeMillis()
        val incrementField = if (isSenderRequester) "unreadCountRunner" else "unreadCountRequester"

        val lastMessagePreview = if (isPaymentProof) "[Payment Proof]" 
                                else if (isLocation) "[Location Share]"
                                else text.ifBlank { "[Image]" }

        val summaryUpdates = mapOf(
            "taskId" to taskId,
            "taskTitle" to taskTitle,
            "lastMessage" to lastMessagePreview,
            "timestamp" to timestamp,
            "participants" to listOf(requesterId, runnerId),
            "requesterId" to requesterId,
            "requesterName" to requesterName,
            "runnerId" to runnerId,
            "runnerName" to runnerName,
            incrementField to FieldValue.increment(1)
        )

        firestore.collection("Conversations").document(convId)
            .set(summaryUpdates, SetOptions.merge())

        val messageMap = mutableMapOf<String, Any>(
            "senderId" to senderId,
            "text" to if (isPaymentProof && text.isBlank()) "Sent payment proof for task: $taskTitle" else text,
            "timestamp" to timestamp,
            "isPaymentProof" to isPaymentProof,
            "isLocation" to isLocation,
            "status" to "SENT"
        )
        if (imageUrl != null) messageMap["imageUrl"] = imageUrl
        if (latitude != null) messageMap["latitude"] = latitude
        if (longitude != null) messageMap["longitude"] = longitude
        if (replyToId != null) messageMap["replyToId"] = replyToId
        if (replyToText != null) messageMap["replyToText"] = replyToText

        firestore.collection("Conversations").document(convId)
            .collection("Messages").add(messageMap)

        // If it's payment proof, also update the Task record for the requester/runner/admin to see
        if (isPaymentProof && imageUrl != null) {
            firestore.collection("Tasks").document(taskId).update(
                "paymentProof", imageUrl,
                "paymentStatus", "PAID" // Auto-set to PAID when proof is sent? Or keep as PENDING?
            )
        }

        // --- TRIGGER NOTIFICATION ---
        val notificationText = if (imageUrl != null && text.isBlank()) "Sent an image" else text
        sendNotificationToUser(receiverId, "New Message from $senderName", notificationText, taskId)

        // --- AGENTIC AI SKILL: Magic Completion Detection (Runner Side) ---
        if (!isSenderRequester) {
            val lowerText = text.lowercase()
            if (lowerText.contains("done") || lowerText.contains("finished") || lowerText.contains("settled")) {
                sendNotificationToUser(
                    userId = requesterId,
                    title = "Magic Suggestion ✨",
                    message = "$senderName says they are finished. Would you like to verify the task now?",
                    taskId = taskId
                )
            }
        }
    }

    private fun markAsRead(convId: String, currentUserId: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("Conversations").document(convId).get().await()
                val summary = doc.toObject(ChatSummary::class.java) ?: return@launch

                val resetField = if (summary.requesterId == currentUserId) "unreadCountRequester" else "unreadCountRunner"
                firestore.collection("Conversations").document(convId).update(resetField, 0)

                // Mark all unread messages as SEEN
                val unreadMessages = firestore.collection("Conversations").document(convId)
                    .collection("Messages")
                    .whereNotEqualTo("senderId", currentUserId)
                    .whereNotEqualTo("status", "SEEN")
                    .get().await()
                
                for (msgDoc in unreadMessages.documents) {
                    msgDoc.reference.update("status", "SEEN")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "MarkAsRead Error", e)
            }
        }
    }

    fun uploadImage(uri: Uri, senderId: String, senderName: String, receiverId: String, receiverName: String, taskId: String, taskTitle: String, isRequester: Boolean, isPaymentProof: Boolean = false) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val base64Image = ImageUtils.uriToBase64(getApplication(), uri, 400, 400)
                if (base64Image != null) {
                    sendMessage(senderId, senderName, receiverId, receiverName, taskId, taskTitle, isRequester, "", imageUrl = base64Image, isPaymentProof = isPaymentProof)
                    
                    // If runner uploads an image (not payment proof), suggest it might be completion
                    if (!isRequester && !isPaymentProof) {
                        sendNotificationToUser(
                            userId = receiverId,
                            title = "Magic Suggestion ✨",
                            message = "$senderName uploaded an image. Is this the completion proof?",
                            taskId = taskId
                        )
                    }
                }
            } finally {
                _isUploading.value = false
            }
        }
    }

    // --- HELPER FUNCTION FOR NOTIFICATIONS ---
    private fun sendNotificationToUser(userId: String, title: String, message: String, taskId: String? = null) {
        firestore.collection("Users").document(userId).get().addOnSuccessListener { doc ->
            val token = doc.getString("fcmToken")
            if (token != null) {
                Log.d("CHAT_FCM", "Pushing to: $userId | Token: $token | Title: $title")
            }
        }

        val notificationData = hashMapOf(
            "receiverId" to userId,
            "title" to title,
            "message" to message,
            "taskId" to taskId,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        firestore.collection("Notifications").add(notificationData)
    }

    override fun onCleared() {
        super.onCleared()
        messageListener?.remove()
        inboxListener?.remove()
    }
}