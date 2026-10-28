package com.jolabs.looplog.habit.alarmManager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.HabitRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Exercises the real AlarmManager/PendingIntent/Intent Android framework classes via
 * Robolectric shadows, rather than mocking them, since setAlarmClock/cancel interact
 * with several framework constructors that are awkward to stub reliably with mockk alone.
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class HabitAlarmManagerSchedulingTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private val repository: HabitRepository = mockk(relaxed = true)
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)

    private val fixedNow = ZonedDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneId.systemDefault())

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Robolectric denies SCHEDULE_EXACT_ALARM by default (like Android 14+); grant it so
        // the exact-path tests exercise setAlarmClock rather than the inexact fallback.
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private fun manager(ctx: Context = context) = HabitAlarmManager(
        context = ctx,
        repository = repository,
        analyticsTracker = analyticsTracker
    ).apply { nowProvider = { fixedNow } }

    @Test
    fun `scheduleHabitAlarm schedules an alarm-clock alarm at the expected trigger time`() {
        val targetTime = fixedNow.withHour(14).toInstant().toEpochMilli()

        val result = manager().scheduleHabitAlarm(1L, "Drink water", targetTime, listOf(DayOfWeek.MONDAY))

        assertThat(result).isTrue()
        val scheduled = shadowOf(alarmManager).scheduledAlarms
        assertThat(scheduled).hasSize(1)
        assertThat(scheduled.first().triggerAtMs).isEqualTo(targetTime)
        assertThat(scheduled.first().alarmClockInfo).isNotNull()
    }

    @Test
    fun `scheduleHabitAlarm schedules one alarm per day of week`() {
        val targetTime = fixedNow.withHour(14).toInstant().toEpochMilli()
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        val result = manager().scheduleHabitAlarm(2L, "Read", targetTime, days)

        assertThat(result).isTrue()
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(days.size)
    }

    @Test
    fun `scheduleHabitAlarm cancels any existing alarm before scheduling a new one`() {
        val targetTime = fixedNow.withHour(14).toInstant().toEpochMilli()
        val m = manager()

        m.scheduleHabitAlarm(3L, "Stretch", targetTime, listOf(DayOfWeek.MONDAY))
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(1)

        // Re-scheduling the same habit/day should replace, not duplicate, the alarm
        m.scheduleHabitAlarm(3L, "Stretch", targetTime, listOf(DayOfWeek.MONDAY))
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(1)
    }

    @Test
    fun `scheduleHabitRepeatAlarm schedules only for the given day`() {
        val targetTime = fixedNow.withHour(9).toInstant().toEpochMilli()

        val result = manager().scheduleHabitRepeatAlarm(4L, "Meditate", targetTime, DayOfWeek.TUESDAY)

        assertThat(result).isTrue()
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(1)
    }

    @Test
    fun `cancelAllHabitAlarms cancels every day by default`() {
        val targetTime = fixedNow.withHour(14).toInstant().toEpochMilli()
        val m = manager()
        m.scheduleHabitAlarm(5L, "Journal", targetTime, listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(2)

        m.cancelAllHabitAlarms(5L)

        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    @Test
    fun `cancelAllHabitAlarms only cancels the specified days when a subset is passed`() {
        val targetTime = fixedNow.withHour(14).toInstant().toEpochMilli()
        val m = manager()
        m.scheduleHabitAlarm(6L, "Walk", targetTime, listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))

        m.cancelAllHabitAlarms(6L, listOf(DayOfWeek.MONDAY))

        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(1)
    }

    @Test
    fun `scheduleHabitAlarm falls back to an inexact alarm when exact alarm permission is missing`() {
        val fakeAlarmManager = mockk<AlarmManager>()
        every { fakeAlarmManager.cancel(any<PendingIntent>()) } just Runs
        every { fakeAlarmManager.canScheduleExactAlarms() } returns false
        every { fakeAlarmManager.setAndAllowWhileIdle(any(), any(), any()) } just Runs

        val spiedContext = spyk(context)
        every { spiedContext.getSystemService(Context.ALARM_SERVICE) } returns fakeAlarmManager

        val targetTime = fixedNow.withHour(14).toInstant().toEpochMilli()
        val result = manager(spiedContext).scheduleHabitAlarm(7L, "No permission", targetTime, listOf(DayOfWeek.MONDAY))

        assertThat(result).isTrue()
        verify(exactly = 0) { fakeAlarmManager.setAlarmClock(any(), any()) }
        verify(exactly = 1) { fakeAlarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), any()) }
    }

    @Test
    fun `scheduleHabitRepeatAlarm falls back to an inexact alarm when the permission is revoked mid-flight`() {
        val fakeAlarmManager = mockk<AlarmManager>()
        every { fakeAlarmManager.cancel(any<PendingIntent>()) } just Runs
        // Permission still reads as granted, but the actual call throws — the revocation
        // raced the check. The fallback must still schedule something.
        every { fakeAlarmManager.canScheduleExactAlarms() } returns true
        every { fakeAlarmManager.setAlarmClock(any(), any()) } throws SecurityException("Exact alarm permission revoked")
        every { fakeAlarmManager.setAndAllowWhileIdle(any(), any(), any()) } just Runs

        val spiedContext = spyk(context)
        every { spiedContext.getSystemService(Context.ALARM_SERVICE) } returns fakeAlarmManager

        val targetTime = fixedNow.withHour(14).toInstant().toEpochMilli()
        val result = manager(spiedContext).scheduleHabitRepeatAlarm(8L, "No permission", targetTime, DayOfWeek.MONDAY)

        assertThat(result).isTrue()
        verify(exactly = 1) { fakeAlarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), any()) }
    }
}
