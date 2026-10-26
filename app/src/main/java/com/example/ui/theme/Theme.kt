package com.example.ui.theme

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

fun getDarkColorScheme(accentOption: AccentColorOption, isOled: Boolean = false) = darkColorScheme(
    primary = accentOption.primary,
    onPrimary = Color(0xFF00202E),
    primaryContainer = accentOption.container,
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = MegaRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5C000E),
    onSecondaryContainer = Color(0xFFFFDADA),
    tertiary = SecurityGreen,
    onTertiary = Color(0xFF003919),
    tertiaryContainer = Color(0xFF005326),
    onTertiaryContainer = Color(0xFF86F8A5),
    background = if (isOled) OledBackground else DarkBackground,
    onBackground = TextPrimary,
    surface = if (isOled) OledSurface else DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = if (isOled) OledSurfaceVariant else DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = if (isOled) OledBorder else DarkBorder,
    error = MegaRedLight
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00758F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBCE9FF),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = MegaRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDADA),
    onSecondaryContainer = Color(0xFF400007),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = MegaRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accentOption: AccentColorOption = AccentColorOption.CYAN,
    isOledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkColorScheme(accentOption, isOledBlack)
        else -> getDarkColorScheme(accentOption, isOledBlack) // Sleek dark aesthetic
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
