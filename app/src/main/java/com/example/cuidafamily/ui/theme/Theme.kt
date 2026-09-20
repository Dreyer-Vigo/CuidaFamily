package com.example.cuidafamily.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GradientStarStart,
    primaryContainer = LavandaPrimaryLight,
    secondary = GradientStarEnd,
    error = RojoAlerta,
    errorContainer = RojoAlertaFondo,
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF2D2D2D)
)

private val LightColorScheme = lightColorScheme(
    primary = GradientStarEnd, // Violeta como primario
    primaryContainer = LavandaPrimaryLight,
    secondary = GradientStarStart, // Rosa como secundario
    error = RojoAlerta,
    errorContainer = RojoAlertaFondo,
    background = FondoNeutro,
    surface = FondoNeutro
)

@Composable
fun CuidaFamilyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
