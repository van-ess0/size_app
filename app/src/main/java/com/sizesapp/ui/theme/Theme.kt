package com.sizesapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF3949AB)
private val IndigoDark = Color(0xFF7986CB)

private val LightColors = lightColorScheme(
    primary = Indigo,
    secondary = Color(0xFF00897B),
)

private val DarkColors = darkColorScheme(
    primary = IndigoDark,
    secondary = Color(0xFF4DB6AC),
)

@Composable
fun SizesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
