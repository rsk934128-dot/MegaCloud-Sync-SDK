package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Cyber / MEGA Dark Palette
val CyanPrimary = Color(0xFF00E5FF)
val CyanPrimaryDark = Color(0xFF00B4D8)
val MegaRed = Color(0xFFD92338)
val MegaRedLight = Color(0xFFFF5252)
val SecurityGreen = Color(0xFF00E676)
val SecurityAmber = Color(0xFFFFAB00)
val ElectricPurple = Color(0xFFA855F7)

// Standard Dark Palette
val DarkBackground = Color(0xFF0B111E)
val DarkSurface = Color(0xFF10192A)
val DarkSurfaceVariant = Color(0xFF17243C)
val DarkBorder = Color(0xFF223554)

// OLED Pure Black Palette
val OledBackground = Color(0xFF000000)
val OledSurface = Color(0xFF090D14)
val OledSurfaceVariant = Color(0xFF121824)
val OledBorder = Color(0xFF1E2838)

val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Light Palette
val LightBackground = Color(0xFFF4F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightBorder = Color(0xFFCBD5E1)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)

enum class AccentColorOption(val label: String, val primary: Color, val container: Color) {
    CYAN("Cyber Cyan", CyanPrimary, Color(0xFF004D61)),
    CRIMSON("MEGA Crimson", Color(0xFFFF2A55), Color(0xFF5C000E)),
    GREEN("Emerald Green", SecurityGreen, Color(0xFF005326)),
    AMBER("Sunset Amber", SecurityAmber, Color(0xFF5A3900)),
    PURPLE("Electric Purple", ElectricPurple, Color(0xFF4C1D95))
}
