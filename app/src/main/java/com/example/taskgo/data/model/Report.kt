package com.example.taskgo.data.model

enum class ReportStatus {
    PENDING, RESOLVED
}

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reportedUserId: String? = null,
    val taskId: String? = null,
    val description: String = "",
    val status: ReportStatus = ReportStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)
