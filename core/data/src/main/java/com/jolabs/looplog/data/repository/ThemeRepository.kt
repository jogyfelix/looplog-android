package com.jolabs.looplog.data.repository

import com.jolabs.looplog.model.AppAccent
import com.jolabs.looplog.model.AppThemeMode
import com.jolabs.looplog.model.ThemePreferences
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    /** The user's appearance preferences, defaulting to system/system/1.0 when unset. */
    val preferences: Flow<ThemePreferences>

    suspend fun setMode(mode: AppThemeMode)
    suspend fun setAccent(accent: AppAccent)
    suspend fun setFontScale(scale: Float)
}
