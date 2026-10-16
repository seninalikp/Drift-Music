package com.soundify.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class DriftColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color = Color(0xFF38BDF8),
    val border: Color,
    val isDark: Boolean
)

val DarkDriftColors = DriftColors(
    background = Color(0xFF121212),
    surface = Color(0xFF18181B),
    card = Color(0xFF222226),
    textPrimary = Color.White,
    textSecondary = Color.White.copy(alpha = 0.65f),
    accent = Color(0xFF38BDF8),
    border = Color.White.copy(alpha = 0.12f),
    isDark = true
)

val LightDriftColors = DriftColors(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFF1F5F9),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    accent = Color(0xFF0284C7),
    border = Color(0xFFE2E8F0),
    isDark = false
)

val LocalDriftColors = staticCompositionLocalOf { DarkDriftColors }

object DriftTheme {
    val colors: DriftColors
        @Composable
        get() = LocalDriftColors.current
}
