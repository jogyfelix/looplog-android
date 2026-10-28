package com.jolabs.looplog.habit.widgets.habitList

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil

class RefreshHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetUpdateUtil.refreshHabitWidgets(context)
    }
}
