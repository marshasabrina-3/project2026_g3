package com.example.taskgo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.taskgo.navigation.TaskGONavGraph
import com.example.taskgo.ui.theme.TaskGOTheme
import com.example.taskgo.ui.viewmodel.ThemeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermission()
        saveCurrentToken()

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val appTheme by themeViewModel.themeState
            
            TaskGOTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    TaskGONavGraph(navController = navController)
                }
            }
        }
    }

    private fun saveCurrentToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    FirebaseFirestore.getInstance().collection("Users").document(userId)
                        .update("fcmToken", token)
                        .addOnSuccessListener {
                            Log.d("FCM", "Token updated, starting listener...")
                            startNotificationListener(userId)
                        }
                }
            }
        }
    }

    private fun startNotificationListener(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Notifications")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val title = change.document.getString("title") ?: "TaskGO Alert"
                        val message = change.document.getString("message") ?: ""
                        val taskId = change.document.getString("taskId")
                        val docId = change.document.id

                        showLocalNotification(title, message, taskId)
                        db.collection("Notifications").document(docId).update("isRead", true)
                    }
                }
            }
    }

    private fun showLocalNotification(title: String, message: String, taskId: String? = null) {
        val channelId = "task_alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Task Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // URI that points to the Main screen with optional taskId
        val uriStr = if (taskId != null) "taskgo://main?taskId=$taskId" else "taskgo://main"
        val deepLinkUri = Uri.parse(uriStr)

        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri, this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.utmlogo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}