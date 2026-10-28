package com.jolabs.looplog.habit.alarmManager

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.HabitAnalytics
import com.jolabs.looplog.habit.workers.SingleAlarmRetryWorker
import com.jolabs.looplog.model.HabitStatus
import com.jolabs.looplog.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

const val GROUP_KEY_HABIT_REMINDERS = "com.jolabs.looplog.habit_reminders"

@AndroidEntryPoint
class HabitAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitAlarmManager: HabitAlarmManager

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val habitId = intent?.getLongExtra("habitId", 0L) ?: 0L
        val habitName = intent?.getStringExtra("habitName")
        val timeOfDay = intent?.getLongExtra("timeOfDay", 0L) ?: 0L
        val dayOfWeekStr = intent?.getStringExtra("dayOfWeek")

        if (habitName == null || dayOfWeekStr == null) {
            Log.e("AlarmReceiver", "Missing required extras: habitName or dayOfWeek")
            return
        }

        // Use goAsync() to prevent BroadcastReceiver timeout (extends time limit from 10s to ~1 minute)
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

        scope.launch {
            try {
                // Check if habit is already marked COMPLETED or SKIPPED for today
                val todayEpoch = DateUtils.todayEpochDay()
                val isAlreadyMarked = try {
                    val entries = habitRepository.getHabitEntries(habitId)
                    val todayEntry = entries.find { it.date == todayEpoch }
                    todayEntry?.isCompleted == HabitStatus.COMPLETED || todayEntry?.isCompleted == HabitStatus.SKIPPED
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error checking habit entries for habitId=$habitId", e)
                    false
                }

                if (!isAlreadyMarked) {
                    // Show notification
                    showNotification(context, habitId, habitName)
                } else {
                    Log.d("AlarmReceiver", "Habit $habitName ($habitId) already marked as done/skipped for today. Skipping notification.")
                }

                // Reschedule the alarm for next week (same day, same time)
                try {
                    val dayOfWeek = DayOfWeek.valueOf(dayOfWeekStr)

                    // Use the repeat alarm method to schedule for the next occurrence of this day
                    val rescheduled = habitAlarmManager.scheduleHabitRepeatAlarm(
                        habitId = habitId,
                        habitName = habitName,
                        timeOfDayMillis = timeOfDay,
                        dayOfWeek = dayOfWeek
                    )

                    if (rescheduled) {
                        Log.d("AlarmReceiver", "✅ Rescheduled alarm for $habitName on $dayOfWeek")
                    } else {
                        analyticsTracker.trackError(
                            AnalyticsError.Soft(
                                source = HabitAnalytics.Sources.AlarmReceive,
                                message = "Failed to reschedule alarm for habitId=$habitId — falling back to WorkManager"
                            )
                        )
                        Log.w("AlarmReceiver", "⚠️ Failed to reschedule alarm for $habitName, falling back to WorkManager")
                        enqueueWorkManagerReschedule(context, habitId, habitName, timeOfDay, dayOfWeekStr)
                    }
                } catch (e: Exception) {
                    analyticsTracker.trackError(
                        AnalyticsError.Hard(
                            throwable = e,
                            source = HabitAnalytics.Sources.AlarmReceive,
                            message = "Exception while rescheduling alarm for habitId=$habitId"
                        )
                    )
                    Log.e("AlarmReceiver", "❌ Failed to reschedule alarm for $habitName", e)

                    // Fallback: Use WorkManager with retry policy to reschedule the alarm
                    enqueueWorkManagerReschedule(context, habitId, habitName, timeOfDay, dayOfWeekStr)
                }
            } finally {
                // Signal that the async work is complete
                pendingResult?.finish()
            }
        }
    }

    private fun enqueueWorkManagerReschedule(
        context: Context,
        habitId: Long,
        habitName: String,
        timeOfDay: Long,
        dayOfWeekStr: String
    ) {
        Log.w("AlarmReceiver", "🔄 Enqueueing WorkManager fallback for $habitName on $dayOfWeekStr")

        val inputData = Data.Builder()
            .putLong("habitId", habitId)
            .putString("habitName", habitName)
            .putLong("timeOfDay", timeOfDay)
            .putString("dayOfWeek", dayOfWeekStr)
            .build()

        // Create work request with exponential backoff retry policy
        // Initial delay: 5 minutes, then 15 minutes, then 1 hour
        val rescheduleWork = OneTimeWorkRequestBuilder<SingleAlarmRetryWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                5, // Initial backoff delay
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueue(rescheduleWork)
        Log.d("AlarmReceiver", "✅ WorkManager reschedule job enqueued for $habitName")
    }

    private fun showNotification(context: Context, habitId: Long, habitName: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                ALARM_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.habit_reminder),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        val pendingIntent = buildOpenAppPendingIntent(context)

        val notification = NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_check_24)
            .setContentTitle(context.getString(R.string.habit_reminder))
            .setContentText(context.getString(R.string.habit_reminder_content, habitName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_HABIT_REMINDERS)
            .setTimeoutAfter(
                Duration.between(
                    ZonedDateTime.now(),
                    ZonedDateTime.now().toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                ).toMillis()
            )
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(context)) {
                notify(habitId.toInt(), notification)
            }
        } else {
            Log.w("AlarmReceiver", "⚠️ Cannot show reminder for $habitName ($habitId) — POST_NOTIFICATIONS not granted")
        }
    }
}
