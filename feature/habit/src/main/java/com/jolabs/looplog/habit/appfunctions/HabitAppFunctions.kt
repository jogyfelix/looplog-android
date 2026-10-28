package com.jolabs.looplog.habit.appfunctions

import android.content.Context
import com.jolabs.looplog.domain.CreateHabitUseCase
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.model.CreateHabit
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.habit.HabitAnalytics
import com.jolabs.looplog.habit.utils.NotificationUtil
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.model.HabitEntryModel
import com.jolabs.looplog.model.HabitStatus
import com.jolabs.looplog.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Habit related logic for AppFunctions.
 * In alpha10, the @AppFunction annotation is moved to the Service entry point.
 */
class HabitAppFunctions @Inject constructor(
    private val createHabitUseCase: CreateHabitUseCase,
    private val habitRepository: HabitRepository,
    private val habitAlarmManager: HabitAlarmManager,
    private val analyticsTracker: AnalyticsTracker
) {
    private fun getLocalizedStatus(context: Context, status: HabitStatus): String {
        return when (status) {
            HabitStatus.NONE -> context.getString(R.string.status_none)
            HabitStatus.COMPLETED -> context.getString(R.string.status_completed)
            HabitStatus.SKIPPED -> context.getString(R.string.status_skipped)
        }
    }

    /**
     * Lists all habits currently configured in the app.
     */
    suspend fun listHabits(context: Context): String = withContext(Dispatchers.IO) {
        val habits = habitRepository.getAllHabitsDirect()
        if (habits.isEmpty()) return@withContext context.getString(R.string.no_habits_created_yet)

        val locale = context.resources.configuration.locales[0]
        val habitList = habits.joinToString("\n") { habit ->
            val days = habit.repeatDays.joinToString(", ") { 
                it.getDisplayName(TextStyle.FULL, locale) 
            }
            "- ${habit.name} (Repeats: $days)"
        }
        "${context.getString(R.string.here_are_your_habits)}\n$habitList"
    }

    /**
     * Creates a new habit with the specified details.
     */
    suspend fun createHabit(
        context: Context,
        name: String,
        daysOfWeek: List<String>,
        description: String? = null,
        alarmTimeMillis: Long? = null
    ): String = withContext(Dispatchers.IO) {
        val days = daysOfWeek.mapNotNull { 
            runCatching { DayOfWeek.valueOf(it.uppercase()) }.getOrNull() 
        }
        
        if (name.isBlank()) return@withContext context.getString(R.string.error_habit_name_empty)
        if (days.isEmpty()) return@withContext context.getString(R.string.error_specify_at_least_one_day)

        try {
            val habitId = createHabitUseCase(
                CreateHabit(
                    id = 0,
                    name = name.trim(),
                    description = description?.trim() ?: "",
                    daysOfWeek = days,
                    timeOfDay = alarmTimeMillis,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            var alarmStatus = ""
            if (alarmTimeMillis != null) {
                if (habitAlarmManager.checkNotificationPermission()) {
                    habitAlarmManager.scheduleHabitAlarm(habitId, name, alarmTimeMillis, days)
                    alarmStatus = context.getString(R.string.alarm_scheduled)
                } else {
                    alarmStatus = context.getString(R.string.alarm_not_scheduled_permissions)
                }
            }

            // Refresh widgets to show the new habit if it's scheduled for today
            WidgetUpdateUtil.requestHabitWidgetsRefresh(context)

            context.getString(R.string.habit_created_successfully, name, alarmStatus)
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Hard(
                    throwable = e,
                    source = HabitAnalytics.Sources.AppFunction,
                    message = "Failed to create habit via AppFunction: $name"
                )
            )
            context.getString(R.string.unknown_error)
        }
    }

    /**
     * Logs the completion state of a habit for today.
     */
    suspend fun logHabitForToday(
        context: Context,
        name: String,
        state: String
    ): String = withContext(Dispatchers.IO) {
        val status = when (state.uppercase()) {
            "NONE" -> HabitStatus.NONE
            "COMPLETED" -> HabitStatus.COMPLETED
            "SKIP", "SKIPPED" -> HabitStatus.SKIPPED
            else -> null
        } ?: return@withContext context.getString(R.string.error_invalid_state, state)

        val habits = habitRepository.getAllHabitsDirect()
        val habit = habits.find { it.name.equals(name, ignoreCase = true) }
            ?: return@withContext context.getString(R.string.error_habit_not_found, name)

        habitRepository.upsertHabitEntry(
            HabitEntryModel(
                habitId = habit.id,
                date = DateUtils.todayEpochDay(),
                isCompleted = status
            )
        )

        if (status == HabitStatus.COMPLETED || status == HabitStatus.SKIPPED) {
            NotificationUtil.cancelHabitNotification(context, habit.id)
        }

        WidgetUpdateUtil.requestHabitWidgetsRefresh(context)

        context.getString(R.string.successfully_logged_habit, name, getLocalizedStatus(context, status))
    }

    /**
     * Logs the completion state for ALL habits scheduled for today.
     */
    suspend fun logAllHabitsForToday(
        context: Context,
        state: String
    ): String = withContext(Dispatchers.IO) {
        val status = when (state.uppercase()) {
            "NONE" -> HabitStatus.NONE
            "COMPLETED" -> HabitStatus.COMPLETED
            "SKIP", "SKIPPED" -> HabitStatus.SKIPPED
            else -> null
        } ?: return@withContext context.getString(R.string.error_invalid_state, state)

        val today = LocalDate.now()
        val epochDay = DateUtils.todayEpochDay()
        
        // Get habits scheduled for today
        val habitsForToday = habitRepository.getHabitByDateOnce(today.dayOfWeek, epochDay)
        
        if (habitsForToday.isEmpty()) {
            return@withContext context.getString(R.string.no_habits_scheduled_today)
        }

        habitsForToday.forEach { habit ->
            habitRepository.upsertHabitEntry(
                HabitEntryModel(
                    habitId = habit.id,
                    date = epochDay,
                    isCompleted = status
                )
            )
            if (status == HabitStatus.COMPLETED || status == HabitStatus.SKIPPED) {
                NotificationUtil.cancelHabitNotification(context, habit.id)
            }
        }

        WidgetUpdateUtil.requestHabitWidgetsRefresh(context)

        context.resources.getQuantityString(
            R.plurals.successfully_marked_habits,
            habitsForToday.size,
            habitsForToday.size,
            getLocalizedStatus(context, status)
        )
    }
}
