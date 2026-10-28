package com.jolabs.looplog.storage.datastore

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal const val CURRENT_AI_NOTICE_VERSION = 1

private val legacyAiPrivacyAcceptedKey = booleanPreferencesKey("ai_privacy_accepted")
private val aiNoticeVersionKey = intPreferencesKey("ai_notice_version")
private val incompatibleModelAppVersionKey = longPreferencesKey("incompatible_model_app_version")

internal fun Preferences.hasAcknowledgedCurrentAiNotice(): Boolean =
    this[aiNoticeVersionKey] == CURRENT_AI_NOTICE_VERSION

internal class AiPreferencesMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.contains(legacyAiPrivacyAcceptedKey)

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            remove(legacyAiPrivacyAcceptedKey)
        }

    override suspend fun cleanUp() = Unit
}

private val Context.aiPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_prefs",
    produceMigrations = { listOf(AiPreferencesMigration()) },
)

@Singleton
class AiPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isAiNoticeAcknowledged: Flow<Boolean> = context.aiPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(Preferences::hasAcknowledgedCurrentAiNotice)

    suspend fun acknowledgeAiNotice() {
        context.aiPrefsDataStore.edit { preferences ->
            preferences.remove(legacyAiPrivacyAcceptedKey)
            preferences[aiNoticeVersionKey] = CURRENT_AI_NOTICE_VERSION
        }
    }

    suspend fun isModelPackIncompatible(appVersionCode: Long): Boolean =
        context.aiPrefsDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { it[incompatibleModelAppVersionKey] == appVersionCode }
            .first()

    suspend fun markModelPackIncompatible(appVersionCode: Long) {
        context.aiPrefsDataStore.edit { preferences ->
            preferences[incompatibleModelAppVersionKey] = appVersionCode
        }
    }
}
