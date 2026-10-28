package com.jolabs.looplog.habit.appfunctions

import android.content.Context
import androidx.appfunctions.AppFunctionContext
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.domain.CreateHabitUseCase
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HabitAppFunctionsTest {

    private lateinit var habitAppFunctions: HabitAppFunctions
    private lateinit var context: Context
    private val createHabitUseCase: CreateHabitUseCase = mockk()
    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val habitAlarmManager: HabitAlarmManager = mockk(relaxed = true)
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private val appFunctionContext: AppFunctionContext = mockk()

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        every { appFunctionContext.context } returns context

        coEvery { habitRepository.upsertHabitEntry(any()) } returns Unit
        coEvery { habitRepository.getHabitByDateOnce(any(), any()) } returns emptyList()

        habitAppFunctions = HabitAppFunctions(
            createHabitUseCase = createHabitUseCase,
            habitRepository = habitRepository,
            habitAlarmManager = habitAlarmManager,
            analyticsTracker = analyticsTracker
        )
    }

    @Test
    fun logHabitForTodayMapsStatusCorrectly() = runTest {
        val habitName = "Exercise"
        val habit = HabitBasic(
            id = 1L,
            name = habitName,
            description = "",
            longestStreak = "0",
            currentStreak = "0",
            habitTime = null,
            habitState = HabitStatus.NONE,
            repeatDays = emptyList()
        )
        every { habitRepository.getAllHabitsDirect() } returns listOf(habit)

        assertTrue(
            habitAppFunctions.logHabitForToday(context, habitName, "COMPLETED")
                .contains("Successfully logged 'Exercise' as Completed")
        )
        assertTrue(
            habitAppFunctions.logHabitForToday(context, habitName, "SKIP")
                .contains("Successfully logged 'Exercise' as Skipped")
        )
        assertTrue(
            habitAppFunctions.logHabitForToday(context, habitName, "SKIPPED")
                .contains("Successfully logged 'Exercise' as Skipped")
        )
        assertTrue(
            habitAppFunctions.logHabitForToday(context, habitName, "NONE")
                .contains("Successfully logged 'Exercise' as None")
        )
    }

    @Test
    fun logHabitForTodayHandlesInvalidState() = runTest {
        val result = habitAppFunctions.logHabitForToday(context, "Exercise", "INVALID")
        assertEquals(
            "Error: Invalid state 'INVALID'. Use 'NONE', 'COMPLETED', or 'SKIPPED'.",
            result
        )
    }

    @Test
    fun logAllHabitsForTodayMapsStatusCorrectly() = runTest {
        coEvery { habitRepository.getHabitByDateOnce(any(), any()) } returns listOf(
            HabitBasic(1, "H1", "", "0", "0", null, HabitStatus.NONE, emptyList()),
            HabitBasic(2, "H2", "", "0", "0", null, HabitStatus.NONE, emptyList())
        )

        val result = habitAppFunctions.logAllHabitsForToday(context, "SKIPPED")
        assertTrue(result.contains("Successfully marked 2 habits as Skipped for today."))
    }
}
