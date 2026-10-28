package com.jolabs.looplog.habit.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.workers.UpdateHabitsWorker
import com.jolabs.looplog.habit.widgets.habitList.ToggleHabitWidget
import com.jolabs.looplog.habit.widgets.habitList.encodeHabits
import com.jolabs.looplog.habit.widgets.habitList.toDto
import com.jolabs.looplog.util.DateUtils.todayEpochDay
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

object WidgetUpdateUtil {
    private const val TAG = "WidgetUpdateUtil"
    private const val REFRESH_WORK_NAME = "widget_refresh_on_change"

    fun requestHabitWidgetsRefresh(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            REFRESH_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<UpdateHabitsWorker>().build()
        )
    }

    suspend fun refreshHabitWidgets(context: Context, repository: HabitRepository? = null) {
        Log.d(TAG, "refreshHabitWidgets() called")

        try {
                val repo = repository ?: EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    com.jolabs.looplog.habit.workers.WorkerEntryPoint::class.java
                ).habitRepository()

                val today = LocalDate.now()
                val epochDay = todayEpochDay()

                Log.d(TAG, "Fetching habits for: ${today.dayOfWeek}, date: $today, epochDay: $epochDay")

                val list = repo.getHabitByDateOnce(today.dayOfWeek, epochDay)

                Log.d(TAG, "Fetched ${list.size} habit(s) for today")
                list.forEachIndexed { index, habit ->
                    Log.d(TAG, "  [$index] ${habit.name} - Status: ${habit.habitState}")
                }

                val json = encodeHabits(list.toDto())
                Log.d(TAG, "Encoded habits to JSON (${json.length} chars)")

                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(ToggleHabitWidget::class.java)

                Log.d(TAG, "Found ${ids.size} widget instance(s) to update")

                val lastUpdateDayKey = longPreferencesKey("last_update_epoch_day")
                ids.forEachIndexed { index, id ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                        val editable = prefs.toMutablePreferences()
                        editable[stringPreferencesKey("habits_json")] = json
                        editable[lastUpdateDayKey] = epochDay
                        editable[booleanPreferencesKey("is_loading")] = false
                        editable
                    }
                    Log.d(TAG, "  Updated widget instance [$index]: $id")
                }

                ToggleHabitWidget().updateAll(context)
                Log.d(TAG, "Called updateAll() to refresh widget UI")
            Log.d(TAG, "Widget refresh completed successfully")

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing widgets: ${e.message}", e)
            throw e
        }
    }
}
