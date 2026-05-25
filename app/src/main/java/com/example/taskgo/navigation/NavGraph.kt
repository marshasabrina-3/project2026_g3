package com.example.taskgo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.taskgo.data.model.UserRole
import com.example.taskgo.ui.screens.*
import com.example.taskgo.ui.viewmodel.UserViewModel
import androidx.compose.runtime.LaunchedEffect
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object Admin : Screen("admin")
}

@Composable
fun TaskGONavGraph(navController: NavHostController) {
    val userViewModel: UserViewModel = viewModel()
    val currentUser by userViewModel.currentUser.collectAsState()

    val startDestination = remember(currentUser) {
        if (currentUser != null) {
            if (currentUser?.role == UserRole.ADMIN) Screen.Admin.route else Screen.Main.route
        } else {
            Screen.Login.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                userViewModel = userViewModel,
                // FIXED FOR TASK #159: Changed parameter type from String to UserRole enum
                onLoginSuccess = { role ->
                    val destination = if (role == UserRole.ADMIN) Screen.Admin.route else Screen.Main.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
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

        // UPDATED THIS SECTION FOR DEEP LINK
        composable(
            route = Screen.Main.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "taskgo://main" }
            )
        ) {
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
            // FIXED FOR TASK #160: Added a security guard check block
            // If profile loads and user is NOT an admin, immediately bounce them to the Login/Main entrance
            if (currentUser != null && currentUser?.role != UserRole.ADMIN) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                AdminHomeScreen(
                    taskViewModel = viewModel(),
                    onLogout = {
                        userViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}