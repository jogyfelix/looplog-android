package com.jolabs.looplog.habit.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.habit.alarmManager.GROUP_KEY_HABIT_REMINDERS
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class NotificationCleanupWorkerRobolectricTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create a channel for API 26+
        val channel = NotificationChannel("test", "Test Channel", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }

    @Test
    fun `worker only clears stale habit notifications`() {
        val staleHabitNotification = Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle("Stale habit")
            .setGroup(GROUP_KEY_HABIT_REMINDERS)
            .setWhen(1L)
            .build()
        val currentHabitNotification = Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle("Current habit")
            .setGroup(GROUP_KEY_HABIT_REMINDERS)
            .setWhen(System.currentTimeMillis())
            .build()
        val unrelatedNotification = Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle("Unrelated")
            .setWhen(1L)
            .build()
        notificationManager.notify(1, staleHabitNotification)
        notificationManager.notify(2, currentHabitNotification)
        notificationManager.notify(3, unrelatedNotification)

        // WHEN: the worker runs
        val worker = TestListenableWorkerBuilder<NotificationCleanupWorker>(context).build()
        val result = worker.startWork().get()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(shadowOf(notificationManager).allNotifications).hasSize(2)
    }
}
