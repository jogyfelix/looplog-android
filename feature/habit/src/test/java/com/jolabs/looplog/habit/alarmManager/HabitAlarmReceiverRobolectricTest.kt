package com.jolabs.looplog.habit.alarmManager

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver.PendingResult
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.habit.workers.SingleAlarmRetryWorker
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import java.time.DayOfWeek

/**
 * Plain onReceive() leaves BroadcastReceiver.getPendingResult() null, so goAsync() returns null and
 * the receiver's `pendingResult.finish()` NPEs. BroadcastReceiver.setPendingResult(...) and
 * ShadowBroadcastPendingResult.create(...) are how Robolectric's own BroadcastReceiver tests attach a
 * real PendingResult, but both are framework-internal/package-private, so ReflectionHelpers is used
 * to invoke them the same way from outside the org.robolectric.shadows package.
 */
private fun dispatch(receiver: HabitAlarmReceiver, context: Context, intent: Intent) {
    val pendingResult = ReflectionHelpers.callStaticMethod<PendingResult>(
        Class.forName("org.robolectric.shadows.ShadowBroadcastPendingResult"),
        "create",
        ReflectionHelpers.ClassParameter.from(Int::class.javaPrimitiveType, 1),
        ReflectionHelpers.ClassParameter.from(String::class.java, "result"),
        ReflectionHelpers.ClassParameter.from(Bundle::class.java, Bundle()),
        ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, true)
    )
    ReflectionHelpers.callInstanceMethod<Unit>(
        receiver,
        "setPendingResult",
        ReflectionHelpers.ClassParameter.from(PendingResult::class.java, pendingResult)
    )
    receiver.onReceive(context, intent)
}

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class HabitAlarmReceiverRobolectricTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        // notify() silently drops notifications posted to a channel that doesn't exist (API 26+)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID, "Habit reminders", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun buildReceiver(
        alarmManager: HabitAlarmManager = HabitAlarmManager(context, mockk(relaxed = true), mockk(relaxed = true))
    ): HabitAlarmReceiver = HabitAlarmReceiver().apply {
        // The Hilt Gradle plugin rewrites onReceive() to call inject() first, which requires a
        // @HiltAndroidApp Application. The Robolectric test runs under a plain Application, so flip
        // the generated `injected` flag to skip Hilt injection and use the fields we set by hand.
        ReflectionHelpers.setField(this, "injected", true)
        ioDispatcher = UnconfinedTestDispatcher()
        habitAlarmManager = alarmManager
        habitRepository = mockk(relaxed = true)
        analyticsTracker = mockk(relaxed = true)
    }

    private fun buildIntent(
        habitId: Long = 1L,
        habitName: String? = "Drink water",
        timeOfDay: Long = 0L,
        dayOfWeek: String? = DayOfWeek.MONDAY.toString()
    ): Intent {
        return Intent().apply {
            putExtra("habitId", habitId)
            if (habitName != null) putExtra("habitName", habitName)
            putExtra("timeOfDay", timeOfDay)
            if (dayOfWeek != null) putExtra("dayOfWeek", dayOfWeek)
        }
    }

    @Test
    fun `onReceive posts a notification and reschedules the alarm for next week`() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val receiver = buildReceiver()

        dispatch(receiver, context, buildIntent(habitId = 42L))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(notificationManager).allNotifications).hasSize(1)
        assertThat(shadowOf(alarmManager).scheduledAlarms).isNotEmpty()
    }

    @Test
    fun `onReceive does nothing when required extras are missing`() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val receiver = buildReceiver()

        dispatch(receiver, context, buildIntent(habitName = null))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(notificationManager).allNotifications).isEmpty()
        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    @Test
    fun `onReceive does not post a notification when POST_NOTIFICATIONS is denied`() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val receiver = buildReceiver()

        dispatch(receiver, context, buildIntent())

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(notificationManager).allNotifications).isEmpty()
        // Rescheduling is independent of the notification permission and should still succeed
        assertThat(shadowOf(alarmManager).scheduledAlarms).isNotEmpty()
    }

    @Test
    fun `onReceive falls back to WorkManager retry when rescheduling fails`() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val failingAlarmManager: HabitAlarmManager = mockk()
        every {
            failingAlarmManager.scheduleHabitRepeatAlarm(any(), any(), any(), any())
        } returns false

        val receiver = buildReceiver(alarmManager = failingAlarmManager)

        dispatch(receiver, context, buildIntent())

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosByTag(SingleAlarmRetryWorker::class.java.name)
            .get()
        assertThat(workInfos).isNotEmpty()
    }
}
