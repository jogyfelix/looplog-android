package com.jolabs.looplog.storage.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
private val themeModeKey = stringPreferencesKey("theme_mode")
private val accentKey = stringPreferencesKey("theme_accent")
private val fontScaleKey = floatPreferencesKey("theme_font_scale")

/** Persists appearance preferences; each flow emits null/absent until the user changes it. */
class ThemeStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val modeName: Flow<String?> = context.themeDataStore.data.map { it[themeModeKey] }
    val accentName: Flow<String?> = context.themeDataStore.data.map { it[accentKey] }
    val fontScale: Flow<Float?> = context.themeDataStore.data.map { it[fontScaleKey] }

    suspend fun setModeName(name: String) {
        context.themeDataStore.edit { it[themeModeKey] = name }
    }

    suspend fun setAccentName(name: String) {
        context.themeDataStore.edit { it[accentKey] = name }
    }

    suspend fun setFontScale(scale: Float) {
        context.themeDataStore.edit { it[fontScaleKey] = scale }
    }
}
