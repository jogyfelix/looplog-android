package com.jolabs.looplog.settings.backup

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jolabs.looplog.data.repository.BackupImportResult
import com.jolabs.looplog.data.repository.BackupRepository
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.habit.workers.DailyAlarmVerificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface BackupEvent {
    data object ExportSucceeded : BackupEvent
    data object ExportFailed : BackupEvent
    data object ImportNeedsPassword : BackupEvent
    data object ImportReadyToConfirm : BackupEvent
    data object ImportNotABackup : BackupEvent
    data object ImportWrongPassword : BackupEvent
    data class ImportIncompatible(val backupVersion: Int) : BackupEvent
    data class ImportSucceeded(val habitCount: Int) : BackupEvent
    data object ImportFailed : BackupEvent
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val habitRepository: HabitRepository,
    private val habitAlarmManager: HabitAlarmManager
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = MutableSharedFlow<BackupEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()

    private var pendingImportBytes: ByteArray? = null
    private var pendingImportPassword: String? = null

    /** Serializes + encrypts everything and writes it to the user-chosen [uri]. */
    fun export(uri: Uri, password: String?) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val bytes = backupRepository.export(password, currentLanguageTag())
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: error("Could not open output stream")
                }
                _events.emit(BackupEvent.ExportSucceeded)
            } catch (_: Exception) {
                _events.emit(BackupEvent.ExportFailed)
            } finally {
                _busy.value = false
            }
        }
    }

    /** Reads the picked file and decides whether to prompt for a password or go to confirm. */
    fun onImportFilePicked(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: run {
                    _events.emit(BackupEvent.ImportNotABackup)
                    return@launch
                }
                pendingImportBytes = bytes
                pendingImportPassword = null
                val header = backupRepository.readHeader(bytes)
                when {
                    header == null -> _events.emit(BackupEvent.ImportNotABackup)
                    header.isPasswordProtected -> _events.emit(BackupEvent.ImportNeedsPassword)
                    else -> _events.emit(BackupEvent.ImportReadyToConfirm)
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun onImportPasswordEntered(password: String) {
        pendingImportPassword = password
        _events.tryEmit(BackupEvent.ImportReadyToConfirm)
    }

    /** Performs the destructive restore after the user confirms. */
    fun confirmImport() {
        val bytes = pendingImportBytes ?: return
        viewModelScope.launch {
            _busy.value = true
            try {
                // Cancel OS alarms for the habits that are about to be replaced, so no reminder
                // is left armed for an id the restore removes.
                val existing = withContext(Dispatchers.IO) { habitRepository.getAllHabitsDirect() }
                existing.forEach { habitAlarmManager.cancelAllHabitAlarms(it.id) }

                when (val result = backupRepository.import(bytes, pendingImportPassword)) {
                    is BackupImportResult.Success -> {
                        result.languageTag?.let(::applyLanguage)
                        rearmAlarmsAndWidgets()
                        clearPending()
                        _events.emit(BackupEvent.ImportSucceeded(result.habitCount))
                    }
                    BackupImportResult.WrongPasswordOrCorrupt -> {
                        // Nothing was wiped (decrypt happens before restore); re-arm the untouched data.
                        rearmAlarmsAndWidgets()
                        _events.emit(BackupEvent.ImportWrongPassword)
                    }
                    BackupImportResult.NotABackupFile -> {
                        rearmAlarmsAndWidgets()
                        _events.emit(BackupEvent.ImportNotABackup)
                    }
                    is BackupImportResult.IncompatibleVersion -> {
                        rearmAlarmsAndWidgets()
                        _events.emit(BackupEvent.ImportIncompatible(result.backupDbVersion))
                    }
                    BackupImportResult.Failed -> {
                        rearmAlarmsAndWidgets()
                        _events.emit(BackupEvent.ImportFailed)
                    }
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun cancelImport() = clearPending()

    private fun clearPending() {
        pendingImportBytes = null
        pendingImportPassword = null
    }

    /** Re-arms alarms for whatever habits currently exist and refreshes the widgets. */
    private suspend fun rearmAlarmsAndWidgets() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "import_alarm_verification",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DailyAlarmVerificationWorker>().build()
        )
        WidgetUpdateUtil.refreshHabitWidgets(context)
    }

    private fun applyLanguage(tag: String?) {
        val locales = if (tag.isNullOrEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private fun currentLanguageTag(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) null else locales.get(0)?.toLanguageTag()
    }
}
