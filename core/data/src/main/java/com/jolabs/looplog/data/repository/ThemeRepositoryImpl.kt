package com.jolabs.looplog.data.repository

import com.jolabs.looplog.model.AppAccent
import com.jolabs.looplog.model.AppThemeMode
import com.jolabs.looplog.model.ThemePreferences
import com.jolabs.looplog.storage.datastore.ThemeStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor(
    private val themeStore: ThemeStore
) : ThemeRepository {

    override val preferences: Flow<ThemePreferences> = combine(
        themeStore.modeName,
        themeStore.accentName,
        themeStore.fontScale
    ) { modeName, accentName, fontScale ->
        ThemePreferences(
            mode = modeName?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.SYSTEM,
            accent = accentName?.let { runCatching { AppAccent.valueOf(it) }.getOrNull() }
                ?: AppAccent.SYSTEM,
            fontScale = fontScale ?: 1.0f
        )
    }

    override suspend fun setMode(mode: AppThemeMode) = themeStore.setModeName(mode.name)

    override suspend fun setAccent(accent: AppAccent) = themeStore.setAccentName(accent.name)

    override suspend fun setFontScale(scale: Float) = themeStore.setFontScale(scale)
}
