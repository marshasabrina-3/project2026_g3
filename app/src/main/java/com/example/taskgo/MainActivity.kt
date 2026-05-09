package com.example.taskgo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.taskgo.navigation.TaskGONavGraph
import com.example.taskgo.ui.theme.TaskGOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskGOTheme {
                val navController = rememberNavController()
                TaskGONavGraph(navController = navController)
            }
        }
    }
}
