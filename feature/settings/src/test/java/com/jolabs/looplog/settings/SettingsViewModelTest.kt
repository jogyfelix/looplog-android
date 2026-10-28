package com.jolabs.looplog.settings

import android.content.Context
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.AppLockRepository
import com.jolabs.looplog.data.repository.SettingsRepository
import com.jolabs.looplog.data.repository.ThemeRepository
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.model.AppLockMode
import com.jolabs.looplog.model.ThemePreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class SettingsViewModelTest {

    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private val appLockRepository: AppLockRepository = mockk {
        every { lockMode } returns flowOf(AppLockMode.DISABLED)
    }
    private val themeRepository: ThemeRepository = mockk {
        every { preferences } returns flowOf(ThemePreferences())
    }
    private val settingsRepository: SettingsRepository = mockk {
        every { showHabitSections } returns flowOf(false)
    }

    private val viewModel = SettingsViewModel(
        context = mockk<Context>(relaxed = true),
        habitAlarmManager = mockk<HabitAlarmManager>(relaxed = true),
        appLockRepository = appLockRepository,
        themeRepository = themeRepository,
        settingsRepository = settingsRepository,
        analyticsTracker = analyticsTracker
    )

    @ParameterizedTest
    @EnumSource(SettingsAnalytics.Item::class)
    fun `each settings item click emits its analytics value`(item: SettingsAnalytics.Item) {
        viewModel.trackItemClick(item)

        verify(exactly = 1) {
            analyticsTracker.trackEvent(SettingsAnalytics.ItemClicked(item))
        }
    }
}
