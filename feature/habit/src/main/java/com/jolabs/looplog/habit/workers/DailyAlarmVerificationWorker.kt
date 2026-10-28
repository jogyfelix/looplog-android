package com.jolabs.looplog.habit.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.habit.HabitAnalytics
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltWorker
class DailyAlarmVerificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val habitAlarmManager: HabitAlarmManager,
    private val analyticsTracker: AnalyticsTracker
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "DailyAlarmVerificationWorker"

        /**
         * This worker runs under two unique work names ("daily_alarm_verification" periodic
         * and "alarm_verification_app_open" one-time), so WorkManager can execute two
         * instances concurrently. Serialize the body so overlapping runs don't race on
         * cancelling/rescheduling the same alarms; the second run is idempotent.
         */
        private val runMutex = Mutex()
    }

    override suspend fun doWork(): Result = runMutex.withLock {
        try {
            val habitsResult = habitRepository.getAllHabitsDirect()
            var hasFailures = false

            habitsResult.forEach { habit ->
                try {
                    val habitRepeatList = habitRepository.getRepeatDaysFromHabit(habit.id)
                    val firstRepeat = habitRepeatList.firstOrNull()

                    if (firstRepeat == null) {
                        habitAlarmManager.cancelAllHabitAlarms(habit.id)
                        return@forEach
                    }

                    if (firstRepeat.timeOfDay != null) {
                        val scheduledDays = habitRepeatList.map { it.dayOfWeek }
                        val rescheduled = habitAlarmManager.scheduleHabitAlarm(
                            habit.id,
                            habit.name,
                            firstRepeat.timeOfDay!!,
                            scheduledDays
                        )

                        if (!rescheduled) {
                            Log.e(TAG, "Failed to reschedule alarm for habit: ${habit.name} — exact alarm permission missing/revoked")
                            hasFailures = true
                        } else {
                            habitAlarmManager.cancelAllHabitAlarms(
                                habit.id,
                                java.time.DayOfWeek.entries.filterNot(scheduledDays::contains)
                            )
                        }
                    } else {
                        habitAlarmManager.cancelAllHabitAlarms(habit.id)
                    }
                } catch (e: CancellationException) {
                    // Worker was stopped (e.g. memory pressure) — not a failure; WorkManager reschedules.
                    throw e
                } catch (e: Exception) {
                    hasFailures = true
                    analyticsTracker.trackError(
                        AnalyticsError.Soft(
                            throwable = e,
                            source = HabitAnalytics.Sources.AlarmVerification,
                            message = "Failed to reschedule alarm for habitId=${habit.id}"
                        )
                    )
                    Log.e(TAG, "Failed to reschedule alarm for habit: ${habit.name}", e)
                }
            }
            if (hasFailures) Result.retry() else Result.success()

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Hard(
                    throwable = e,
                    source = HabitAnalytics.Sources.AlarmVerification,
                    message = "Global failure in DailyAlarmVerificationWorker"
                )
            )
            Log.e(TAG, "Failed to reschedule alarms", e)
            Result.retry()
        }
    }
}
