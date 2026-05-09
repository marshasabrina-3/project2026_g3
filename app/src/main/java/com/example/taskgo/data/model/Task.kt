package com.example.taskgo.data.model

enum class TaskStatus {
    OPEN,            // Waiting for runner to accept
    PENDING_APPROVAL, // Runner accepted, waiting for requester to approve
    ASSIGNED,        // Requester approved runner (On going)
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
    val runnerId: String? = null, // The final accepted runner
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
    val timestamp: Long = System.currentTimeMillis()
)
