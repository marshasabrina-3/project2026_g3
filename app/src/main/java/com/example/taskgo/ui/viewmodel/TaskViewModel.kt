package com.example.taskgo.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.taskgo.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TaskViewModel : ViewModel() {
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

    enum class SortOption {
        LATEST, ALPHA_ASC, ALPHA_DESC, PRICE_LOW_HIGH, PRICE_HIGH_LOW
    }
    private val _sortOption = MutableStateFlow(SortOption.LATEST)
    val sortOption = _sortOption.asStateFlow()

    val filteredTasks = combine(
        _tasks, _searchQuery, _selectedCategory, _selectedBlock, _priceRange, _sortOption
    ) { array ->
        val tasks = array[0] as List<Task>
        val query = array[1] as String
        val category = array[2] as TaskCategory?
        val block = array[3] as String?
        @Suppress("UNCHECKED_CAST")
        val price = array[4] as ClosedFloatingPointRange<Double>
        val sort = array[5] as SortOption

        tasks.filter { task ->
            val isOpen = task.status == TaskStatus.OPEN
            val matchesCategory = category == null || task.category == category
            val matchesQuery = task.title.contains(query, ignoreCase = true) || 
                               task.description.contains(query, ignoreCase = true)
            val matchesBlock = block == null || task.location.contains("Block $block", ignoreCase = true)
            val matchesPrice = if (price.endInclusive >= 100.0) {
                task.paymentAmount >= price.start
            } else {
                task.paymentAmount in price
            }
            
            isOpen && matchesCategory && matchesQuery && matchesBlock && matchesPrice
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
        firestore.collection("Tasks")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _tasks.value = snapshot.documents.mapNotNull { it.toObject<Task>() }
                }
            }
    }

    private fun fetchReports() {
        firestore.collection("Reports")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _reports.value = snapshot.documents.mapNotNull { it.toObject<Report>() }
                }
            }
    }

    private fun fetchReviews() {
        firestore.collection("Reviews")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _reviews.value = snapshot.documents.mapNotNull { it.toObject<Review>() }
                }
            }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryChange(category: TaskCategory?) {
        _selectedCategory.value = category
    }

    fun onBlockChange(block: String?) {
        _selectedBlock.value = block
    }

    fun onPriceRangeChange(range: ClosedFloatingPointRange<Double>) {
        _priceRange.value = range
    }

    fun onSortOptionChange(option: SortOption) {
        _sortOption.value = option
    }

    fun addTask(
        title: String,
        description: String,
        category: TaskCategory,
        type: TaskType,
        campus: String,
        address: String,
        deadline: String,
        paymentAmount: Double,
        requesterId: String
    ) {
        val docRef = firestore.collection("Tasks").document()
        val newTask = Task(
            id = docRef.id,
            requesterId = requesterId,
            title = title,
            description = description,
            category = category,
            type = type,
            campus = campus,
            address = address,
            deadline = deadline,
            paymentAmount = paymentAmount,
            status = TaskStatus.OPEN,
            timestamp = System.currentTimeMillis()
        )
        docRef.set(newTask)
    }

    fun deleteTask(taskId: String) {
        firestore.collection("Tasks").document(taskId).delete()
    }

    fun updateTask(updatedTask: Task) {
        firestore.collection("Tasks").document(updatedTask.id).set(updatedTask)
    }

    suspend fun getInterestedRunners(runnerIds: List<String>): List<User> {
        if (runnerIds.isEmpty()) return emptyList()
        return try {
            val snapshot = firestore.collection("Users")
                .whereIn("id", runnerIds)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject<User>() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun assignRunner(taskId: String, runnerId: String) {
        firestore.collection("Tasks").document(taskId).update(
            "runnerId", runnerId,
            "status", TaskStatus.ASSIGNED
        )
    }

    fun applyForTask(taskId: String, runnerId: String) {
        firestore.collection("Tasks").document(taskId).update(
            "interestedRunnerIds", com.google.firebase.firestore.FieldValue.arrayUnion(runnerId)
        )
    }

    fun reportTask(taskId: String, reporterId: String, reason: String) {
        val docRef = firestore.collection("Reports").document()
        val newReport = Report(
            id = docRef.id,
            taskId = taskId,
            reporterId = reporterId,
            description = reason,
            status = ReportStatus.PENDING
        )
        docRef.set(newReport)
    }
}
