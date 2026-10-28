package com.jolabs.looplog.navigation

import com.jolabs.looplog.analytics.model.AnalyticsEvent

/**
 * Analytics definitions for app-level navigation.
 */
object AppAnalytics {

    object Screens {
        const val EmptyState = "empty_state"
        const val Home = "home"
        const val Stats = "stats"
        const val Settings = "settings"
        const val Appearance = "appearance"
        const val HabitCreate = "habit_create"
        const val HabitEdit = "habit_edit"
        const val HabitDetail = "habit_detail"
    }

    /**
     * Uses GA4's reserved `screen_view` event so Compose screens show up in
     * path exploration and user explorer; Firebase only auto-tracks Activities.
     */
    data class ScreenViewed(val screenName: String) : AnalyticsEvent {
        override val name = "screen_view"
        override val params = mapOf("screen_name" to screenName)
    }
}
