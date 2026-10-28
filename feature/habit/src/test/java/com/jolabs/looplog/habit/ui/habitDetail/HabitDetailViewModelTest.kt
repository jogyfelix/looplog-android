package com.jolabs.looplog.habit.ui.habitDetail

import UiMessage
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.util.MainDispatcherExtension
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitEntryModel
import com.jolabs.looplog.model.HabitRepeat
import com.jolabs.looplog.model.HabitStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class HabitDetailViewModelTest {

    private val habitRepository: HabitRepository = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private lateinit var viewModel: HabitDetailViewModel

    // Fixed "today" for testing: 2024-01-01 (Monday)
    private val fixedToday = LocalDate.of(2024, 1, 1)

    @BeforeEach
    fun setUp() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns fixedToday
        viewModel = HabitDetailViewModel(habitRepository, analyticsTracker)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LocalDate::class)
    }

    @Test
    fun `initial state should be Loading`() {
        assertThat(viewModel.uiState.value).isEqualTo(HabitDetailUiState.Loading)
    }

    @Test
    fun `loadHabitDetail should emit Success with correct data when habit found`() = runTest {
        val habitId = 1L
        val habitBasic = HabitBasic(
            id = habitId,
            name = "Exercise",
            description = "Daily workout",
            longestStreak = "10",
            currentStreak = "5",
            habitTime = null,
            habitState = HabitStatus.NONE
        )
        // Entry for today (Monday)
        val entries = listOf(
            HabitEntryModel(habitId, fixedToday.toEpochDay(), HabitStatus.COMPLETED)
        )
        val repeatDays = listOf(
            HabitRepeat(habitId, DayOfWeek.MONDAY, null),
            HabitRepeat(habitId, DayOfWeek.WEDNESDAY, null)
        )

        coEvery { habitRepository.getHabitBasicById(habitId) } returns habitBasic
        coEvery { habitRepository.getHabitEntries(habitId) } returns entries
        coEvery { habitRepository.getRepeatDaysFromHabit(habitId) } returns repeatDays

        val states = mutableListOf<HabitDetailUiState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        viewModel.loadHabitDetail(habitId)

        val finalState = states.last()
        assertThat(finalState).isInstanceOf(HabitDetailUiState.Success::class.java)
        val successState = finalState as HabitDetailUiState.Success
        
        // Verify basic info
        assertThat(successState.habitName).isEqualTo("Exercise")
        assertThat(successState.habitDescription).isEqualTo("Daily workout")
        assertThat(successState.currentStreak).isEqualTo(5)
        assertThat(successState.bestStreak).isEqualTo(10)
        assertThat(successState.repeatDays).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        
        // Verify heatmap (last 84 days)
        // today (index 83) should be COMPLETED
        assertThat(successState.heatmapData[83].date).isEqualTo(fixedToday)
        assertThat(successState.heatmapData[83].state).isEqualTo(CompletionState.COMPLETED)
        
        // yesterday (index 82, Sunday) should be NOT_SCHEDULED
        assertThat(successState.heatmapData[82].state).isEqualTo(CompletionState.NOT_SCHEDULED)

        // Verify consistency (last 30 days)
        // Today is Monday. In the last 30 days, there are 5 Mondays (Jan 1, Dec 25, 18, 11, 4).
        // Only Jan 1 (today) is completed. So Monday percentage = 1/5 = 20%
        val mondayStat = successState.consistencyData.find { it.dayOfWeek == DayOfWeek.MONDAY }
        assertThat(mondayStat?.percentage).isEqualTo(20)
        assertThat(mondayStat?.completedCount).isEqualTo(1)

        job.cancel()
    }

    @Test
    fun `loadHabitDetail should emit Error when habit not found`() = runTest {
        val habitId = 1L
        coEvery { habitRepository.getHabitBasicById(habitId) } returns null

        val states = mutableListOf<HabitDetailUiState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        viewModel.loadHabitDetail(habitId)

        assertThat(states.last()).isInstanceOf(HabitDetailUiState.Error::class.java)
        assertThat((states.last() as HabitDetailUiState.Error).message).isEqualTo(UiMessage.StringRes(R.string.habit_not_found))
        job.cancel()
    }

    @Test
    fun `loadHabitDetail should emit Error when exception occurs`() = runTest {
        val habitId = 1L
        coEvery { habitRepository.getHabitBasicById(habitId) } throws RuntimeException("Database error")

        val states = mutableListOf<HabitDetailUiState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        viewModel.loadHabitDetail(habitId)

        assertThat(states.last()).isInstanceOf(HabitDetailUiState.Error::class.java)
        assertThat((states.last() as HabitDetailUiState.Error).message).isEqualTo(UiMessage.Text("Database error"))
        job.cancel()
    }
}
