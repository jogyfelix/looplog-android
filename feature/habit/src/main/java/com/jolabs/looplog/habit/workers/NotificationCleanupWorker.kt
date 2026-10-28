package com.jolabs.looplog.habit.workers

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.ZoneId
import com.jolabs.looplog.habit.alarmManager.GROUP_KEY_HABIT_REMINDERS
import kotlinx.coroutines.CancellationException

@HiltWorker
class NotificationCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "NotificationCleanupWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting daily notification cleanup")
        return try {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val startOfToday = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            notificationManager.activeNotifications
                .filter { it.notification.group == GROUP_KEY_HABIT_REMINDERS }
                .filter { it.notification.`when` < startOfToday }
                .forEach { notificationManager.cancel(it.tag, it.id) }
            Log.d(TAG, "Successfully cleared stale habit notifications")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear notifications", e)
            Result.retry()
        }
    }
}
