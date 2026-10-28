package com.jolabs.looplog.data.backup

import kotlinx.serialization.Serializable

/** Bump when the JSON payload shape changes in an incompatible way. */
const val BACKUP_FORMAT_VERSION = 1

/**
 * Top-level backup payload (the plaintext that gets gzipped + encrypted). DTOs are decoupled
 * from the Room entities on purpose, so schema changes don't break the file format.
 */
@Serializable
data class BackupFile(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val appVersionName: String,
    val appVersionCode: Long,
    val dbVersion: Int,
    val exportedAtEpochMillis: Long,
    val data: BackupData
)

@Serializable
data class BackupData(
    val habits: List<HabitDto>,
    val entries: List<EntryDto>,
    val repeats: List<RepeatDto>,
    val streaks: List<StreakDto>,
    val settings: SettingsDto
)

@Serializable
data class HabitDto(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class EntryDto(
    val habitId: Long,
    val date: Long,
    val status: String?,
    val updatedAtMillis: Long
)

@Serializable
data class RepeatDto(
    val habitId: Long,
    val dayOfWeek: String,
    val timeOfDay: Long?
)

@Serializable
data class StreakDto(
    val habitId: Long,
    val currentStreak: Int,
    val longestStreak: Int
)

/** User-meaningful settings carried in the backup. All nullable so a partial/older file imports. */
@Serializable
data class SettingsDto(
    val themeMode: String? = null,
    val accent: String? = null,
    val fontScale: Float? = null,
    val appLockMode: String? = null,
    val languageTag: String? = null
)
