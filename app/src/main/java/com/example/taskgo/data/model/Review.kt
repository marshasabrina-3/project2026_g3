package com.example.taskgo.data.model

data class Review(
    val id: String = "",
    val taskId: String = "",
    val reviewerId: String = "",
    val revieweeId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
