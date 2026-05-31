package com.example.taskgo.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskgo.data.model.User
import com.example.taskgo.data.model.UserRole
import com.example.taskgo.data.model.UserStatus
import com.example.taskgo.util.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val sharedPrefs = application.getSharedPreferences("taskgo_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing = _isInitializing.asStateFlow()

    // --- TG-US21: ADMIN USER MANAGEMENT STATE FLOWS ---
    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers = _allUsers.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    init {
        val shouldRemember = sharedPrefs.getBoolean("remember_me", false)
        val firebaseUser = auth.currentUser

        if (shouldRemember && firebaseUser != null) {
            fetchUserProfile(firebaseUser.uid)
        } else {
            auth.signOut()
            _isInitializing.value = false
        }
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val document = firestore.collection("Users").document(uid).get().await()
                val userObject = document.toObject<User>()
                _currentUser.value = userObject
                Log.d("AUTH_INIT", "Auto-logged in user with role: ${userObject?.role}")
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("AUTH_INIT", "Failed to fetch profile during auto-login", e)
            } finally {
                _isLoading.value = false
                _isInitializing.value = false
            }
        }
    }

    fun login(emailPrefix: String, password: String, rememberMe: Boolean, onResult: (Boolean, UserRole?) -> Unit) {
        val email = if (emailPrefix.contains("@")) emailPrefix else "$emailPrefix@graduate.utm.my"
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let { firebaseUser ->
                    val document = firestore.collection("Users").document(firebaseUser.uid).get().await()
                    val userObject = document.toObject<User>()

                    if (userObject != null) {
                        _currentUser.value = userObject
                        saveToPrefs(firebaseUser.uid, email, rememberMe)

                        Log.d("AUTH_ROLE", "User verified with role: ${userObject.role}")
                        onResult(true, userObject.role)
                    } else {
                        _error.value = "User profile configuration data not found."
                        onResult(false, null)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                onResult(false, null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(user: User, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = auth.createUserWithEmailAndPassword(user.email, password).await()
                result.user?.let {
                    val finalUser = user.copy(id = it.uid)
                    firestore.collection("Users").document(it.uid).set(finalUser).await()
                    _currentUser.value = finalUser
                    saveToPrefs(it.uid, user.email, true)
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveToPrefs(uid: String, email: String, rememberMe: Boolean) {
        sharedPrefs.edit()
            .putString("user_id", uid)
            .putString("user_email", email)
            .putBoolean("remember_me", rememberMe)
            .apply()
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        sharedPrefs.edit().clear().apply()
    }

    fun updateProfile(name: String, email: String, phoneNumber: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val updates = mapOf("name" to name, "email" to email, "phoneNumber" to phoneNumber)
                firestore.collection("Users").document(uid).update(updates).await()
                _currentUser.value = _currentUser.value?.copy(name = name, email = email, phoneNumber = phoneNumber)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun uploadProfileImage(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val base64Image = ImageUtils.uriToBase64(getApplication(), uri)
                if (base64Image != null) {
                    withTimeout(30000) {
                        firestore.collection("Users").document(uid).update("profileImageUrl", base64Image).await()
                        _currentUser.value = _currentUser.value?.copy(profileImageUrl = base64Image)
                    }
                    Toast.makeText(getApplication(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Upload failed", e)
                Toast.makeText(getApplication(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- TG-US21 & TG-US27: ADMIN ACTIONS AND FILTERS ---

    /**
     * Task #163: Fetches all registered student records from Firestore
     */
    fun fetchAllUserRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firestore.collection("Users").get().await()
                val userList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<User>()
                }.filter { it.role != UserRole.ADMIN } // Keep other admins safe from modifications

                _allUsers.value = userList
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("ADMIN_DB", "Failed to load user records from Firestore", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Task #165: Updates user search keyword string
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Helper logic combining retrieval records with query filtering parameters
     */
    fun getFilteredUsers(): List<User> {
        val query = _searchQuery.value.trim().lowercase()
        if (query.isEmpty()) return _allUsers.value

        return _allUsers.value.filter { user ->
            user.name.lowercase().contains(query) ||
                    user.email.lowercase().contains(query) ||
                    user.matric.lowercase().contains(query)
        }
    }

    /**
     * Task #195 & #198: Updates database values to suspend or ban a user
     */
    fun modifyUserAccountStatus(userId: String, targetStatus: UserStatus, suspensionMinutes: Long? = null) {
        viewModelScope.launch {
            try {
                val databaseUpdates = mutableMapOf<String, Any>(
                    "status" to targetStatus.name
                )

                if (targetStatus == UserStatus.SUSPENDED && suspensionMinutes != null) {
                    val startTimeMillis = System.currentTimeMillis()
                    val endTimeMillis = startTimeMillis + (suspensionMinutes * 60 * 1000L)

                    databaseUpdates["suspensionStartDate"] = startTimeMillis.toString()
                    databaseUpdates["suspensionEndDate"] = endTimeMillis.toString()
                } else {
                    databaseUpdates["suspensionStartDate"] = com.google.firebase.firestore.FieldValue.delete()
                    databaseUpdates["suspensionEndDate"] = com.google.firebase.firestore.FieldValue.delete()
                }

                firestore.collection("Users").document(userId).update(databaseUpdates).await()
                Log.d("ADMIN_SECURITY", "Account tracking changed for $userId -> ${targetStatus.name}")

                // Refresh local values so the search lists update immediately on-screen
                fetchAllUserRecords()
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("ADMIN_SECURITY", "Failed to update account authorization parameters", e)
            }
        }
    }
}