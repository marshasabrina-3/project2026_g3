package com.example.taskgo.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.taskgo.data.model.UserRole
import com.example.taskgo.data.model.UserStatus
import com.example.taskgo.ui.screens.*
import com.example.taskgo.ui.viewmodel.UserViewModel
import androidx.compose.runtime.LaunchedEffect

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object Admin : Screen("admin")
    object Restricted : Screen("restricted")
}

@Composable
fun TaskGONavGraph(navController: NavHostController) {
    val userViewModel: UserViewModel = viewModel()
    val currentUser by userViewModel.currentUser.collectAsState()
    val isInitializing by userViewModel.isInitializing.collectAsState()

    val startDestination = remember(currentUser, isInitializing) {
        if (isInitializing) {
            Screen.Login.route
        } else if (currentUser != null) {
            if (currentUser?.status == UserStatus.BANNED || currentUser?.status == UserStatus.SUSPENDED) {
                Screen.Restricted.route
            } else if (currentUser?.role == UserRole.ADMIN) {
                Screen.Admin.route
            } else {
                Screen.Main.route
            }
        } else {
            Screen.Login.route
        }
    }

    if (isInitializing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF800000))
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    userViewModel = userViewModel,
                    onLoginSuccess = { role ->
                        val user = userViewModel.currentUser.value
                        if (user?.status == UserStatus.BANNED || user?.status == UserStatus.SUSPENDED) {
                            navController.navigate(Screen.Restricted.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            val destination = if (role == UserRole.ADMIN) Screen.Admin.route else Screen.Main.route
                            navController.navigate(destination) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Main.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "taskgo://main" }
                )
            ) {
                if (currentUser?.status == UserStatus.BANNED || currentUser?.status == UserStatus.SUSPENDED) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Restricted.route) { popUpTo(0) { inclusive = true } }
                    }
                }
                MainContainerScreen(
                    userViewModel = userViewModel,
                    onLogout = {
                        userViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Admin.route) {
                if (currentUser != null && currentUser?.role != UserRole.ADMIN) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Main.route) { popUpTo(0) { inclusive = true } }
                    }
                } else if (currentUser == null) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }
                } else {
                    // FIXED: Passed userViewModel argument to accommodate AdminHomeScreen's new signature
                    AdminHomeScreen(
                        taskViewModel = viewModel(),
                        userViewModel = userViewModel,
                        onLogout = {
                            userViewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(Screen.Restricted.route) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentUser?.status == UserStatus.BANNED) "ACCOUNT BANNED" else "ACCOUNT SUSPENDED",
                            color = Color(0xFF800000),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (currentUser?.status == UserStatus.BANNED) {
                                "Your access to TaskGO has been permanently terminated due to policy violations."
                            } else {
                                "Your account is temporarily frozen. Please check back later or contact support."
                            },
                            color = Color.Gray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                userViewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))
                        ) {
                            Text("Back to Login", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}