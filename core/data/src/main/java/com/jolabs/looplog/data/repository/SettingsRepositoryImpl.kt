package com.jolabs.looplog.data.repository

import com.jolabs.looplog.storage.datastore.SettingsPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) : SettingsRepository {

    override val showHabitSections: Flow<Boolean> = settingsPreferences.showHabitSections

    override suspend fun setShowHabitSections(show: Boolean) {
        settingsPreferences.setShowHabitSections(show)
    }
}
