package com.jolabs.looplog.habit.alarmManager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.habit.workers.DailyAlarmVerificationWorker

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
        private const val WORK_NAME = "reschedule_alarms"
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"

        // AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (API 31+). The
        // system cancels ALL exact alarms when the permission is revoked and sends this when
        // it's granted back, so we must reschedule here or reminders stay dead until 3 AM.
        private const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        private val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_QUICKBOOT_POWERON,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            ACTION_EXACT_ALARM_PERMISSION_CHANGED
        )
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action in RESCHEDULE_ACTIONS) {
            context?.let { ctx ->
                try {
                    Log.d(TAG, "Rescheduling alarms due to ${intent?.action}")
                    val workManager = WorkManager.getInstance(ctx)
                    val workRequest = OneTimeWorkRequestBuilder<DailyAlarmVerificationWorker>().build()
                    workManager.enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                    WidgetUpdateUtil.requestHabitWidgetsRefresh(ctx)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to enqueue alarm reschedule work", e)
                    e.printStackTrace()
                }
            }
        }
    }
}
