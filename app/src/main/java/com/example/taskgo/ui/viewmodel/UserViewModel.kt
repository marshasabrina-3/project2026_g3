package com.example.taskgo.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskgo.data.model.User
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

    init {
        val shouldRemember = sharedPrefs.getBoolean("remember_me", false)
        if (shouldRemember) {
            auth.currentUser?.let { fetchUserProfile(it.uid) }
        } else {
            auth.signOut()
        }
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("Users").document(uid).get().await()
                _currentUser.value = document.toObject<User>()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun login(emailPrefix: String, password: String, rememberMe: Boolean) {
        val email = if (emailPrefix.contains("@")) emailPrefix else "$emailPrefix@graduate.utm.my"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let {
                    fetchUserProfile(it.uid)
                    saveToPrefs(it.uid, email, rememberMe)
                }
            } catch (e: Exception) {
                _error.value = e.message
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
        sharedPrefs.edit().putString("user_id", uid).putString("user_email", email).putBoolean("remember_me", rememberMe).apply()
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
            } catch (e: Exception) { _error.value = e.message }
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
}
