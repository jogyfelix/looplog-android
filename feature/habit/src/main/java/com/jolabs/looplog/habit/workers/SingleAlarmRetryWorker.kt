package com.jolabs.looplog.habit.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.habit.HabitAnalytics
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.time.DayOfWeek

/**
 * WorkManager worker that handles alarm rescheduling with retry logic.
 *
 * This worker is enqueued when the BroadcastReceiver fails to reschedule an alarm,
 * providing a reliable fallback mechanism with exponential backoff retry.
 *
 * Retry Policy: 5 min, 15 min, 1 hour (exponential backoff)
 */
@HiltWorker
class SingleAlarmRetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val habitAlarmManager: HabitAlarmManager,
    private val analyticsTracker: AnalyticsTracker
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "SingleAlarmRetryWorker"
        const val MAX_RETRY_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        val habitId = inputData.getLong("habitId", 0L)
        val habitName = inputData.getString("habitName")
        val timeOfDay = inputData.getLong("timeOfDay", 0L)
        val dayOfWeekStr = inputData.getString("dayOfWeek")

        if (habitId == 0L || habitName == null || dayOfWeekStr == null) {
            Log.e(TAG, "❌ Invalid input data: habitId=$habitId, habitName=$habitName, dayOfWeek=$dayOfWeekStr")
            return Result.failure()
        }

        return try {
            val dayOfWeek = DayOfWeek.valueOf(dayOfWeekStr)

            // Attempt to reschedule the alarm
            val rescheduled = habitAlarmManager.scheduleHabitRepeatAlarm(
                habitId = habitId,
                habitName = habitName,
                timeOfDayMillis = timeOfDay,
                dayOfWeek = dayOfWeek
            )

            if (!rescheduled) {
                throw IllegalStateException("scheduleHabitRepeatAlarm returned false for habitId=$habitId")
            }

            Log.d(TAG, "✅ Successfully rescheduled alarm for $habitName on $dayOfWeek (Attempt ${runAttemptCount + 1})")
            Result.success()

        } catch (e: CancellationException) {
            // Worker was stopped — not a failure; WorkManager reschedules.
            throw e
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Soft(
                    throwable = e,
                    source = HabitAnalytics.Sources.AlarmRetry,
                    message = "Failed to reschedule alarm (Attempt ${runAttemptCount + 1}): habitId=$habitId"
                )
            )
            Log.e(TAG, "❌ Failed to reschedule alarm (Attempt ${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS): $habitName", e)

            // Retry with exponential backoff if under max attempts
            if (runAttemptCount < MAX_RETRY_ATTEMPTS - 1) {
                Log.w(TAG, "🔄 Will retry rescheduling alarm for $habitName")
                Result.retry()
            } else {
                Log.e(TAG, "⚠️ Max retry attempts reached for $habitName. Giving up.")
                Result.failure()
            }
        }
    }
}
