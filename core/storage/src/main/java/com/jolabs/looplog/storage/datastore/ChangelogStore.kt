package com.jolabs.looplog.storage.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val Context.changelogDataStore by preferencesDataStore(name = "changelog_prefs")
private val lastShownVersionCodeKey = longPreferencesKey("last_shown_version_code")

class ChangelogStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun getLastShownVersionCode(): Long? =
        context.changelogDataStore.data.first()[lastShownVersionCodeKey]

    suspend fun setLastShownVersionCode(versionCode: Long) {
        context.changelogDataStore.edit { it[lastShownVersionCodeKey] = versionCode }
    }
}
