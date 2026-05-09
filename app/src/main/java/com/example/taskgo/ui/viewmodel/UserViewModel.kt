package com.example.taskgo.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.taskgo.data.model.User
import com.example.taskgo.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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

    init {
        // Auto-login if Firebase user exists AND rememberMe was checked
        val shouldRemember = sharedPrefs.getBoolean("remember_me", false)
        if (shouldRemember) {
            auth.currentUser?.let { firebaseUser ->
                fetchUserProfile(firebaseUser.uid)
            }
        } else {
            // If we shouldn't remember, sign out any existing Firebase session
            auth.signOut()
            _currentUser.value = null
        }
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("Users").document(uid).get().await()
                val user = document.toObject<User>()
                _currentUser.value = user
            } catch (e: Exception) {
                _error.value = "Failed to fetch profile: ${e.message}"
            }
        }
    }

    fun login(emailPrefix: String, password: String, rememberMe: Boolean) {
        val email = if (emailPrefix.contains("@")) emailPrefix else "$emailPrefix@graduate.utm.my"
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let { firebaseUser ->
                    fetchUserProfile(firebaseUser.uid)
                    // Always save the preference of whether to remember or not
                    saveToPrefs(firebaseUser.uid, email, rememberMe)
                }
            } catch (e: Exception) {
                _error.value = "Login failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(user: User, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = auth.createUserWithEmailAndPassword(user.email, password).await()
                result.user?.let { firebaseUser ->
                    val finalUser = user.copy(id = firebaseUser.uid)
                    // Save to Firestore
                    firestore.collection("Users").document(firebaseUser.uid).set(finalUser).await()
                    _currentUser.value = finalUser
                    // Registration counts as "remember me" by default for the first session
                    saveToPrefs(firebaseUser.uid, user.email, true)
                }
            } catch (e: Exception) {
                _error.value = "Registration failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveToPrefs(uid: String, email: String, rememberMe: Boolean) {
        sharedPrefs.edit().apply {
            putString("user_id", uid)
            putString("user_email", email)
            putBoolean("remember_me", rememberMe)
            apply()
        }
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
                val updates = mapOf(
                    "name" to name,
                    "email" to email,
                    "phoneNumber" to phoneNumber
                )
                firestore.collection("Users").document(uid).update(updates).await()
                _currentUser.value = _currentUser.value?.copy(
                    name = name,
                    email = email,
                    phoneNumber = phoneNumber
                )
            } catch (e: Exception) {
                _error.value = "Failed to update profile: ${e.message}"
            }
        }
    }

    fun updateProfileImage(imageUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("Users").document(uid).update("profileImageUrl", imageUrl).await()
                _currentUser.value = _currentUser.value?.copy(profileImageUrl = imageUrl)
            } catch (e: Exception) {
                _error.value = "Failed to update profile image: ${e.message}"
            }
        }
    }
}
