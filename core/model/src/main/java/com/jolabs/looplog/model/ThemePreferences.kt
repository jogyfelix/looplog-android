package com.jolabs.looplog.model

/** The user's appearance preferences. [fontScale] multiplies the system font scale. */
data class ThemePreferences(
    val mode: AppThemeMode = AppThemeMode.SYSTEM,
    val accent: AppAccent = AppAccent.SYSTEM,
    val fontScale: Float = 1.0f
)
