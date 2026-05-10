package com.example.taskgo.data.model

enum class TaskStatus {
    OPEN,            // Waiting for runner to accept
    PENDING_APPROVAL, // Runner applied, waiting for requester to approve the runner
    ASSIGNED,        // Task in progress
    WAITING_VERIFICATION, // Runner finished, waiting for requester to verify payment/service
    COMPLETED,
    CANCELLED
}

enum class PaymentStatus {
    PENDING, PAID
}

enum class TaskCategory {
    GENERAL, FOOD_DELIVERY, CARPOOL, PRINTING
}

enum class TaskType {
    REQUEST, SERVICE
}

data class Task(
    val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "", // Added name for quick display
    val runnerId: String? = null,
    val runnerName: String? = null, // Added runner name
    val interestedRunnerIds: List<String> = emptyList(), // Runners who want to do this task
    val title: String = "",
    val description: String = "",
    val category: TaskCategory = TaskCategory.GENERAL,
    val type: TaskType = TaskType.REQUEST,
    val location: String = "",
    val address: String = "", // Specific address under UTMKL/UTMJB
    val campus: String = "UTMKL", // UTMKL or UTMJB
    val deadline: String = "",
    val paymentAmount: Double = 0.0,
    val status: TaskStatus = TaskStatus.OPEN,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val images: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val completionTimestamp: Long? = null
)
