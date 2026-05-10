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
    val timestamp: Long = System.currentTimeMillis()
)
