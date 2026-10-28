package com.jolabs.looplog.storage.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.appLockDataStore by preferencesDataStore(name = "app_lock_prefs")
private val lockModeKey = stringPreferencesKey("app_lock_mode")

/** Persists the selected app-lock mode by name; emits null until the user picks one. */
class BiometricStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val lockMode: Flow<String?> = context.appLockDataStore.data.map { it[lockModeKey] }

    suspend fun setLockMode(modeName: String) {
        context.appLockDataStore.edit { it[lockModeKey] = modeName }
    }
}
