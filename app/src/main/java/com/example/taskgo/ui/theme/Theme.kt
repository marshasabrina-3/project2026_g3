package com.example.taskgo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UTMColorScheme = lightColorScheme(
    primary = UTM_Maroon,
    secondary = UTM_Gold,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = Color(0xFFB00020)
)

@Composable
fun TaskGOTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UTMColorScheme,
        typography = Typography,
        content = content
    )
}
