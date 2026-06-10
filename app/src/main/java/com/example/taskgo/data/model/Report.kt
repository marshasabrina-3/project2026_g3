package com.example.taskgo.data.model

enum class ReportStatus {
    PENDING, RESOLVED, ACTION_TAKEN
}

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reportedUserId: String? = null,
    val taskId: String? = null,
    val reason: String = "", // Added reason field
    val description: String = "",
    val status: ReportStatus = ReportStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)
