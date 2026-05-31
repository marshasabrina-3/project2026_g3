package com.example.taskgo.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskgo.data.model.*
import com.example.taskgo.util.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val firestore = FirebaseFirestore.getInstance()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val allReports = _reports.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val allReviews = _reviews.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<TaskCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedBlock = MutableStateFlow<String?>(null)
    val selectedBlock = _selectedBlock.asStateFlow()

    private val _priceRange = MutableStateFlow(0.2..100.0)
    val priceRange = _priceRange.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting = _isPosting.asStateFlow()

    enum class SortOption { LATEST, ALPHA_ASC, ALPHA_DESC, PRICE_LOW_HIGH, PRICE_HIGH_LOW }
    private val _sortOption = MutableStateFlow(SortOption.LATEST)
    val sortOption = _sortOption.asStateFlow()

    private val _selectedCampus = MutableStateFlow<String?>(null)
    val selectedCampus = _selectedCampus.asStateFlow()

    private val _selectedType = MutableStateFlow<TaskType?>(null)
    val selectedType = _selectedType.asStateFlow()

    val filteredTasks = combine(
        _tasks, _searchQuery, _selectedCategory, _selectedBlock, _priceRange, _sortOption, _selectedCampus, _selectedType
    ) { array ->
        val tasks = array[0] as List<Task>
        val query = array[1] as String
        val category = array[2] as TaskCategory?
        val block = array[3] as String?
        val price = array[4] as ClosedFloatingPointRange<Double>
        val sort = array[5] as SortOption
        val campus = array[6] as String?
        val type = array[7] as TaskType?

        tasks.filter { task ->
            val isLive = task.status == TaskStatus.OPEN
            val matchesCategory = category == null || task.category == category
            val matchesQuery = task.title.contains(query, ignoreCase = true) || task.description.contains(query, ignoreCase = true)
            val matchesBlock = block == null || task.location.contains("Block $block", ignoreCase = true)
            val matchesPrice = if (price.endInclusive >= 100.0) task.paymentAmount >= price.start else task.paymentAmount in price
            val matchesCampus = campus == null || task.campus == campus
            val matchesType = type == null || task.type == type
            
            isLive && matchesCategory && matchesQuery && matchesBlock && matchesPrice && matchesCampus && matchesType
        }.let { filtered ->
            when (sort) {
                SortOption.LATEST -> filtered.sortedByDescending { it.timestamp }
                SortOption.ALPHA_ASC -> filtered.sortedBy { it.title }
                SortOption.ALPHA_DESC -> filtered.sortedByDescending { it.title }
                SortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.paymentAmount }
                SortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.paymentAmount }
            }
        }
    }

    init {
        fetchTasks()
        fetchReports()
        fetchReviews()
    }

    private fun fetchTasks() {
        firestore.collection("Tasks").orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) _tasks.value = snapshot.documents.mapNotNull { it.toObject<Task>() }
        }
    }

    private fun fetchReports() {
        firestore.collection("Reports").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) _reports.value = snapshot.documents.mapNotNull { it.toObject<Report>() }
        }
    }

    private fun fetchReviews() {
        firestore.collection("Reviews").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) _reviews.value = snapshot.documents.mapNotNull { it.toObject<Review>() }
        }
    }

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }
    fun onCategoryChange(c: TaskCategory?) { _selectedCategory.value = c }
    fun onBlockChange(b: String?) { _selectedBlock.value = b }
    fun onPriceRangeChange(r: ClosedFloatingPointRange<Double>) { _priceRange.value = r }
    fun onSortOptionChange(s: SortOption) { _sortOption.value = s }
    fun onCampusChange(c: String?) { _selectedCampus.value = c }
    fun onTypeChange(t: TaskType?) { _selectedType.value = t }

    fun applyAiFilters(category: TaskCategory?, campus: String?, type: TaskType?, query: String) {
        _selectedCategory.value = category
        _selectedCampus.value = campus
        _selectedType.value = type
        _searchQuery.value = query
    }

    // --- TASK ACTIONS WITH NOTIFICATION LOGS ---

    fun addTask(
        title: String, description: String, category: TaskCategory, type: TaskType,
        campus: String, address: String, deadline: String, paymentAmount: Double,
        requesterId: String, requesterName: String, imageUris: List<Uri> = emptyList(),
        latitude: Double? = null, longitude: Double? = null,
        destinationAddress: String? = null,
        destinationLatitude: Double? = null, destinationLongitude: Double? = null
    ) {
        viewModelScope.launch {
            _isPosting.value = true
            try {
                val base64Images = imageUris.mapNotNull { uri ->
                    ImageUtils.uriToBase64(getApplication(), uri, 500, 500)
                }

                val docRef = firestore.collection("Tasks").document()
                val newTask = Task(
                    id = docRef.id,
                    requesterId = requesterId,
                    requesterName = requesterName,
                    title = title,
                    description = description,
                    category = category,
                    type = type,
                    campus = campus,
                    address = address,
                    destinationAddress = destinationAddress,
                    latitude = latitude,
                    longitude = longitude,
                    destinationLatitude = destinationLatitude,
                    destinationLongitude = destinationLongitude,
                    deadline = deadline,
                    paymentAmount = paymentAmount,
                    status = TaskStatus.OPEN,
                    images = base64Images,
                    timestamp = System.currentTimeMillis()
                )

                withTimeout(15000) {
                    docRef.set(newTask).await()
                }
                Toast.makeText(getApplication(), "Task posted successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("TaskViewModel", "AddTask Error", e)
                Toast.makeText(getApplication(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isPosting.value = false
            }
        }
    }

    fun assignRunner(taskId: String, runnerId: String) {
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("Users").document(runnerId).get().await()
                val runnerName = userDoc.getString("name") ?: "Runner"

                firestore.collection("Tasks").document(taskId).update(
                    "runnerId", runnerId,
                    "runnerName", runnerName,
                    "status", TaskStatus.ASSIGNED
                ).await()

                // Notify Runner
                sendNotificationToUser(runnerId, "Task Assigned!", "You have been picked for a task.")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "AssignRunner Error", e)
            }
        }
    }

    fun applyForTask(taskId: String, runnerId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("Tasks").document(taskId).update(
                    "interestedRunnerIds", com.google.firebase.firestore.FieldValue.arrayUnion(runnerId)
                ).await()

                // Notify Requester
                val taskDoc = firestore.collection("Tasks").document(taskId).get().await()
                val requesterId = taskDoc.getString("requesterId") ?: ""
                sendNotificationToUser(requesterId, "New Applicant!", "Someone wants to help with your task.")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Apply Error", e)
            }
        }
    }

    fun markTaskAsFinished(taskId: String, proofUri: Uri?) {
        viewModelScope.launch {
            try {
                val proofBase64 = proofUri?.let { ImageUtils.uriToBase64(getApplication(), it, 500, 500) }
                firestore.collection("Tasks").document(taskId).update(
                    "status", TaskStatus.WAITING_VERIFICATION,
                    "completionProof", proofBase64
                ).await()
                Toast.makeText(getApplication(), "Task marked as finished!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("TaskViewModel", "MarkFinished Error", e)
            }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            try {
                val docRef = firestore.collection("Tasks").document(taskId)
                val taskSnapshot = docRef.get().await()
                val task = taskSnapshot.toObject<Task>() ?: return@launch
                
                docRef.update(
                    "status", TaskStatus.COMPLETED,
                    "completionTimestamp", System.currentTimeMillis()
                ).await()

                // Update Runner's Wallet Balance (Mock)
                task.runnerId?.let { runnerId ->
                    val userRef = firestore.collection("Users").document(runnerId)
                    val runner = userRef.get().await().toObject<User>()
                    val currentBalance = runner?.walletBalance ?: 0.0
                    userRef.update("walletBalance", currentBalance + task.paymentAmount).await()
                }
                
                Toast.makeText(getApplication(), "Task verified and closed!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("TaskViewModel", "CompleteTask Error", e)
            }
        }
    }

    fun updatePaymentStatus(taskId: String, status: PaymentStatus, proofUri: Uri? = null) {
        viewModelScope.launch {
            try {
                val proofBase64 = proofUri?.let { ImageUtils.uriToBase64(getApplication(), it, 500, 500) }
                val updates = mutableMapOf<String, Any>(
                    "paymentStatus" to status
                )
                if (proofBase64 != null) {
                    updates["paymentProof"] = proofBase64
                }
                firestore.collection("Tasks").document(taskId).update(updates).await()
                Toast.makeText(getApplication(), "Payment status updated to $status", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("TaskViewModel", "UpdatePaymentStatus Error", e)
            }
        }
    }

    fun cancelTask(taskId: String) {
        firestore.collection("Tasks").document(taskId).update("status", TaskStatus.CANCELLED)
    }

    fun hideTaskForUser(taskId: String, userId: String) {
        viewModelScope.launch {
            try {
                val docRef = firestore.collection("Tasks").document(taskId)
                val task = docRef.get().await().toObject<Task>() ?: return@launch
                
                val updates = mutableMapOf<String, Any>()
                if (task.requesterId == userId) {
                    updates["hiddenByRequester"] = true
                } else if (task.runnerId == userId || task.interestedRunnerIds.contains(userId)) {
                    updates["hiddenByRunner"] = true
                }
                
                if (updates.isNotEmpty()) {
                    docRef.update(updates).await()
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "HideTask Error", e)
            }
        }
    }

    fun withdrawApplication(taskId: String, runnerId: String) {
        firestore.collection("Tasks").document(taskId).update(
            "interestedRunnerIds", com.google.firebase.firestore.FieldValue.arrayRemove(runnerId)
        )
    }

    // --- ORIGINAL HELPER FUNCTIONS (RATING/REPORTS) ---

    fun getUserRating(userId: String): Double {
        val userReviews = _reviews.value.filter { it.revieweeId == userId }
        if (userReviews.isEmpty()) return 0.0
        return userReviews.map { it.rating }.average()
    }

    fun getUserReportCount(userId: String): Int {
        return _reports.value.count { it.reportedUserId == userId }
    }

    fun addReview(review: Review) {
        val docRef = firestore.collection("Reviews").document()
        docRef.set(review.copy(id = docRef.id))
    }

    fun addReport(report: Report) {
        val docRef = firestore.collection("Reports").document()
        docRef.set(report.copy(id = docRef.id))
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("Tasks").document(taskId).delete().await()
                Log.d("TaskViewModel", "Task deleted successfully from Firebase: $taskId")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error deleting task: $taskId", e)
                Toast.makeText(getApplication(), "Error deleting task: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun updateTask(t: Task) { firestore.collection("Tasks").document(t.id).set(t) }

    suspend fun getInterestedRunners(ids: List<String>): List<User> {
        if (ids.isEmpty()) return emptyList()
        return try {
            val snapshot = firestore.collection("Users").whereIn("id", ids).get().await()
            snapshot.documents.mapNotNull { it.toObject<User>() }
        } catch (e: Exception) { emptyList() }
    }

    // --- FCM HELPER ---

    private fun sendNotificationToUser(userId: String, title: String, message: String) {
        // 1. Log for debugging
        Log.d("FCM_LOG", "Sending to $userId: $title")

        // 2. SAVE TO DATABASE (This makes it show up in your screenshot)
        val notificationData = hashMapOf(
            "receiverId" to userId,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )

        firestore.collection("Notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("DATABASE", "Notification saved to Firestore!")
            }
            .addOnFailureListener { e ->
                Log.e("DATABASE", "Failed to save notification", e)
            }
    }
}