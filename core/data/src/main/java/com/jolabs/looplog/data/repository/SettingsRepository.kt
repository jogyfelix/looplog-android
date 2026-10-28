package com.jolabs.looplog.data.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val showHabitSections: Flow<Boolean>
    suspend fun setShowHabitSections(show: Boolean)
}
