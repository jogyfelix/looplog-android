package com.jolabs.looplog.data.repository

import android.content.Context
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.ErrorSource
import com.jolabs.looplog.data.backup.BackupCrypto
import com.jolabs.looplog.data.backup.BackupData
import com.jolabs.looplog.data.backup.BackupDecryptException
import com.jolabs.looplog.data.backup.BackupFile
import com.jolabs.looplog.data.backup.BackupHeaderInfo
import com.jolabs.looplog.data.backup.EntryDto
import com.jolabs.looplog.data.backup.HabitDto
import com.jolabs.looplog.data.backup.NotABackupException
import com.jolabs.looplog.data.backup.RepeatDto
import com.jolabs.looplog.data.backup.SettingsDto
import com.jolabs.looplog.data.backup.StreakDto
import com.jolabs.looplog.storage.dao.HabitDao
import com.jolabs.looplog.storage.datastore.BiometricStore
import com.jolabs.looplog.storage.datastore.ThemeStore
import com.jolabs.looplog.storage.entity.HabitEntryStatus
import com.jolabs.looplog.storage.entity.HabitEntryTable
import com.jolabs.looplog.storage.entity.HabitTable
import com.jolabs.looplog.storage.entity.RepeatTable
import com.jolabs.looplog.storage.entity.StreakTable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject

private val BACKUP_ERROR_SOURCE = ErrorSource("backup")

/** Keep in sync with HabitDatabase's @Database(version = …). */
private const val CURRENT_DB_VERSION = 3

class BackupRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val themeStore: ThemeStore,
    private val biometricStore: BiometricStore,
    private val crypto: BackupCrypto,
    private val analyticsTracker: AnalyticsTracker
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(password: String?, languageTag: String?): ByteArray {
        try {
            val habits = habitDao.getAllHabitsList().map {
                HabitDto(it.id, it.name, it.description, it.createdAt, it.updatedAt)
            }
            val entries = habitDao.getAllEntries().map {
                EntryDto(it.habitId, it.date, it.isCompleted?.name, it.updatedAtMillis)
            }
            val repeats = habitDao.getAllRepeats().map {
                RepeatDto(it.habitId, it.dayOfWeek.name, it.timeOfDay)
            }
            val streaks = habitDao.getAllStreaks().map {
                StreakDto(it.habitId, it.currentStreak, it.longestStreak)
            }
            val settings = SettingsDto(
                themeMode = themeStore.modeName.first(),
                accent = themeStore.accentName.first(),
                fontScale = themeStore.fontScale.first(),
                appLockMode = biometricStore.lockMode.first(),
                languageTag = languageTag
            )
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            val file = BackupFile(
                appVersionName = pkg.versionName.orEmpty(),
                appVersionCode = pkg.longVersionCode,
                dbVersion = CURRENT_DB_VERSION,
                exportedAtEpochMillis = System.currentTimeMillis(),
                data = BackupData(habits, entries, repeats, streaks, settings)
            )
            val plaintext = gzip(json.encodeToString(file).toByteArray(Charsets.UTF_8))
            return crypto.encrypt(plaintext, password)
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Hard(e, BACKUP_ERROR_SOURCE, "Backup export failed")
            )
            throw e
        }
    }

    override fun readHeader(bytes: ByteArray): BackupHeaderInfo? = try {
        crypto.readHeader(bytes)
    } catch (_: NotABackupException) {
        null
    }

    override suspend fun import(bytes: ByteArray, password: String?): BackupImportResult {
        val plaintext = try {
            crypto.decrypt(bytes, password)
        } catch (_: NotABackupException) {
            return BackupImportResult.NotABackupFile
        } catch (_: BackupDecryptException) {
            return BackupImportResult.WrongPasswordOrCorrupt
        }

        return try {
            val file = json.decodeFromString<BackupFile>(ungzip(plaintext).toString(Charsets.UTF_8))
            if (file.dbVersion > CURRENT_DB_VERSION) {
                return BackupImportResult.IncompatibleVersion(file.dbVersion, CURRENT_DB_VERSION)
            }

            val habits = file.data.habits.map {
                HabitTable(it.id, it.name, it.description, it.createdAt, it.updatedAt)
            }
            val entries = file.data.entries.map {
                HabitEntryTable(
                    habitId = it.habitId,
                    date = it.date,
                    isCompleted = it.status?.let { s -> HabitEntryStatus.valueOf(s) },
                    updatedAtMillis = it.updatedAtMillis
                )
            }
            val repeats = file.data.repeats.map {
                RepeatTable(it.habitId, DayOfWeek.valueOf(it.dayOfWeek), it.timeOfDay)
            }
            val streaks = file.data.streaks.map {
                StreakTable(it.habitId, it.currentStreak, it.longestStreak)
            }

            habitDao.replaceAllData(habits, entries, repeats, streaks)
            applySettings(file.data.settings)

            BackupImportResult.Success(habits.size, file.data.settings.languageTag)
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Hard(e, BACKUP_ERROR_SOURCE, "Backup import failed")
            )
            BackupImportResult.Failed
        }
    }

    private suspend fun applySettings(settings: SettingsDto) {
        settings.themeMode?.let { themeStore.setModeName(it) }
        settings.accent?.let { themeStore.setAccentName(it) }
        settings.fontScale?.let { themeStore.setFontScale(it) }
        settings.appLockMode?.let { biometricStore.setLockMode(it) }
        // languageTag is returned to the caller to apply via AppCompatDelegate.
    }

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    private fun ungzip(data: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
}
