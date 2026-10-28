package com.jolabs.looplog.storage.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val Context.updateDataStore by preferencesDataStore(name = "update_prefs")
private val graceStartedAtKey = longPreferencesKey("mandatory_grace_started_at")

/** Persists when the mandatory-update grace period started, across app restarts. */
class UpdateGraceStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend fun getGraceStartedAt(): Long? {
        return context.updateDataStore.data.first()[graceStartedAtKey]
    }

    suspend fun setGraceStartedAt(timestamp: Long) {
        context.updateDataStore.edit { it[graceStartedAtKey] = timestamp }
    }

    suspend fun clearGraceStarted() {
        context.updateDataStore.edit { it.remove(graceStartedAtKey) }
    }
}
