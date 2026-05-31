package com.example.taskgo.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val taskId: String = "", 
    val text: String = "",
    val imageUrl: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val isPaymentProof: Boolean = false,
    val isLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val replyToId: String? = null, // ID of message being replied to
    val replyToText: String? = null, // Snippet of replied message
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageStatus {
    SENT, DELIVERED, SEEN
}
