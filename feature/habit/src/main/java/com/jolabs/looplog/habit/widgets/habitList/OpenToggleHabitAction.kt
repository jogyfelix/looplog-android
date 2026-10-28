package com.jolabs.looplog.habit.widgets.habitList

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.jolabs.looplog.habit.HabitAnalytics
import com.jolabs.looplog.habit.utils.NotificationUtil
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.habit.widgets.WidgetEntryPoint
import com.jolabs.looplog.model.HabitEntryModel
import com.jolabs.looplog.model.HabitStatus
import com.jolabs.looplog.util.DateUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenToggleHabitAction: ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[HabitIdKey] ?: return
        val date = DateUtils.todayEpochDay()
        val statusName = parameters[StatusKey]

        val currentStatus = statusName?.let { runCatching { HabitStatus.valueOf(it) }.getOrNull() } ?: HabitStatus.NONE

        // Cycle through three states: NONE → COMPLETED → SKIPPED → NONE
        val newStatus = when (currentStatus) {
            HabitStatus.NONE -> HabitStatus.COMPLETED
            HabitStatus.COMPLETED -> HabitStatus.SKIPPED
            HabitStatus.SKIPPED -> HabitStatus.NONE
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val repo = entryPoint.habitRepository()

        try {
            withContext(Dispatchers.IO) {
                repo.upsertHabitEntry(
                    HabitEntryModel(
                        habitId = habitId,
                        date = date,
                        isCompleted = newStatus
                    )
                )
            }
            if (newStatus == HabitStatus.COMPLETED || newStatus == HabitStatus.SKIPPED) {
                NotificationUtil.cancelHabitNotification(context, habitId)
            }
            entryPoint.analyticsTracker().trackEvent(
                HabitAnalytics.HabitStatusChanged(newStatus, HabitAnalytics.EntrySources.Widget)
            )
        } catch (e: Exception) {
            Log.e("OpenToggleHabitAction", "Error updating habitId: $habitId", e)
            return
        }

        try {
            WidgetUpdateUtil.refreshHabitWidgets(context, repo)
        } catch (e: Exception) {
            Log.e("OpenToggleHabitAction", "Error refreshing widget state: $habitId", e)
        }
    }
}
