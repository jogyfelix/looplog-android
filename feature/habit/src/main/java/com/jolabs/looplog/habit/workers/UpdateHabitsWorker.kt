package com.jolabs.looplog.habit.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.util.DateUtils.todayEpochDay
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

@HiltWorker
class UpdateHabitsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "UpdateHabitsWorker"
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        val today = LocalDate.now()

        Log.d(TAG, "==== Widget Midnight Update Started ====")
        Log.d(TAG, "Current date/time: $today ${LocalDate.now().atStartOfDay()}")
        Log.d(TAG, "Fetching habits for: ${today.dayOfWeek} (epochDay: ${todayEpochDay()})")

        return try {
            WidgetUpdateUtil.refreshHabitWidgets(applicationContext, habitRepository)

            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Widget update SUCCESS in ${duration}ms")
            Log.d(TAG, "==== Widget Midnight Update Completed ====")

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Widget update FAILED after ${duration}ms", e)
            Log.e(TAG, "Error: ${e.message}")
            Log.e(TAG, "==== Widget Midnight Update Failed ====")

            Result.retry()
        }
    }
}
