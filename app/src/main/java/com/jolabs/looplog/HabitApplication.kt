package com.jolabs.looplog

import android.app.Application
import androidx.compose.ui.ComposeUiFlags
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.habit.workers.DailyAlarmVerificationWorker
import com.jolabs.looplog.habit.workers.NotificationCleanupWorker
import com.jolabs.looplog.habit.workers.UpdateHabitsWorker
import com.jolabs.looplog.notifications.createPushNotificationChannel
import dagger.hilt.android.HiltAndroidApp
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class HabitApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true
        super.onCreate()
        createPushNotificationChannel(this)
        scheduleDailyWidgetRefresh()
        scheduleDailyAlarmVerification()
        scheduleDailyNotificationCleanup()
        WidgetUpdateUtil.requestHabitWidgetsRefresh(applicationContext)
    }

    private fun scheduleDailyWidgetRefresh() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT).atZone(zone)
        val initialDelayMillis = Duration.between(now, nextMidnight).toMillis()

        val request = PeriodicWorkRequestBuilder<UpdateHabitsWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_widget_refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun scheduleDailyAlarmVerification() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now()

        var next3AM = now.toLocalDate().atTime(LocalTime.of(3, 0)).atZone(zone)

        if (next3AM.isBefore(now) || next3AM.isEqual(now)) {
            next3AM = next3AM.plusDays(1)
        }

        val initialDelayMillis = Duration.between(now, next3AM).toMillis()

        val request = PeriodicWorkRequestBuilder<DailyAlarmVerificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_alarm_verification",
            // UPDATE rewrites the persisted work spec (including the worker class name) on
            // every launch. Registrations created by pre-1.1.9 builds point at an
            // R8-obfuscated class name that no longer exists, and KEEP would leave that
            // stale spec in place until it failed once.
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun scheduleDailyNotificationCleanup() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT).atZone(zone)
        val initialDelayMillis = Duration.between(now, nextMidnight).toMillis()

        val request = PeriodicWorkRequestBuilder<NotificationCleanupWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_notification_cleanup",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
