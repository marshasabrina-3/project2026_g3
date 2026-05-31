package com.example.taskgo.data.model

enum class UserRole {
    REQUESTER, RUNNER, ADMIN, STUDENT
}

enum class UserStatus {
    ACTIVE, BANNED, SUSPENDED
}

data class User(
    val id: String = "", // Firebase UID
    val matric: String = "",
    val name: String = "",
    val nric: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: UserRole = UserRole.STUDENT,
    val status: UserStatus = UserStatus.ACTIVE,
    val rating: Float = 0f,
    val reportCount: Int = 0,
    val profileImageUrl: String? = null,
    val walletBalance: Double = 0.0,
    val suspensionStartDate: String? = null,
    val suspensionEndDate: String? = null
)
