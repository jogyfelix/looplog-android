package com.jolabs.looplog.habit.alarmManager

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent

const val ALARM_NOTIFICATION_CHANNEL_ID = "looplog_alarm_channel"

fun buildOpenAppPendingIntent(context: Context, requestCode: Int = 0): PendingIntent {
    val intent = Intent().apply {
        component = ComponentName(context.packageName, "com.jolabs.looplog.MainActivity")
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
