package com.runanywhere.startup_hackathon20.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple60,
    secondary = Teal60,
    tertiary = Yellow60,
    background = DarkBackground,
    surface = CardDark,
    primaryContainer = DeepPurple,
    secondaryContainer = DeepTeal,
    tertiaryContainer = Gold
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = Teal40,
    tertiary = Yellow40,
    background = LightBackground,
    surface = CardLight,
    primaryContainer = Purple80,
    secondaryContainer = Teal80,
    tertiaryContainer = Yellow80
)

@Composable
fun Startup_hackathon20Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}