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
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _messages.value = snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                }
            }
    }

    fun listenForInbox(currentUserId: String) {
        inboxListener?.remove()
        inboxListener = firestore.collection("Conversations")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val summaries = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatSummary::class.java)?.copy(convId = doc.id)
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
        imageUrl: String? = null
    ) {
        if (text.isBlank() && imageUrl == null) return

        val runnerId = if (isSenderRequester) receiverId else senderId
        val runnerName = if (isSenderRequester) receiverName else senderName
        val requesterId = if (isSenderRequester) senderId else receiverId
        val requesterName = if (isSenderRequester) senderName else receiverName

        val convId = "${taskId}_$runnerId"
        val timestamp = System.currentTimeMillis()
        val incrementField = if (isSenderRequester) "unreadCountRunner" else "unreadCountRequester"

        val summaryUpdates = mapOf(
            "taskId" to taskId,
            "taskTitle" to taskTitle,
            "lastMessage" to text.ifBlank { "[Image]" },
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

        val messageMap = mutableMapOf(
            "senderId" to senderId,
            "text" to text,
            "timestamp" to timestamp
        )
        if (imageUrl != null) messageMap["imageUrl"] = imageUrl

        firestore.collection("Conversations").document(convId)
            .collection("Messages").add(messageMap)

        // --- TRIGGER NOTIFICATION ---
        val notificationText = if (imageUrl != null && text.isBlank()) "Sent an image" else text
        sendNotificationToUser(receiverId, "New Message from $senderName", notificationText)
    }

    private fun markAsRead(convId: String, currentUserId: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("Conversations").document(convId).get().await()
                val summary = doc.toObject(ChatSummary::class.java) ?: return@launch

                val resetField = if (summary.requesterId == currentUserId) "unreadCountRequester" else "unreadCountRunner"
                firestore.collection("Conversations").document(convId).update(resetField, 0)
            } catch (e: Exception) {}
        }
    }

    fun uploadImage(uri: Uri, senderId: String, senderName: String, receiverId: String, receiverName: String, taskId: String, taskTitle: String, isRequester: Boolean) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val base64Image = ImageUtils.uriToBase64(getApplication(), uri, 400, 400)
                if (base64Image != null) {
                    sendMessage(senderId, senderName, receiverId, receiverName, taskId, taskTitle, isRequester, "", imageUrl = base64Image)
                }
            } finally {
                _isUploading.value = false
            }
        }
    }

    // --- HELPER FUNCTION FOR NOTIFICATIONS ---
    private fun sendNotificationToUser(userId: String, title: String, message: String) {
        firestore.collection("Users").document(userId).get().addOnSuccessListener { doc ->
            val token = doc.getString("fcmToken")
            if (token != null) {
                Log.d("CHAT_FCM", "Pushing to: $userId | Token: $token | Title: $title")
                // In a real production setup, this would call a Cloud Function or API.
            }
        }

        // Also save to your Firestore Notifications collection as requested
        val notificationData = hashMapOf(
            "receiverId" to userId,
            "title" to title,
            "message" to message,
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