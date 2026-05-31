package com.example.taskgo.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

enum class AppTheme { LIGHT, DARK, SYSTEM }

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    private val _themeState = mutableStateOf(loadTheme())
    val themeState: State<AppTheme> = _themeState

    fun setTheme(theme: AppTheme) {
        _themeState.value = theme
        saveTheme(theme)
    }

    private fun saveTheme(theme: AppTheme) {
        sharedPreferences.edit().putString("app_theme", theme.name).apply()
    }

    private fun loadTheme(): AppTheme {
        val themeName = sharedPreferences.getString("app_theme", AppTheme.SYSTEM.name)
        return try {
            AppTheme.valueOf(themeName ?: AppTheme.SYSTEM.name)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }
}
