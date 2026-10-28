package com.jolabs.looplog.habit

import com.jolabs.looplog.analytics.model.AnalyticsEvent
import com.jolabs.looplog.analytics.model.ErrorSource
import com.jolabs.looplog.analytics.model.Feature
import com.jolabs.looplog.model.HabitStatus

/**
 * Analytics definitions for the Habit feature.
 */
object HabitAnalytics {

    val Feature = Feature("habits")

    object Sources {
        val Create = ErrorSource("habit_create")
        val Update = ErrorSource("habit_update")
        val Delete = ErrorSource("habit_delete")
        val Entry = ErrorSource("habit_entry")
        val Detail = ErrorSource("habit_detail")
        val AlarmVerification = ErrorSource("alarm_verification")
        val AlarmReceive = ErrorSource("alarm_receive")
        val AppFunction = ErrorSource("app_function")
        val AlarmRetry = ErrorSource("alarm_retry")
        val Home = ErrorSource("habit_home")
        val Database = ErrorSource("habit_database")
        val UseCase = ErrorSource("habit_use_case")
    }

    /** Where a habit entry status change was initiated from. */
    object EntrySources {
        const val Home = "home"
        const val Widget = "widget"
    }

    data class HabitCreated(
        val habitType: String,
        val hasReminder: Boolean,
        val repeatDayCount: Int,
    ) : AnalyticsEvent {
        override val name = "habit_created"
        override val params = mapOf(
            "habit_type" to habitType,
            "has_reminder" to hasReminder,
            "repeat_day_count" to repeatDayCount,
        )
    }

    data class HabitUpdated(
        val habitType: String,
        val hasReminder: Boolean,
        val repeatDayCount: Int,
    ) : AnalyticsEvent {
        override val name = "habit_updated"
        override val params = mapOf(
            "habit_type" to habitType,
            "has_reminder" to hasReminder,
            "repeat_day_count" to repeatDayCount,
        )
    }

    data object HabitDeleted : AnalyticsEvent {
        override val name = "habit_deleted"
    }

    /** Fired when the user changes a habit's status for a day, from [source] (see [EntrySources]). */
    data class HabitStatusChanged(
        val status: HabitStatus,
        val source: String,
    ) : AnalyticsEvent {
        override val name = when (status) {
            HabitStatus.COMPLETED -> "habit_completed"
            HabitStatus.SKIPPED -> "habit_skipped"
            HabitStatus.NONE -> "habit_unchecked"
        }
        override val params = mapOf("source" to source)
    }
}
