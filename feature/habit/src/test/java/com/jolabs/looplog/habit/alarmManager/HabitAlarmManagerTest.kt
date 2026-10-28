package com.jolabs.looplog.habit.alarmManager

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.HabitRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class HabitAlarmManagerTest {

    private val context: Context = mockk()
    private val alarmManager: AlarmManager = mockk()
    private val repository: HabitRepository = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private lateinit var habitAlarmManager: HabitAlarmManager

    // Fixed "now" for testing: Monday Jan 1st, 2024, 10:00 AM
    private val fixedNow = ZonedDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneId.systemDefault())

    @BeforeEach
    fun setUp() {
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        habitAlarmManager = HabitAlarmManager(
            context = context,
            repository = repository,
            analyticsTracker = analyticsTracker
        ).apply {
            nowProvider = { fixedNow }
            sdkIntProvider = { 33 }
        }
        mockkStatic(ContextCompat::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `checkNotificationPermission should return true when granted`() {
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_GRANTED
        assertThat(habitAlarmManager.checkNotificationPermission()).isTrue()
    }

    @Test
    fun `checkNotificationPermission should return false when denied`() {
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_DENIED
        assertThat(habitAlarmManager.checkNotificationPermission()).isFalse()
    }

    @Test
    fun `getNextAlarmTime when target time is in the future today should return today`() {
        // Target: 2:00 PM (14:00) today (Monday)
        val targetTimeMillis = ZonedDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli()
        val scheduledDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)

        // Act
        val resultMillis = habitAlarmManager.getNextAlarmTime(targetTimeMillis, scheduledDays)
        val resultDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(resultMillis), ZoneId.systemDefault())

        // Assert
        assertThat(resultDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(resultDate.dayOfMonth).isEqualTo(1)
        assertThat(resultDate.hour).isEqualTo(14)
    }

    @Test
    fun `getNextAlarmTime when target time has passed today should return next scheduled day`() {
        // Change fixed now to afternoon: 4:00 PM (16:00)
        val afternoonNow = fixedNow.withHour(16)
        val manager = HabitAlarmManager(context, repository, analyticsTracker).apply { nowProvider = { afternoonNow } }

        // Target: 8:00 AM. Today (Monday) 8:00 AM has passed.
        val targetTimeMillis = afternoonNow.withHour(8).toInstant().toEpochMilli()
        val scheduledDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)

        // Act
        val resultMillis = manager.getNextAlarmTime(targetTimeMillis, scheduledDays)
        val resultDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(resultMillis), ZoneId.systemDefault())

        // Assert: Should skip Monday 8AM and go to Wednesday Jan 3rd 8AM
        assertThat(resultDate.dayOfWeek).isEqualTo(DayOfWeek.WEDNESDAY)
        assertThat(resultDate.dayOfMonth).isEqualTo(3)
        assertThat(resultDate.hour).isEqualTo(8)
    }

    @Test
    fun `getNextAlarmTime when target time passed and next day is not scheduled should skip to next week`() {
        // Start at Saturday Jan 6th, 10:00 AM
        val saturdayNow = ZonedDateTime.of(2024, 1, 6, 10, 0, 0, 0, ZoneId.systemDefault())
        val manager = HabitAlarmManager(context, repository, analyticsTracker).apply { nowProvider = { saturdayNow } }

        // Target: 8:00 AM. Only scheduled for Mondays.
        val targetTimeMillis = saturdayNow.withHour(8).toInstant().toEpochMilli()
        val scheduledDays = listOf(DayOfWeek.MONDAY)

        // Act
        val resultMillis = manager.getNextAlarmTime(targetTimeMillis, scheduledDays)
        val resultDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(resultMillis), ZoneId.systemDefault())

        // Assert: Should jump to next Monday (Jan 8th)
        assertThat(resultDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(resultDate.dayOfMonth).isEqualTo(8)
    }
}
