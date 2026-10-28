package com.jolabs.looplog.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.jolabs.looplog.R
import com.jolabs.looplog.habit.R as HabitR
import com.jolabs.looplog.settings.R as SettingsR

/**
 * Bottom navigation item configuration
 */
sealed class BottomNavItem(
    val route: Any,
    @DrawableRes val icon: Int,
    @StringRes val label: Int
) {
    data object Habits : BottomNavItem(
        route = HabitsTab,
        icon = HabitR.drawable.calendar_month,
        label = R.string.nav_habits
    )

    data object Stats : BottomNavItem(
        route = StatsTab,
        icon = HabitR.drawable.bar_chart,
        label = R.string.nav_stats
    )

    data object Settings : BottomNavItem(
        route = SettingsTab,
        icon = SettingsR.drawable.settings,
        label = SettingsR.string.nav_settings
    )

    companion object {
        val items = listOf(Habits, Stats, Settings)
    }
}
