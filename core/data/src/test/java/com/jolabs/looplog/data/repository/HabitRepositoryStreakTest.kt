package com.jolabs.looplog.data.repository

import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.storage.dao.HabitDao
import com.jolabs.looplog.storage.entity.HabitEntryStatus
import com.jolabs.looplog.storage.entity.HabitEntryTable
import com.jolabs.looplog.storage.entity.RepeatTable
import com.jolabs.looplog.storage.entity.StreakTable
import com.jolabs.looplog.model.HabitEntryModel
import com.jolabs.looplog.model.HabitStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HabitRepositoryStreakTest {

    private val habitDao: HabitDao = mockk(relaxed = true)
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private lateinit var repository: HabitRepositoryImpl

    @BeforeEach
    fun setup() {
        repository = HabitRepositoryImpl(habitDao, analyticsTracker)
    }

    @Test
    fun `streak should be 1 when completed today`() = runTest {
        val habitId = 1L
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()

        coEvery { habitDao.getHabitRepetitionById(habitId) } returns listOf(RepeatTable(habitId, today.dayOfWeek, null))
        coEvery { habitDao.getHabitEntryById(habitId) } returns listOf(HabitEntryTable(habitId, todayEpoch, HabitEntryStatus.COMPLETED, 0L))

        repository.upsertHabitEntry(HabitEntryModel(habitId, todayEpoch, HabitStatus.COMPLETED))

        coVerify { habitDao.upsertHabitStreak(StreakTable(habitId, 1, 1)) }
    }

    @Test
    fun `streak should be 0 when skipped today`() = runTest {
        val habitId = 1L
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()

        coEvery { habitDao.getHabitRepetitionById(habitId) } returns listOf(RepeatTable(habitId, today.dayOfWeek, null))
        coEvery { habitDao.getHabitEntryById(habitId) } returns listOf(HabitEntryTable(habitId, todayEpoch, HabitEntryStatus.SKIPPED, 0L))

        repository.upsertHabitEntry(HabitEntryModel(habitId, todayEpoch, HabitStatus.SKIPPED))

        coVerify { habitDao.upsertHabitStreak(StreakTable(habitId, 0, 0)) }
    }

    @Test
    fun `streak should be 2 when completed today and yesterday`() = runTest {
        val habitId = 1L
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        coEvery { habitDao.getHabitRepetitionById(habitId) } returns listOf(
            RepeatTable(habitId, today.dayOfWeek, null),
            RepeatTable(habitId, yesterday.dayOfWeek, null)
        )
        coEvery { habitDao.getHabitEntryById(habitId) } returns listOf(
            HabitEntryTable(habitId, today.toEpochDay(), HabitEntryStatus.COMPLETED, 0L),
            HabitEntryTable(habitId, yesterday.toEpochDay(), HabitEntryStatus.COMPLETED, 0L)
        )

        repository.upsertHabitEntry(HabitEntryModel(habitId, today.toEpochDay(), HabitStatus.COMPLETED))

        coVerify { habitDao.upsertHabitStreak(StreakTable(habitId, 2, 2)) }
    }

    @Test
    fun `streak should be 1 when completed today but skipped yesterday`() = runTest {
        val habitId = 1L
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        coEvery { habitDao.getHabitRepetitionById(habitId) } returns listOf(
            RepeatTable(habitId, today.dayOfWeek, null),
            RepeatTable(habitId, yesterday.dayOfWeek, null)
        )
        coEvery { habitDao.getHabitEntryById(habitId) } returns listOf(
            HabitEntryTable(habitId, today.toEpochDay(), HabitEntryStatus.COMPLETED, 0L),
            HabitEntryTable(habitId, yesterday.toEpochDay(), HabitEntryStatus.SKIPPED, 0L)
        )

        repository.upsertHabitEntry(HabitEntryModel(habitId, today.toEpochDay(), HabitStatus.COMPLETED))

        coVerify { habitDao.upsertHabitStreak(StreakTable(habitId, 1, 1)) }
    }

    @Test
    fun `streak should be 0 when missed yesterday (NONE)`() = runTest {
        val habitId = 1L
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        coEvery { habitDao.getHabitRepetitionById(habitId) } returns listOf(
            RepeatTable(habitId, today.dayOfWeek, null),
            RepeatTable(habitId, yesterday.dayOfWeek, null)
        )
        // Only today has an entry, yesterday is missing (implicitly NONE)
        coEvery { habitDao.getHabitEntryById(habitId) } returns listOf(
            HabitEntryTable(habitId, today.toEpochDay(), HabitEntryStatus.NONE, 0L)
        )

        repository.upsertHabitEntry(HabitEntryModel(habitId, today.toEpochDay(), HabitStatus.NONE))

        coVerify { habitDao.upsertHabitStreak(StreakTable(habitId, 0, 0)) }
    }

    @Test
    fun `streak should be alive when today is NONE but yesterday was COMPLETED`() = runTest {
        val habitId = 1L
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        coEvery { habitDao.getHabitRepetitionById(habitId) } returns listOf(
            RepeatTable(habitId, today.dayOfWeek, null),
            RepeatTable(habitId, yesterday.dayOfWeek, null)
        )
        coEvery { habitDao.getHabitEntryById(habitId) } returns listOf(
            HabitEntryTable(habitId, today.toEpochDay(), HabitEntryStatus.NONE, 0L),
            HabitEntryTable(habitId, yesterday.toEpochDay(), HabitEntryStatus.COMPLETED, 0L)
        )

        repository.upsertHabitEntry(HabitEntryModel(habitId, today.toEpochDay(), HabitStatus.NONE))

        coVerify { habitDao.upsertHabitStreak(StreakTable(habitId, 1, 1)) }
    }
}
