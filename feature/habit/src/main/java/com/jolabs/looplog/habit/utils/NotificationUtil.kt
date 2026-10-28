package com.jolabs.looplog.habit.utils

import android.content.Context
import androidx.core.app.NotificationManagerCompat

object NotificationUtil {
    /**
     * Cancels the notification for a specific habit if it exists.
     * The notification ID is assumed to be the habit's ID (toInt).
     */
    fun cancelHabitNotification(context: Context, habitId: Long) {
        NotificationManagerCompat.from(context).cancel(habitId.toInt())
    }
}
