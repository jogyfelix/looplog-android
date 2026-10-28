package com.jolabs.looplog.habit.workers

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitRepeat
import com.jolabs.looplog.model.HabitStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class DailyAlarmVerificationWorkerRobolectricTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private val habitRepository: HabitRepository = mockk()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun buildWorker(): DailyAlarmVerificationWorker {
        val habitAlarmManager = HabitAlarmManager(context, mockk(relaxed = true), mockk(relaxed = true))
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker = DailyAlarmVerificationWorker(
                appContext,
                workerParameters,
                habitRepository,
                habitAlarmManager,
                mockk(relaxed = true)
            )
        }
        return TestListenableWorkerBuilder<DailyAlarmVerificationWorker>(context)
            .setWorkerFactory(factory)
            .build()
    }

    private fun habit(id: Long, name: String) = HabitBasic(
        id = id,
        name = name,
        description = "",
        longestStreak = "0",
        currentStreak = "0",
        habitTime = null,
        habitState = HabitStatus.NONE
    )

    @Test
    fun `reschedules alarms for every habit with a reminder`() {
        every { habitRepository.getAllHabitsDirect() } returns listOf(habit(1L, "Drink water"), habit(2L, "Read"))
        coEvery { habitRepository.getRepeatDaysFromHabit(1L) } returns listOf(
            HabitRepeat(1L, DayOfWeek.MONDAY, 1_000L)
        )
        coEvery { habitRepository.getRepeatDaysFromHabit(2L) } returns listOf(
            HabitRepeat(2L, DayOfWeek.TUESDAY, 2_000L)
        )

        val result = buildWorker().startWork().get()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(shadowOf(alarmManager).scheduledAlarms).isNotEmpty()
    }

    @Test
    fun `one habit failing to load repeat days does not block rescheduling the rest`() {
        every { habitRepository.getAllHabitsDirect() } returns listOf(habit(1L, "Broken"), habit(2L, "Read"))
        coEvery { habitRepository.getRepeatDaysFromHabit(1L) } throws RuntimeException("boom")
        coEvery { habitRepository.getRepeatDaysFromHabit(2L) } returns listOf(
            HabitRepeat(2L, DayOfWeek.TUESDAY, 2_000L)
        )

        val result = buildWorker().startWork().get()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(1)
    }

    @Test
    fun `repository level failure returns Result retry`() {
        every { habitRepository.getAllHabitsDirect() } throws RuntimeException("db down")

        val result = buildWorker().startWork().get()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }
}
