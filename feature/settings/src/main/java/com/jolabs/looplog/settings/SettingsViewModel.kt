package com.jolabs.looplog.settings

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.AppLockRepository
import com.jolabs.looplog.data.repository.SettingsRepository
import com.jolabs.looplog.data.repository.ThemeRepository
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.model.AppAccent
import com.jolabs.looplog.model.AppLockMode
import com.jolabs.looplog.model.AppThemeMode
import com.jolabs.looplog.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Settings screen. The notification/alarm toggles mirror the current OS
 * permission state rather than an in-app preference, so state is re-derived from the
 * system on demand (see [refreshPermissionState]). The app-lock mode, by contrast, is a
 * persisted in-app preference sourced from [AppLockRepository].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitAlarmManager: HabitAlarmManager,
    private val appLockRepository: AppLockRepository,
    private val themeRepository: ThemeRepository,
    private val settingsRepository: SettingsRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _alarmsAllowed = MutableStateFlow(false)
    val alarmsAllowed: StateFlow<Boolean> = _alarmsAllowed.asStateFlow()

    val appLockMode: StateFlow<AppLockMode> = appLockRepository.lockMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLockMode.DISABLED)

    val themePreferences: StateFlow<ThemePreferences> = themeRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreferences())

    val showHabitSections: StateFlow<Boolean> = settingsRepository.showHabitSections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Re-reads the current OS notification and exact-alarm permission state. */
    fun refreshPermissionState() {
        _notificationsEnabled.value = NotificationManagerCompat.from(context).areNotificationsEnabled()
        _alarmsAllowed.value = habitAlarmManager.checkAlarmPermission()
    }

    fun setAppLockMode(mode: AppLockMode) {
        viewModelScope.launch {
            appLockRepository.setLockMode(mode)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { themeRepository.setMode(mode) }
    }

    fun setAccent(accent: AppAccent) {
        viewModelScope.launch { themeRepository.setAccent(accent) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { themeRepository.setFontScale(scale) }
    }

    fun setShowHabitSections(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowHabitSections(show) }
    }

    fun trackItemClick(item: SettingsAnalytics.Item) {
        analyticsTracker.trackEvent(SettingsAnalytics.ItemClicked(item))
    }
}
