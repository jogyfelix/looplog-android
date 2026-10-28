package com.jolabs.looplog.habit.ui.habitStats

import Resource
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.util.MainDispatcherExtension
import com.jolabs.looplog.model.DailyCompletionState
import com.jolabs.looplog.model.DailyStat
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.DayOfWeek
import java.time.LocalDate

@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class HabitStatsViewModelTest {

    private val habitRepository: HabitRepository = mockk()
    private lateinit var viewModel: HabitStatsViewModel

    // Fixed "today" for testing: 2024-01-08 (Monday)
    private val fixedToday = LocalDate.of(2024, 1, 8)

    @BeforeEach
    fun setUp() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns fixedToday

        // Default repository mocks to prevent init crashes
        every { habitRepository.getAllHabits() } returns flowOf(Resource.Success(emptyList()))
        every { habitRepository.getHabitByDate(any(), any()) } returns flowOf(Resource.Success(emptyList()))
        every { habitRepository.getWeeklyCompletionRate() } returns flowOf(0)
        every { habitRepository.getPerfectDaysCount() } returns flowOf(0)

        viewModel = HabitStatsViewModel(habitRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LocalDate::class)
    }

    @Test
    fun `initial state of animation played should be false`() {
        assertThat(viewModel.hasWeeklyStatsAnimationPlayed.value).isFalse()
    }

    @Test
    fun `markWeeklyStatsAnimationPlayed should update state to true`() {
        viewModel.markWeeklyStatsAnimationPlayed()
        assertThat(viewModel.hasWeeklyStatsAnimationPlayed.value).isTrue()
    }

    @Test
    fun `habitList should emit data from repository`() = runTest {
        val habits = listOf(mockk<HabitBasic>())
        every { habitRepository.getAllHabits() } returns flowOf(Resource.Success(habits))

        // Create new VM after mocking to trigger init collection or use late init
        val vm = HabitStatsViewModel(habitRepository)
        
        val results = mutableListOf<Resource<List<HabitBasic>>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.habitList.collect { results.add(it) }
        }

        assertThat(results.last()).isInstanceOf(Resource.Success::class.java)
        assertThat((results.last() as Resource.Success).data).isEqualTo(habits)
        job.cancel()
    }

    @Test
    fun `weeklyStats should calculate state correctly for mixed completion`() = runTest {
        // Mock data for "today" (fixedToday - Jan 8th, Monday)
        // Let's say all 7 days have some habits. 
        // We'll mock Jan 8th (today) as PERFECT, Jan 7th as PARTIAL, Jan 6th as NONE.
        
        val perfectHabits = listOf(
            mockk<HabitBasic> { every { habitState } returns HabitStatus.COMPLETED }
        )
        val partialHabits = listOf(
            mockk<HabitBasic> { every { habitState } returns HabitStatus.COMPLETED },
            mockk<HabitBasic> { every { habitState } returns HabitStatus.NONE }
        )
        val noneHabits = listOf(
            mockk<HabitBasic> { every { habitState } returns HabitStatus.NONE }
        )

        every { habitRepository.getHabitByDate(fixedToday.dayOfWeek, fixedToday.toEpochDay()) } returns flowOf(Resource.Success(perfectHabits))
        every { habitRepository.getHabitByDate(fixedToday.minusDays(1).dayOfWeek, fixedToday.minusDays(1).toEpochDay()) } returns flowOf(Resource.Success(partialHabits))
        every { habitRepository.getHabitByDate(fixedToday.minusDays(2).dayOfWeek, fixedToday.minusDays(2).toEpochDay()) } returns flowOf(Resource.Success(noneHabits))
        // Others default to empty from setUp

        val vm = HabitStatsViewModel(habitRepository)
        val stats = mutableListOf<List<DailyStat>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.weeklyStats.collect { stats.add(it) }
        }

        val lastStats = stats.last()
        assertThat(lastStats).hasSize(7)
        
        // weeklyStats is built as (6 downTo 0).map { daysAgo -> ... }
        // index 6 is today (daysAgo = 0)
        // index 5 is yesterday (daysAgo = 1)
        // index 4 is 2 days ago (daysAgo = 2)
        
        assertThat(lastStats[6].state).isEqualTo(DailyCompletionState.PERFECT)
        assertThat(lastStats[5].state).isEqualTo(DailyCompletionState.PARTIAL)
        assertThat(lastStats[4].state).isEqualTo(DailyCompletionState.NONE)
        
        job.cancel()
    }

    @Test
    fun `globalCompletionPercentage should reflect repository value`() = runTest {
        val expectedPercentage = 75
        every { habitRepository.getWeeklyCompletionRate() } returns flowOf(expectedPercentage)
        
        val vm = HabitStatsViewModel(habitRepository)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.globalCompletionPercentage.collect { }
        }

        assertThat(vm.globalCompletionPercentage.value).isEqualTo(expectedPercentage)
        job.cancel()
    }

    @Test
    fun `perfectDaysCount should reflect repository value`() = runTest {
        val expectedCount = 5
        every { habitRepository.getPerfectDaysCount() } returns flowOf(expectedCount)
        
        val vm = HabitStatsViewModel(habitRepository)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.perfectDaysCount.collect { }
        }

        assertThat(vm.perfectDaysCount.value).isEqualTo(expectedCount)
        job.cancel()
    }
}
