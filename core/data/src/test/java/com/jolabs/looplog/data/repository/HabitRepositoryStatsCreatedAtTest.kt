package com.jolabs.looplog.data.repository

import Resource
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.storage.dao.HabitDao
import com.jolabs.looplog.storage.entity.HabitEntryStatus
import com.jolabs.looplog.storage.entity.HabitEntryTable
import com.jolabs.looplog.storage.entity.HabitTable
import com.jolabs.looplog.storage.entity.RepeatTable
import com.jolabs.looplog.storage.entity.StreakTable
import com.jolabs.looplog.storage.relation.HabitWithDetails
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Regression tests for the "adding a new habit zeroes out past completion stats" bug: a habit's
 * RepeatTable rows match every occurrence of its day of week regardless of when the habit was
 * created, so past-day queries must exclude habits whose createdAt is after the queried day.
 */
class HabitRepositoryStatsCreatedAtTest {

    private val habitDao: HabitDao = mockk(relaxed = true)
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private lateinit var repository: HabitRepositoryImpl

    private val today = LocalDate.now()
    private val oldHabitId = 1L
    private val newHabitId = 2L

    @BeforeEach
    fun setup() {
        repository = HabitRepositoryImpl(habitDao, analyticsTracker)

        // Habit that has existed for the whole 7-day window and was completed every day.
        val oldHabit = HabitTable(
            id = oldHabitId,
            name = "Old habit",
            description = "",
            createdAt = today.minusDays(30).toStartOfDayMillis(),
            updatedAt = 0L
        )

        // Habit created today, repeating on every day of week - simulates the bug scenario of
        // adding a new habit after already having a perfect week.
        val newHabit = HabitTable(
            id = newHabitId,
            name = "New habit",
            description = "",
            createdAt = today.toStartOfDayMillis(),
            updatedAt = 0L
        )

        for (daysAgo in 0..6) {
            val date = today.minusDays(daysAgo.toLong())
            val details = mutableListOf(
                HabitWithDetails(
                    habit = oldHabit,
                    habitEntry = HabitEntryTable(oldHabitId, date.toEpochDay(), HabitEntryStatus.COMPLETED, 0L),
                    habitRepeat = listOf(RepeatTable(oldHabitId, date.dayOfWeek, null)),
                    habitStreak = StreakTable(oldHabitId, 7, 7)
                )
            )
            // Only "today" carries the new habit's row - Room would still return it for past
            // days too since RepeatTable isn't date-scoped, which is exactly what we're
            // guarding against below by not injecting it for daysAgo > 0.
            if (daysAgo == 0) {
                details += HabitWithDetails(
                    habit = newHabit,
                    habitEntry = null,
                    habitRepeat = listOf(RepeatTable(newHabitId, date.dayOfWeek, null)),
                    habitStreak = StreakTable(newHabitId, 0, 0)
                )
            }
            every { habitDao.getHabitByDate(date.dayOfWeek, date.toEpochDay()) } returns flowOf(details)
        }
    }

    @Test
    fun `weekly completion rate ignores habits not yet created on past days`() = runTest {
        // 6 fully-completed past days (1/1) + today counted against both habits (1/2 completed)
        // = 7 completed out of 8 scheduled = 88 percent, not dragged down to 50 percent by
        // counting the new habit as incomplete on the 6 days before it existed.
        val rate = repository.getWeeklyCompletionRate().first()
        assertEquals(88, rate)
    }

    @Test
    fun `perfect days count ignores habits not yet created on past days`() = runTest {
        // The 6 past days remain perfect (old habit only); today isn't perfect since the new
        // habit is still uncompleted.
        val perfectDays = repository.getPerfectDaysCount().first()
        assertEquals(6, perfectDays)
    }

    @Test
    fun `getHabitByDate excludes a habit on days before it was created`() = runTest {
        val yesterday = today.minusDays(1)
        val result = repository.getHabitByDate(yesterday.dayOfWeek, yesterday.toEpochDay()).first { it is Resource.Success }
        val habits = (result as Resource.Success).data.orEmpty()
        assertEquals(listOf(oldHabitId), habits.map { it.id })
    }

    private fun LocalDate.toStartOfDayMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}