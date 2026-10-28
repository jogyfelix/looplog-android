package com.jolabs.looplog.habit.alarmManager

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.DegradedState
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.HabitAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages alarm scheduling and cancellation for habit reminders.
 */
@Singleton
class HabitAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: HabitRepository,
    private val analyticsTracker: AnalyticsTracker
) {
    internal var nowProvider: () -> ZonedDateTime = { ZonedDateTime.now(ZoneId.systemDefault()) }
    internal var sdkIntProvider: () -> Int = { Build.VERSION.SDK_INT }
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "HabitAlarmManager"
    }

    fun checkAlarmPermission() : Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }


    fun checkNotificationPermission() : Boolean {
        return sdkIntProvider() < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getNextAlarmTime(timeOfDayMillis: Long, dayOfWeek: List<DayOfWeek>): Long {
        val now = nowProvider()
        val alarmTime = Instant.ofEpochMilli(timeOfDayMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()

        var nextAlarmDateTime = now.with(alarmTime)

        if (nextAlarmDateTime.isBefore(now)) {
            nextAlarmDateTime = nextAlarmDateTime.plusDays(1)
        }

        while (!dayOfWeek.contains(nextAlarmDateTime.dayOfWeek)) {
            nextAlarmDateTime = nextAlarmDateTime.plusDays(1)
        }

        return nextAlarmDateTime.toInstant().toEpochMilli()
    }

    private fun requestIdFor(habitId: Long, day: DayOfWeek): Int {
        if (habitId.toInt().toLong() != habitId) {
            Log.w(TAG, "habitId $habitId truncated when building alarm requestId — alarm scheduling may collide")
        }
        return (habitId.toInt() * 100) + day.ordinal
    }

    /**
     * Cancels any existing alarm for [uniqueRequestId] and schedules a new one via
     * setAlarmClock — the API real alarm-clock apps use for user-set, must-fire-exact alarms.
     * It's never deferred by Doze/App Standby, but (like setExactAndAllowWhileIdle) it still
     * requires the exact-alarm permission. When that permission is missing/revoked (denied by
     * default on Android 14+), fall back to an inexact alarm so the reminder still fires
     * roughly on time instead of never.
     */
    private fun scheduleExact(uniqueRequestId: Int, pendingIntent: PendingIntent, triggerAtMillis: Long, habitId: Long, habitName: String): Boolean {
        val cancelIntent = PendingIntent.getBroadcast(
            context,
            uniqueRequestId,
            Intent(context, HabitAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(cancelIntent)

        val showPendingIntent = buildOpenAppPendingIntent(context)

        return try {
            if (checkAlarmPermission()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                    pendingIntent
                )
            } else {
                analyticsTracker.trackDegradedState(
                    DegradedState(
                        feature = HabitAnalytics.Feature,
                        reason = "Exact alarm permission not granted for habitId=$habitId — falling back to inexact"
                    )
                )
                Log.w(TAG, "⚠️ Exact alarm permission not granted — scheduling $habitName inexactly")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            true
        } catch (e: SecurityException) {
            // Permission was revoked between the check above and the setAlarmClock call.
            analyticsTracker.trackError(
                AnalyticsError.Soft(
                    throwable = e,
                    source = HabitAnalytics.Sources.Update, // Using Update as scheduling is an update-like action
                    message = "Exact alarm permission revoked while scheduling habitId=$habitId — falling back to inexact"
                )
            )
            Log.e(TAG, "❌ Exact alarm permission revoked while scheduling $habitName — falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            true
        }
    }

    fun scheduleHabitAlarm(habitId: Long,habitName:String, timeOfDayMillis: Long, dayOfWeek: List<DayOfWeek>): Boolean {
        var allScheduled = true

        dayOfWeek.forEach { day ->
            val nextAlarmTime = getNextAlarmTime(timeOfDayMillis, listOf(day))
            val uniqueRequestId = requestIdFor(habitId, day)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueRequestId,
                Intent(context, HabitAlarmReceiver::class.java).apply {
                    putExtra("habitId", habitId)
                    putExtra("habitName", habitName)
                    putExtra("timeOfDay",timeOfDayMillis)
                    putExtra("dayOfWeek",day.toString())
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val scheduled = scheduleExact(uniqueRequestId, pendingIntent, nextAlarmTime, habitId, habitName)
            allScheduled = allScheduled && scheduled

            // Save alarm metadata to repository for verification
            if (scheduled) {
                scope.launch {
                    try {
                        repository.saveAlarmMetadata(
                            habitId = habitId,
                            dayOfWeek = day,
                            timeOfDay = timeOfDayMillis,
                            nextFireTime = nextAlarmTime
                        )
                        Log.d(TAG, "✅ Saved alarm metadata for $habitName on $day")
                    } catch (e: Exception) {
                        analyticsTracker.trackError(
                            AnalyticsError.Soft(
                                throwable = e,
                                source = HabitAnalytics.Sources.Update,
                                message = "Failed to save alarm metadata for habitId=$habitId"
                            )
                        )
                        Log.e(TAG, "❌ Failed to save alarm metadata", e)
                    }
                }
            }
        }

        return allScheduled
    }

    fun scheduleHabitRepeatAlarm(habitId: Long,habitName:String, timeOfDayMillis: Long, dayOfWeek: DayOfWeek): Boolean {
        val uniqueRequestId = requestIdFor(habitId, dayOfWeek)

        // Calculate the next alarm time for this specific day of week
        val nextAlarmTime = getNextAlarmTime(timeOfDayMillis, listOf(dayOfWeek))

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            uniqueRequestId,
            Intent(context, HabitAlarmReceiver::class.java).apply {
                putExtra("habitId", habitId)
                putExtra("habitName", habitName)
                putExtra("timeOfDay",timeOfDayMillis)
                putExtra("dayOfWeek",dayOfWeek.toString())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scheduled = scheduleExact(uniqueRequestId, pendingIntent, nextAlarmTime, habitId, habitName)

        // Save alarm metadata to repository for verification
        if (scheduled) {
            scope.launch {
                try {
                    repository.saveAlarmMetadata(
                        habitId = habitId,
                        dayOfWeek = dayOfWeek,
                        timeOfDay = timeOfDayMillis,
                        nextFireTime = nextAlarmTime
                    )
                    Log.d(TAG, "✅ Saved alarm metadata for $habitName on $dayOfWeek (reschedule)")
                } catch (e: Exception) {
                    analyticsTracker.trackError(
                        AnalyticsError.Soft(
                            throwable = e,
                            source = HabitAnalytics.Sources.Update,
                            message = "Failed to save alarm metadata for habitId=$habitId (reschedule)"
                        )
                    )
                    Log.e(TAG, "❌ Failed to save alarm metadata on reschedule", e)
                }
            }
        }

        return scheduled
    }


    fun cancelAllHabitAlarms(habitId: Long, dayOfWeek: List<DayOfWeek> = DayOfWeek.entries) {
        dayOfWeek.forEach { day ->
            val uniqueRequestId = requestIdFor(habitId, day)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueRequestId, // Use the unique ID here
                Intent(context, HabitAlarmReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)

            // Delete alarm metadata from repository
            scope.launch {
                try {
                    repository.deleteAlarmMetadata(habitId, day)
                    Log.d(TAG, "✅ Deleted alarm metadata for habitId=$habitId on $day")
                } catch (e: Exception) {
                    analyticsTracker.trackError(
                        AnalyticsError.Soft(
                            throwable = e,
                            source = HabitAnalytics.Sources.Delete,
                            message = "Failed to delete alarm metadata for habitId=$habitId"
                        )
                    )
                    Log.e(TAG, "❌ Failed to delete alarm metadata", e)
                }
            }
        }
    }

}
